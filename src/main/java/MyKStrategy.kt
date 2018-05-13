import model.*
import java.util.*
import kotlin.collections.ArrayList


class MyKStrategy : Strategy {

    public lateinit var painter: MyStrategyPainter

    public lateinit var self: Car
    lateinit var world: World
    lateinit var game: Game
    private lateinit var currentMove: Move

    private var nextWaypointX: Double = 0.0
    private var nextWaypointY: Double = 0.0

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

    private var rndm: Random = Random()

    private fun onInitStrategy() {
        if (rndm == null) {

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
        var bestCntx: SimContext? = null

        for (i in 1..160) {   //TODO visualise

            val cntx = SimContext();
            allSimContexts.add(cntx)
            cntx.self = toExtSelf(self);
            cntx.firstMove = getNextMove(i, null, cntx);
            var move = cntx.firstMove;
            for (j in 1..700) {
                cntx.apply(move!!, this)
                play(cntx)
                move = getNextMove(i, move, cntx)
                if (cntx.collisions) {
                    break
                }
            }
            cntx.score = evaluate(cntx);

            if (cntx.isValid && cntx.score > bestScore) { //TODO do not pick solution if it is too short
                bestScore = cntx.score
                bestCntx = cntx;
                log("best score " + Utils.format(bestScore))
            }
        }
        if (bestCntx != null) {
            bestSimContext = bestCntx;
            allSimContexts.remove(bestSimContext)
        } else {
            bestCntx = SimContext()
            bestCntx.firstMove = randomMove(null).apply {
                if (world.tick % 10 > 5) {
                    enginePower = -1.0
                } else {
                    enginePower = 1.0
                }
            }
            bestSimContext = bestCntx
        }
        currentMove.apply {
            wheelTurn = bestCntx.firstMove!!.wheelTurn
            enginePower = bestCntx.firstMove!!.enginePower
            //TODO other
        }

    }

    private fun getNextMove(i: Int, move: Move?, cntx: SimContext): Move {
        var move1 = move
        move1 = randomMove(move1)

        val n = 0
        val spread = 20
        val to = n + spread

        if (i == 1) {
            move1.wheelTurn = -1.0
        } else if (i == 2) {
            move1.wheelTurn = 1.0
        } else if (i == 3 || i == 4) {
            move1.enginePower = -1.0
            if (i == 3) {
                move1.wheelTurn = 1.0
            } else {
                move1.wheelTurn = -1.0
            }
            cntx.isMovingBackward = true
        } else if (i in n..to) {
            if (i % 3 == 0) {
                move1.enginePower = -1.0
            }
            move1.wheelTurn = -1.0
            move1.wheelTurn += (i - n) * (2 / spread.toFloat())
        }
        return move1
    }

    private fun evaluate(cntx: SimContext): Double {
        cntx.isValid = cntx.self.movedDistance > game.carWidth * 1.2

        var eval = cntx.self.getFinalEvaluation()
        if (cntx.isMovingBackward) {
            eval = (100 - eval) / 100
        }
        return eval;
    }

    private fun play(context: SimContext) {
        val car = context.self
        playCar(car, context)
        context.afterPlay()
    }

    private fun playCar(car: CarExt, context: SimContext) {
        var ox = car.x
        var oy = car.y

        car.x += car.speedX;
        car.y += car.speedY;
        //TODO check collisions
        if (!noCollisions(car)) {
            car.x = ox
            car.y = oy
            car.speedX = 0.0;
            car.speedY = 0.0;
            context.collisions = true
        } else {
            val tileSize = game.trackTileSize
            if (car.x >= car.nextWaypointX * tileSize && car.x <= car.nextWaypointX * tileSize + tileSize &&
                    car.y >= car.nextWaypointY * tileSize && car.y <= car.nextWaypointY * tileSize + tileSize) {
                car.calcNextWaypoint()
            }
        }
    }

    private fun noCollisions(car: CarExt): Boolean {
        val carPoints = getCarPoints(car, game)

        val tileSize = game.trackTileSize
        val margin = game.trackTileMargin

        for (p in carPoints) {
            val x = (p.x / tileSize).toInt()
            val y = (p.y / tileSize).toInt()
            if (x >= game.worldWidth || y >= game.worldHeight || y < 0 || x < 0) {
                return false
            }
            val currentTile: TileType = world.tilesXY[x][y]
            if (hasCollisions(p, currentTile, tileSize, margin)) {
                return false
            }
        }
        return true
    }

    private fun toExtSelf(self: Car): CarExt {
        val carExt = CarExt(self, this)
        return carExt
    }

    private fun randomMove(prevMove: Move?): Move {
        val m = Move()
        m.enginePower = 1.0
        if (prevMove == null) {
            m.wheelTurn = rndm.nextDouble() * 2 - 1
        } else {
            if (rndm.nextBoolean()) {
                m.wheelTurn = prevMove.wheelTurn + game.carWheelTurnChangePerTick
            } else {
                m.wheelTurn = prevMove.wheelTurn - game.carWheelTurnChangePerTick
            }
        }
        if (rndm.nextBoolean()) {
            m.enginePower -= game.carEnginePowerChangePerTick
        } else {
            m.enginePower += game.carEnginePowerChangePerTick
        }

        m.enginePower = Math.max(m.enginePower, -1.0)
        m.enginePower = Math.min(m.enginePower, 1.0)

        m.wheelTurn = Math.max(m.wheelTurn, -1.0)
        m.wheelTurn = Math.min(m.wheelTurn, 1.0)
        return m
    }

    fun getCurrentMove(): Move {
        return currentMove
    }
}

