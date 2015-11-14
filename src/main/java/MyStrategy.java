import model.*;

import static java.lang.Math.*;

public final class MyStrategy implements Strategy {
    private Car self;
    private World world;
    private Game game;
    private Move move;


    private double speedModule;
    private double angleToWaypoint;
    private double nextX;
    private double nextY;

    @Override
    public void move(Car self, World world, Game game, Move move) {

        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        doMove();
    }

    private void doMove() {
        doWheelTurn();

        if (speedModule * speedModule * abs(angleToWaypoint) > 2.5 * 2.5 * PI) {
            move.setBrake(true);
        }
    }

    private void doWheelTurn() {
        nextX = (self.getNextWaypointX() + 0.5) * game.getTrackTileSize();
        nextY = (self.getNextWaypointY() + 0.5) * game.getTrackTileSize();

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

        angleToWaypoint = self.getAngleTo(nextX, nextY);
        speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        move.setWheelTurn(angleToWaypoint * 32d / PI);
        move.setEnginePower(1);
    }

    private TileType getTileType() {
        return world.getTilesXY()[self.getNextWaypointX()][self.getNextWaypointY()];
    }
}
