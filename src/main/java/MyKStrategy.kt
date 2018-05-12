import model.Car
import model.Game
import model.Move
import model.World
import java.lang.Math.cos
import java.lang.Math.sin
import java.util.*
import kotlin.collections.ArrayList


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

    public var allSimContexts: MutableList<SimContext> = ArrayList()

    public var bestSimContext: SimContext = SimContext()

    private fun simMove() {
        allSimContexts = ArrayList()
        var bestScore = -100_000.0
        var bestCntx = SimContext()
        bestCntx.firstMove = randomMove()

        for (i in 1..10) {   //TODO visualise

            val cntx = SimContext();
            allSimContexts.add(cntx)
            cntx.self = toExtSelf(self);
            cntx.firstMove = randomMove();
            var move = cntx.firstMove;
            for (j in 1..100) {
                cntx.self.apply(move, this)
                play(cntx)
                move = randomMove()
            }
            cntx.score = evaluate(cntx);

            if (cntx.score > bestScore) {
                bestScore = cntx.score
                bestCntx = cntx;
                log("best score " + Utils.format(bestScore))
            }
        }

        bestSimContext = bestCntx;

        allSimContexts.remove(bestSimContext)

        currentMove.apply {
            wheelTurn = bestCntx.firstMove.wheelTurn
            enginePower = bestCntx.firstMove.enginePower
            //TODO other
        }

    }

    private fun evaluate(cntx: SimContext): Double {
        return cntx.self.getFinalEvaluation();
    }

    private fun play(context: SimContext) {
        val car = context.self
        playCar(car)
        context.afterPlay()
    }

    private fun playCar(car: CarExt) {
        var ox = car.x
        var oy = car.y

        car.x += car.speedX;
        car.y += car.speedY;
        //TODO check collisions
        if (!noColisions(car)) {
            car.x = ox
            car.y = oy
            car.speedX = 0.0;
            car.speedY = 0.0;
        } else {
            val tileSize = game.trackTileSize
            if (car.x >= car.nextWaypointX * tileSize && car.x <= car.nextWaypointX * tileSize + tileSize &&
                    car.y >= car.nextWaypointY * tileSize && car.y <= car.nextWaypointY * tileSize + tileSize) {
                car.calcNextWaypoint()
            }
        }
    }

    public fun noColisions(car: CarExt): Boolean {
        val carPoints = getCarPoints(car)

        carPoints.size
        return true
    }

    public fun getCarPoints(car: Car): ArrayList<Point2D> {
        val carPoints = ArrayList<Point2D>()
        val w = game.carWidth / 2
        val h = game.carHeight / 2
        carPoints.add(Point2D(car.x + w, car.y + h))
        carPoints.add(Point2D(car.x - w, car.y + h))
        carPoints.add(Point2D(car.x - w, car.y - h))
        carPoints.add(Point2D(car.x + w, car.y - h))

        for (p in carPoints) {
            // translate point to origin
            val tempX = p.x - car.x
            val tempY = p.y - car.y

            // now apply rotation
            val rotatedX = tempX * cos(car.angle) - tempY * sin(car.angle)
            val rotatedY = tempX * sin(car.angle) + tempY * cos(car.angle)

            // translate back
            p.x = rotatedX + car.x
            p.y = rotatedY + car.y

        }
        return carPoints
    }

    private fun toExtSelf(self: Car): CarExt {
        val carExt = CarExt(self, this)
        return carExt
    }

    private fun randomMove(): Move {
        val m = Move()
        m.enginePower = 0.5
        m.wheelTurn = Math.random() * 2 - 1
        return m
    }

    fun getCurrentMove(): Move {
        return currentMove
    }
}

