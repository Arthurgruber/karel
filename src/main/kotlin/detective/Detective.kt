package detective

import common.Diagnostic
import syntax.parser.Sema
import syntax.tree.*

class Detective(val program: Program, val sema: Sema) {
    private val visited = HashSet<Command>()
    private val postcondition = HashMap<Command, Knowledge>()
    private val warnings = ArrayList<Diagnostic>()

    private fun warnAbout(position: Int, message: String) {
        warnings.add(Diagnostic(position, message))
    }

    private fun stopWithBug(position: Int, message: String): Nothing {
        throw Diagnostic(position, message)
    }

    val result: List<Diagnostic> by lazy {
        for (command in program.commands) {
            command.check()
        }
        warnings
    }

    private fun Command.check(): Knowledge {
        return postcondition.getOrPut(this) {
            if (visited.add(this)) {
                body.check(TAUTOLOGY)
            } else {
                // TODO How do we deal with recursive calls?
                TAUTOLOGY
            }
        }
    }

    private fun Statement?.check(above: Knowledge): Knowledge {
        return if (this == null) above else
        when (this) {
            is Block -> {
                var knowledge = above
                for (statement in statements) {
                    if (knowledge == CONTRADICTION) {
                        // warnAbout(0, "dead code")
                        return CONTRADICTION
                    }
                    knowledge = statement.check(knowledge)
                }
                knowledge
            }
            is Call -> {
                when (target.lexeme) {
                    "moveForward" -> {
                        if (above implies FRONT_IS_BLOCKED) stopWithBug(target.start, "cannot move through wall")
                        else above.moveForward()
                    }
                    "turnLeft" -> above.turnLeft()
                    "turnAround" -> above.turnAround()
                    "turnRight" -> above.turnRight()
                    "pickBeeper" -> {
                        if (above implies NO_BEEPER) stopWithBug(target.start, "there is no beeper to pick")
                        else above.pickBeeper()
                    }
                    // TODO How do we deal with preconditions?
                    // Should we simply check each function again for each client?
                    // That would probably require a static "stack trace" to be readable.
                    // TODO Recursive functions yield TAUTOLOGY as of now;
                    // Can we do better for tail-recursive functions?
                    "dropBeeper" -> {
                        if (above implies ON_BEEPER) stopWithBug(target.start, "cannot drop another beeper")
                        else above.dropBeeper()
                    }
                    else -> sema.command(target.lexeme)!!.check()
                }
            }
            is IfThenElse -> {
                val p = condition.learn()
                if (above implies p) {
                    warnAbout(iF.start, "condition is always true")
                    th3n.check(above)
                } else if (above implies p.not()) {
                    warnAbout(iF.start, "condition is always false")
                    e1se.check(above)
                } else {
                    th3n.check(above and p) or e1se.check(above and p.not())
                }
            }
            is While -> {
                val p = condition.learn()
                if (above implies p.not()) {
                    warnAbout(whi1e.start, "loop is never entered")
                    above
                } else if (body.check(p) implies p) {
                    warnAbout(whi1e.start, "infinite loop")
                    p.not()
                } else {
                    p.not()
                }
            }
            is Repeat -> {
                // TODO loop unrolling causes false negatives in conditionals
                body.check(TAUTOLOGY)
            }
        }
    }

    private fun Condition.learn(): Knowledge = when (this) {
        is False -> CONTRADICTION
        is True -> TAUTOLOGY

        is OnBeeper -> ON_BEEPER
        is BeeperAhead -> BEEPER_AHEAD
        is FrontIsClear -> FRONT_IS_CLEAR
        is LeftIsClear -> LEFT_IS_CLEAR
        is RightIsClear -> RIGHT_IS_CLEAR

        is Not -> p.learn().not()
        is Conjunction -> p.learn() and q.learn()
        is Disjunction -> p.learn() or q.learn()
    }
}
