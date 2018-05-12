import model.Move

class SimContext() {
    var isMovingBackward: Boolean = false
    private val debugSim: Boolean = true

    public val moves: MutableList<Point2D> = ArrayList(0)

    fun afterPlay() {
        if (debugSim) {
            moves.add(Point2D(self))
        }
    }

    lateinit var self: CarExt
    lateinit var firstMove: Move
    var score: Double = 0.0
    var collisions: Boolean = false
    var isValid: Boolean = true

}
