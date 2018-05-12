import model.Car
import model.Game
import model.TileType

public fun hasCollisions(p: Point2D, currentTile: TileType, tileSize: Double, margin: Double): Boolean {
    val normX = p.x % tileSize;
    val normY = p.y % tileSize;
    when (currentTile) {
        TileType.EMPTY -> {
        }
        TileType.VERTICAL -> {
            if (isLeftW(normX, margin) || isRightW(normX, margin, tileSize)) {
                return true;
            }
        }
        TileType.HORIZONTAL -> {
            if (isTopW(normY, margin) || isBottomW(normY, margin, tileSize)) {
                return true;
            }
        }
        TileType.LEFT_TOP_CORNER -> {
            if (isLeftW(normX, margin) || isTopW(normY, margin) || isRBCor(normX, normY, margin, tileSize)) {
                return true;
            }
        }
        TileType.RIGHT_TOP_CORNER -> {
            if (isRightW(margin, normX, tileSize) || isTopW(normY, margin) || isLBCor(normX, normY, margin, tileSize)) {
                return true;
            }
        }
        TileType.LEFT_BOTTOM_CORNER -> {
            if (isLeftW(normX, margin) || isBottomW(normY, margin, tileSize) || isRTCor(normX, normY, margin, tileSize)) {
                return true;
            }
        }
        TileType.RIGHT_BOTTOM_CORNER -> {
            if (isRightW(margin, normX, tileSize) || isBottomW(normY, margin, tileSize) || isLTCor(normX, normY, margin, tileSize)) {
                return true;
            }
        }
        TileType.LEFT_HEADED_T -> {
            if (isRightW(margin, normX, tileSize) || isLTCor(normX, normY, margin, tileSize) || isLBCor(normX, normY, margin, tileSize)) {
                return true;
            }
        }
        TileType.RIGHT_HEADED_T -> {
            if (isLeftW(normX, margin) || isRTCor(normX, normY, margin, tileSize) || isRBCor(normX, normY, margin, tileSize)) {
                return true;
            }
        }
        TileType.TOP_HEADED_T -> {
            if (isLTCor(normX, normY, margin, tileSize) || isBottomW(normY, margin, tileSize) || isRTCor(normX, normY, margin, tileSize)) {
                return true;
            }
        }
        TileType.BOTTOM_HEADED_T -> {
            if (isTopW(normY, margin) || isLBCor(normX, normY, margin, tileSize) || isRBCor(normX, normY, margin, tileSize)) {
                return true;
            }
        }
        TileType.CROSSROADS -> {
            if (isLTCor(normX, normY, margin, tileSize) || isLBCor(normX, normY, margin, tileSize) || isRTCor(normX, normY, margin, tileSize) || isRBCor(normX, normY, margin, tileSize)) {
                return true;
            }
        }
        TileType.UNKNOWN -> TODO()
    }
    return false
}

private fun isBottomW(normY: Double, margin: Double, tileSize: Double) = normY > tileSize - margin

private fun isRBCor(normX: Double, normY: Double, margin: Double, tileSize: Double): Boolean {
    return Point2D.getDistance(normX, normY, tileSize, tileSize) < margin
}

private fun isLBCor(normX: Double, normY: Double, margin: Double, tileSize: Double): Boolean {
    return Point2D.getDistance(normX, normY, 0.0, tileSize) < margin
}

private fun isRTCor(normX: Double, normY: Double, margin: Double, tileSize: Double): Boolean {
    return Point2D.getDistance(normX, normY, tileSize, 0.0) < margin
}

private fun isLTCor(normX: Double, normY: Double, margin: Double, tileSize: Double): Boolean {
    return Point2D.getDistance(normX, normY, 0.0, 0.0) < margin
}

private fun isRightW(normX: Double, margin: Double, tileSize: Double) = tileSize - margin < normX

private fun isLeftW(normX: Double, margin: Double) = normX < margin

private fun isTopW(normY: Double, margin: Double) = normY < margin

fun getCarPoints(car: Car, game: Game): ArrayList<Point2D> {
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
        val rotatedX = tempX * Math.cos(car.angle) - tempY * Math.sin(car.angle)
        val rotatedY = tempX * Math.sin(car.angle) + tempY * Math.cos(car.angle)

        // translate back
        p.x = rotatedX + car.x
        p.y = rotatedY + car.y

    }
    return carPoints
}

public fun getAngleDelta(minAngle: Double, maxAngle: Double, angleToPoint: Double): Double {
    var result = Math.min(Math.abs(angleToPoint - minAngle), Math.abs(angleToPoint - maxAngle))
    if (result < Math.PI / 3) {
        return result
    }

    result = Math.min(result, Math.abs(-Math.PI - minAngle) + Math.PI - angleToPoint)
    result = Math.min(result, Math.abs(-Math.PI - angleToPoint) + Math.PI - maxAngle)

    return result
}
