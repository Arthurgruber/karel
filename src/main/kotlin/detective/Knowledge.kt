package detective

val /*  */ FRONT_IS_CLEAR = Knowledge(0xaaaaaaaaaaaaaaaaUL) // 01...
val /**/ FRONT_IS_BLOCKED = FRONT_IS_CLEAR.not()
val /*   */ LEFT_IS_CLEAR = Knowledge(0xccccccccccccccccUL) // 0011...
val /* */ LEFT_IS_BLOCKED = LEFT_IS_CLEAR.not()
val /*   */ BACK_IS_CLEAR = Knowledge(0xf0f0f0f0f0f0f0f0UL) // 00001111...
val /* */ BACK_IS_BLOCKED = BACK_IS_CLEAR.not()
val /*  */ RIGHT_IS_CLEAR = Knowledge(0xff00ff00ff00ff00UL) // 0000000011111111...
val /**/ RIGHT_IS_BLOCKED = RIGHT_IS_CLEAR.not()
val /*    */ BEEPER_AHEAD = Knowledge(0xffff0000ffff0000UL) // 00000000000000001111111111111111...
val /*   */ NOTHING_AHEAD = BEEPER_AHEAD.not()
val /*       */ ON_BEEPER = Knowledge(0xffffffff00000000UL) // 0000000000000000000000000000000011111111111111111111111111111111...
val /*       */ NO_BEEPER = ON_BEEPER.not()

val /*     */ TAUTOLOGY = Knowledge(0xffffffffffffffffUL)
val /* */ CONTRADICTION = Knowledge(0x0000000000000000UL)

@JvmInline
value class Knowledge(private val knowledge: ULong) {
    fun not() = Knowledge(knowledge.inv())

    infix fun and(that: Knowledge) = Knowledge(this.knowledge and that.knowledge)

    infix fun or(that: Knowledge) = Knowledge(this.knowledge or that.knowledge)

    infix fun implies(that: Knowledge) = ((this.not() or that) == TAUTOLOGY)

    fun leftIsClear() = this implies LEFT_IS_CLEAR
    fun frontIsClear() = this implies FRONT_IS_CLEAR
    fun rightIsClear() = this implies RIGHT_IS_CLEAR

    fun onBeeper() = this implies ON_BEEPER
    fun beeperAhead() = this implies BEEPER_AHEAD

    // TODO Can this be improved with bit twiddling?
    fun moveForward() = when {
        beeperAhead() -> BACK_IS_CLEAR and ON_BEEPER
        this implies NOTHING_AHEAD -> BACK_IS_CLEAR and NO_BEEPER
        else -> BACK_IS_CLEAR
    }

    fun turnLeft(): Knowledge {
        var x = forgetAhead()
        x = bit_permute_step(x, 0x2222222222222222UL, 1)
        x = bit_permute_step(x, 0x0c0c0c0c0c0c0c0cUL, 2)
        x = bit_permute_step(x, 0x00f000f000f000f0UL, 4)
        return Knowledge(x)
    }

    fun turnAround(): Knowledge {
        var x = forgetAhead()
        x = bit_permute_step(x, 0x0a0a0a0a0a0a0a0aUL, 3)
        x = bit_permute_step(x, 0x00cc00cc00cc00ccUL, 6)
        return Knowledge(x)
    }

    fun turnRight(): Knowledge {
        var x = forgetAhead()
        x = bit_permute_step(x, 0x00aa00aa00aa00aaUL, 7)
        x = bit_permute_step(x, 0x00cc00cc00cc00ccUL, 6)
        x = bit_permute_step(x, 0x00f000f000f000f0UL, 4)
        return Knowledge(x)
    }

    private fun forgetAhead() = knowledge or bit_permute_step_simple(knowledge, 0x0000ffff0000ffffUL, 16)

    // see http://programming.sirrida.de/calcperm.php
    private fun bit_permute_step_simple(x: ULong, mask: ULong, shift: Int): ULong {
        val t = (x shr shift) and mask
        return ((x and mask) shl shift) or t
    }

    // see http://programming.sirrida.de/calcperm.php
    private fun bit_permute_step(x: ULong, mask: ULong, shift: Int): ULong {
        val t = ((x shr shift) xor x) and mask
        return (x xor t) xor (t shl shift)
    }

    fun pickBeeper() = Knowledge(knowledge shr 32)
    fun dropBeeper() = Knowledge(knowledge shl 32)
}
