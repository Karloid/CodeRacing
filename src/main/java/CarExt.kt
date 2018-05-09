import model.Car
import model.CarType
import model.Move

class CarExt : Car {
    var runningDistance = 0f

    constructor(id: Long, mass: Double, x: Double, y: Double, speedX: Double, speedY: Double, angle: Double, angularSpeed: Double, width: Double, height: Double, playerId: Long, teammateIndex: Int, teammate: Boolean, type: CarType, projectileCount: Int, nitroChargeCount: Int, oilCanisterCount: Int, remainingProjectileCooldownTicks: Int, remainingNitroCooldownTicks: Int, remainingOilCooldownTicks: Int, remainingNitroTicks: Int, remainingOiledTicks: Int, durability: Double, enginePower: Double, wheelTurn: Double, nextWaypointIndex: Int, nextWaypointX: Int, nextWaypointY: Int, finishedTrack: Boolean) : super(id, mass, x, y, speedX, speedY, angle, angularSpeed, width, height, playerId, teammateIndex, teammate, type, projectileCount, nitroChargeCount, oilCanisterCount, remainingProjectileCooldownTicks, remainingNitroCooldownTicks, remainingOilCooldownTicks, remainingNitroTicks, remainingOiledTicks, durability, enginePower, wheelTurn, nextWaypointIndex, nextWaypointX, nextWaypointY, finishedTrack) {}

    constructor(other: Car) : super(other) {}

    private lateinit var mys: MyKStrategy

    constructor(other: Car, mys: MyKStrategy) : super(other) {
        this.mys = mys
    }

    fun apply(move: Move) {
        enginePower = move.enginePower

    }

    fun calcNextWaypoint() {
        this.nextWaypointIndex++
        if (mys.world.waypoints.size == nextWaypointIndex) {
            runningDistance = runningDistance + 100;
            nextWaypointIndex = 0
        }
        nextWaypointX = mys.world.waypoints[nextWaypointIndex][0]
        nextWaypointY = mys.world.waypoints[nextWaypointIndex][1]
    }
}
