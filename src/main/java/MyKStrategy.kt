import model.Car
import model.Game
import model.Move
import model.World
import java.util.*


class MyKStrategy : Strategy {

    public lateinit var painter: MyStrategyPainter

    public lateinit var self: Car
    lateinit var world: World
    lateinit var game: Game
    private lateinit var currentMove: Move

    public var nextWaypointX: Double = 0.0
    public var nextWaypointY: Double = 0.0

    override fun move(self: Car, world: World, game: Game, move: Move) {
        onTickStart(self, world, game, move)
        onInitStrategy()


        painter.onStartTick()

        if (world.tick > 200) {
            simMove();

            log(Utils.format(currentMove.wheelTurn) + " " + Utils.format(currentMove.enginePower))
        }
        painter.onEndTick()
    }

    private var rndm: Random? = null

    private fun onInitStrategy() {
        if (rndm == null) {
            rndm = Random()
            painter.onInitializeStrategy()
        }
    }

    private fun log(msg: String) {
        println(world.tick.toString() + ": " + msg)
    }

    private fun onTickStart(self: Car, world: World, game: Game, move: Move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.currentMove = move;

        nextWaypointX = (self.nextWaypointX + 0.5) * game.trackTileSize
        nextWaypointY = (self.nextWaypointY + 0.5) * game.trackTileSize
    }

    private fun simMove() {
        var bestScore = -100_000.0
        var bestMove = Move()
        for (i in 1..100) {   //TODO visualise
            val selfCopy = toExtSelf(self);
            val firstMove = randomMove();
            var move = firstMove;
            for (j in 1..100) {
                selfCopy.apply(move)
                play(selfCopy)
                move = randomMove()
            }
            val score = evaluate(selfCopy);

            if (score > bestScore) {
                bestScore = score
                bestMove = firstMove;
                log("best score " + Utils.format(bestScore))
            }
        }

        currentMove.apply {
            wheelTurn = bestMove.wheelTurn
            enginePower = bestMove.enginePower
            //TODO other
        }

    }

    private fun evaluate(selfCopy: CarExt): Double {
        return selfCopy.getFinalEvaluation();
    }

    private fun play(car: CarExt) {
        car.x += car.speedX;
        car.y += car.speedY;
        //TODO check collisions


        val tileSize = game.trackTileSize
        if (car.x >= car.nextWaypointX * tileSize && car.x <= car.nextWaypointX * tileSize + tileSize &&
                car.y >= car.nextWaypointY * tileSize && car.y <= car.nextWaypointY * tileSize + tileSize) {
            car.calcNextWaypoint()
        }
    }

    private fun toExtSelf(self: Car): CarExt {
        val carExt = CarExt(self, this)
        return carExt
    }

    private fun randomMove(): Move {
        val m = Move()
        m.enginePower = 0.1
        m.wheelTurn = Math.random() * 2 - 1
        return m
    }

    fun getCurrentMove(): Move {
        return currentMove
    }
}

