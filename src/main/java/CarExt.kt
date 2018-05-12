import model.Car
import model.Move

class CarExt : Car {
    var scoreValue = 0.0

    private var mys: MyKStrategy

    public var movedDistance: Double = 0.0

    constructor(other: Car, mys: MyKStrategy) : super(other) {
        this.mys = mys
    }


    fun apply(move: Move, mys: MyKStrategy) {
        if (move.enginePower > enginePower) {
            enginePower += mys.game.carEnginePowerChangePerTick
        } else {
            enginePower -= mys.game.carEnginePowerChangePerTick
        }


        var speedVector = speedVector()

        movedDistance += speedVector.length()

        val isMovingForward = move.enginePower > 0

        val speedIncreaseByTick = Point2D(0.25 * Math.abs(enginePower), 0.0).rotate(angle)
        if (isMovingForward) {
            speedVector = speedVector.add(speedIncreaseByTick)
        } else {
            speedVector = speedVector.sub(speedIncreaseByTick)
        }
        //speedVector = speedVector.mul(2.0)

        var turn = mys.game.carWheelTurnChangePerTick
        if (move.wheelTurn < 0) {
            turn = -turn;
        }

        speedVector = speedVector.rotate(turn)
        speedX = speedVector.x
        speedY = speedVector.y
        var i = 10
    }

    private fun speedVector(): Point2D {
        return Point2D(speedX, speedY)
    }

    fun calcNextWaypoint() {
        this.nextWaypointIndex++
        scoreValue = scoreValue + 100;
        if (mys.world.waypoints.size == nextWaypointIndex) {
            nextWaypointIndex = 0
        }
        nextWaypointX = mys.world.waypoints[nextWaypointIndex][0]
        nextWaypointY = mys.world.waypoints[nextWaypointIndex][1]
    }

    fun getFinalEvaluation(): Double {
        var prev = getPrevWaypoint().add(0.5, 0.5).mul(mys.game.trackTileSize)
        var next = Point2D(nextWaypointX.toDouble() + 0.5, nextWaypointY.toDouble() + 0.5).mul(mys.game.trackTileSize)

        var bonus = (1 - next.getDistance(this) / prev.getDistance(next)) * 100
        return scoreValue + bonus
    }

    private fun getPrevWaypoint(): Point2D {
        var i = nextWaypointIndex - 1
        if (i < 0) {
            i = mys.world.waypoints.size - 1
        }
        return Point2D(mys.world.waypoints[i][0].toDouble(), mys.world.waypoints[i][1].toDouble())
    }
}
