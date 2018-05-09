import model.Car
import model.Game
import model.Move
import model.World


class MyKStrategy : Strategy {
    private lateinit var self: Car
    lateinit var world: World
    private lateinit var game: Game
    private lateinit var currentMove: Move

    public var nextWaypointX: Double = 0.0
    public var nextWaypointY: Double = 0.0

    override fun move(self: Car, world: World, game: Game, move: Move) {
        onTickStart(self, world, game, move)

        simMove();

        move.enginePower = 1.0
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
        var bestScore = -100_000
        for (i in 1..50) {
            val cars = world.cars.toCollection(ArrayList())
            var selfCopy = toExtSelf(self);
            val firstMove: Move = randomMove();
            val move: Move = firstMove;
            for (j in 1..30) {
                selfCopy.apply(move)
                play(selfCopy)
            }
            var score: Float = evaluate(selfCopy);
        }

    }

    private fun evaluate(selfCopy: CarExt): Float {
        return selfCopy.runningDistance;
    }

    private fun play(car: CarExt) {
        car.x += car.speedX;
        car.y += car.speedY;

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
        m.enginePower = 1.0
        m.wheelTurn = Math.random() * 2 - 1
        return m
    }
}

