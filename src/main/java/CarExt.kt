import model.Car
import model.CarType
import model.Move

class CarExt : Car {
    var runningDistance = 0.0

    constructor(id: Long, mass: Double, x: Double, y: Double, speedX: Double, speedY: Double, angle: Double, angularSpeed: Double, width: Double, height: Double, playerId: Long, teammateIndex: Int, teammate: Boolean, type: CarType, projectileCount: Int, nitroChargeCount: Int, oilCanisterCount: Int, remainingProjectileCooldownTicks: Int, remainingNitroCooldownTicks: Int, remainingOilCooldownTicks: Int, remainingNitroTicks: Int, remainingOiledTicks: Int, durability: Double, enginePower: Double, wheelTurn: Double, nextWaypointIndex: Int, nextWaypointX: Int, nextWaypointY: Int, finishedTrack: Boolean) : super(id, mass, x, y, speedX, speedY, angle, angularSpeed, width, height, playerId, teammateIndex, teammate, type, projectileCount, nitroChargeCount, oilCanisterCount, remainingProjectileCooldownTicks, remainingNitroCooldownTicks, remainingOilCooldownTicks, remainingNitroTicks, remainingOiledTicks, durability, enginePower, wheelTurn, nextWaypointIndex, nextWaypointX, nextWaypointY, finishedTrack) {}

    constructor(other: Car) : super(other) {}

    private lateinit var mys: MyKStrategy

    constructor(other: Car, mys: MyKStrategy) : super(other) {
        this.mys = mys
    }

    fun apply(move: Move, mys: MyKStrategy) {
        enginePower = move.enginePower

        val origSpeedVector = speedVector()
        var speedVector = speedVector()

        if (speedVector.length() < 0.5) {
            speedVector = speedVector.length(1.0)
        }
        //speedVector = speedVector.mul(2.0)

        var turn = mys.game.carWheelTurnChangePerTick
        if (move.wheelTurn > 0) {
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
        runningDistance = runningDistance + 100;
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
        return runningDistance + bonus
    }

    private fun getPrevWaypoint(): Point2D {
        var i = nextWaypointIndex - 1
        if (i < 0) {
            i = mys.world.waypoints.size - 1
        }
        return Point2D(mys.world.waypoints[i][0].toDouble(), mys.world.waypoints[i][1].toDouble())
    }
}
