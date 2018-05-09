import model.Car
import model.Game
import model.Move
import model.TileType.*
import model.World


class MyKStrategy : Strategy {
    override fun move(self: Car, world: World, game: Game, move: Move) {
        var nextWaypointX = (self.nextWaypointX + 0.5) * game.trackTileSize
        var nextWaypointY = (self.nextWaypointY + 0.5) * game.trackTileSize

        val cornerTileOffset = 0.25 * game.trackTileSize

        when (world.tilesXY[self.nextWaypointX][self.nextWaypointY]) {
            LEFT_TOP_CORNER -> {
                nextWaypointX += cornerTileOffset
                nextWaypointY += cornerTileOffset
            }
            RIGHT_TOP_CORNER -> {
                nextWaypointX -= cornerTileOffset
                nextWaypointY += cornerTileOffset
            }
            LEFT_BOTTOM_CORNER -> {
                nextWaypointX += cornerTileOffset
                nextWaypointY -= cornerTileOffset
            }
            RIGHT_BOTTOM_CORNER -> {
                nextWaypointX -= cornerTileOffset
                nextWaypointY -= cornerTileOffset
            }
        }

        val angleToWaypoint = self.getAngleTo(nextWaypointX, nextWaypointY)
        val speedModule = FastMath.hypot(self.speedX, self.speedY)

        move.wheelTurn = angleToWaypoint * 32.0 / Math.PI
        move.enginePower = 0.75

        if (speedModule * speedModule * Math.abs(angleToWaypoint) > 2.5 * 2.5 * Math.PI) {
            move.isBrake = true
        }
    }
}
