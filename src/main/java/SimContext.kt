import model.Move

class SimContext() {
    var isMovingBackward: Boolean = false
    private val debugSim: Boolean = true

    public val pos: MutableList<Point2D> = ArrayList(0)
    public val moves: MutableList<Move> = ArrayList(0)

    fun afterPlay() {
        if (debugSim) {
            pos.add(Point2D(self))
        }
    }

    fun apply(move: Move, myKStrategy: MyKStrategy) {
        self.apply(move, myKStrategy)
        moves.add(move)
    }

    lateinit var self: CarExt
    var firstMove: Move? = null
        set(value) {
            moves.add(value!!)
            field = value
        }
    var score: Double = 0.0
    var collisions: Boolean = false
    var isValid: Boolean = true

}
