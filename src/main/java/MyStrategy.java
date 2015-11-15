import model.*;

import javax.swing.*;

import java.awt.*;

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

    private JFrame frame;
    private MyPanel panel;
    private Graphics2D dG;
    private int sidePadding = 46;

    @Override
    public void move(Car self, World world, Game game, Move move) {

        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        doMove();

        log("distance " + f(distanceToWaypoint) + "; angle: " + f(angleToWaypoint) + "; speed " + f(speedModule));
        drawWindow();
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

    @SuppressWarnings("MagicConstant")
    private void drawWindow() {
        if (!DEBUG)
            return;

        if (frame == null) {
            frame = new JFrame();
            frame.setSize(dSize(world.getWidth() * game.getTrackTileSize()) + sidePadding * 2, dSize(world.getHeight() * game.getTrackTileSize()) + sidePadding * 2);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            panel = new MyPanel();
            frame.add(panel);
        }

        frame.repaint();
    }

    private int dSize(double v, int padding) {
        return (int) (v / 12) + padding;
    }

    private int dSize(double x) {
        return dSize(x, MyStrategy.this.sidePadding);
    }

    private class MyPanel extends JPanel {

        private Graphics2D g2;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g2 = (Graphics2D) g;
            g2.setColor(Color.WHITE);
            g2.fillRect(sidePadding, sidePadding, panel.getWidth() - sidePadding * 2, panel.getHeight() - sidePadding * 2);
            drawCars();

            drawMyLines();
        }

        private void drawMyLines() {
            g2.setColor(Color.green);
            g2.drawLine(dSize(self.getX()), dSize(self.getY()), dSize(nextX), dSize(nextY));
        }

        private void drawCars() {
            int width = dSize(game.getCarWidth(), 0);
            int height = dSize(game.getCarHeight(), 0);

            for (Car car : world.getCars()) {
                Graphics2D gg = (Graphics2D) g2.create();

                gg.setColor(car.getPlayerId() == world.getMyPlayer().getId() ? Color.green : Color.red);
                int x = (int) (dSize(car.getX()) + width / 2d);
                int y = (int) (dSize(car.getY()) + height / 2d);

                Rectangle r = new Rectangle(
                        (int) (0 - width / 2d), (int) (0 - height / 2d),
                        width, height);

                gg.translate(x, y);
                gg.rotate(car.getAngle());
                gg.draw(r);
                gg.fill(r);

                gg.dispose();

                /*
                g2.rotate(0);
                g2.draw(rect2);
                g2.fill(rect2);*/
            }
        }


    }
}
