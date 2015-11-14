import model.*;

import static java.lang.Math.*;

public final class MyStrategy implements Strategy {
    private Car self;
    private World world;
    private Game game;
    private Move move;

    @Override
    public void move(Car self, World world, Game game, Move move) {

        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        doMove();
    }

    private void doMove() {
        double nextX = (self.getNextWaypointX() + 0.5) * game.getTrackTileSize();
        double nextY = (self.getNextWaypointY() + 0.5) * game.getTrackTileSize();

        double cornerTileOffset = 0.25D * game.getTrackTileSize();

        switch (getTileType()) {
            case LEFT_TOP_CORNER:
                nextX += cornerTileOffset;
                nextY += cornerTileOffset;
                break;
            case RIGHT_TOP_CORNER:
                nextX -= cornerTileOffset;
                nextY += cornerTileOffset;
                break;
            case LEFT_BOTTOM_CORNER:
                nextX += cornerTileOffset;
                nextY -= cornerTileOffset;
                break;
            case RIGHT_BOTTOM_CORNER:
                nextX -= cornerTileOffset;
                nextY -= cornerTileOffset;
                break;
            default:
        }

        double angleTo = self.getAngleTo(nextX, nextY);
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        move.setWheelTurn(angleTo * 32d / PI);
        move.setEnginePower(0.75);
        if (speedModule * speedModule * abs(angleTo) > 2.5 * 2.5 * PI) {
            move.setBrake(true);
        }
    }

    private TileType getTileType() {
        return world.getTilesXY()[self.getNextWaypointX()][self.getNextWaypointY()];
    }
}
