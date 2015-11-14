import model.*;

import static java.lang.Math.*;

public final class MyStrategy implements Strategy {
    private static final boolean DEBUG = true;

    private Car self;
    private World world;
    private Game game;
    private Move move;


    private double speedModule;
    private double angleToWaypoint;
    private double nextX;
    private double nextY;
    private double distanceToWaypoint;

    @Override
    public void move(Car self, World world, Game game, Move move) {

        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        doMove();

        log("distance " + f(distanceToWaypoint) + "; angle: " + f(angleToWaypoint) + "; speed " + f(speedModule));
    }

    private String f(double distanceToWaypoint) {
        return String.format("%.2f", distanceToWaypoint);
    }

    private void log(String string) {
        if (DEBUG)
            System.out.println(world.getTick() + " - " + string);
    }

    private void doMove() {
        doWheelTurn();

        if (speedModule * speedModule * abs(angleToWaypoint) > 2.5 * 2.5 * PI) {
            move.setBrake(true);
        } else if (isTimeForNitro()) {
            move.setUseNitro(true);
            log("!!! use nitro!");
        }
    }

    private boolean isTimeForNitro() {
        return abs(angleToWaypoint) < 0.1f && distanceToWaypoint > 1000 && game.getInitialFreezeDurationTicks() < world.getTick() && self.getNitroChargeCount() > 0;
    }

    private void doWheelTurn() {
        nextX = (self.getNextWaypointX() + 0.5) * game.getTrackTileSize();
        nextY = (self.getNextWaypointY() + 0.5) * game.getTrackTileSize();

        distanceToWaypoint = self.getDistanceTo(nextX, nextY);

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
