import model.*;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.*;

public final class MyStrategy implements Strategy {
    private static final boolean DEBUG = true;
    public static final int X = 0;
    public static final int Y = 1;
    public static final double debugKoef = 16d;
    public static final String MAP_03 = "map03";
    private static final String MAP_04 = "map04";
    public static final int TICKS_COUNT_FOR_DISTANCE = 80;
    public static final int GAP = 40;

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
    private double pointTileOffset;
    private int curWaypointInd;
    private List<FPoint[]> tilesPoints;
    private int[][] customWaypoints;
    private LinkedHashMap<Integer, Double> distanceQueue;
    private int moveBackwardPoint = -TICKS_COUNT_FOR_DISTANCE;
    private boolean reverseMove;

    @Override
    public void move(Car self, World world, Game game, Move move) {

        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        findCurrentWaypoint();

        doMove();

        doStatistics();
        //log("distance " + f(distanceToWaypoint) + "; angle: " + f(angleToWaypoint) + "; speed " + f(speedModule));
        drawWindow();
    }

    private void doStatistics() {
        if (distanceQueue == null) {
            distanceQueue = new LinkedHashMap<Integer, Double>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, Double> eldest) {
                    return this.size() > TICKS_COUNT_FOR_DISTANCE;
                }
            };
        }

        distanceQueue.put(world.getTick(), speedModule);

        Double sum = distanceQueue.values().stream().collect(Collectors.summingDouble(value -> value));
        //   log("distance: " + sum);

        if (world.getTick() > TICKS_COUNT_FOR_DISTANCE + game.getInitialFreezeDurationTicks()) {
            if (sum < 80 && getMoveBackWardDelta() > TICKS_COUNT_FOR_DISTANCE * 3) {
                log("Need to move back!!!");
                moveBackwardPoint = world.getTick();
            }
        }
    }

    private int getMoveBackWardDelta() {
        return world.getTick() - moveBackwardPoint;
    }

    private void findCurrentWaypoint() {
        if (isMap03() || isMap04() || isMap05()) {
            int[] currentTile = getWaypoints()[curWaypointInd];
            if ((int) (self.getX() / game.getTrackTileSize()) == currentTile[0] && (int) (self.getY() / game.getTrackTileSize()) == currentTile[1]) {
                curWaypointInd++;
                if (curWaypointInd >= getWaypoints().length) {
                    curWaypointInd = 0;
                }
            }
        } else {
            boolean find = false;
            int realNextX = getNextWaypointX();
            int realNextY = getNextWaypointY();
            for (; curWaypointInd < getWaypoints().length; curWaypointInd++) {
                int[] xy = getWaypoints()[curWaypointInd];
                if (xy[0] == realNextX && xy[1] == realNextY) {
                    find = true;
                    break;
                }
            }
            if (!find) {
                curWaypointInd = 0;
                int[] xy = getWaypoints()[curWaypointInd];
                if (xy[0] != realNextX || xy[1] != realNextY) {
                    log("SUPER ERROR: Incorrect waypoint index");
                }
            }
        }
    }

    private int getNextWaypointX() {
        return self.getNextWaypointX();
    }

    private int[][] getWaypoints() {
        if (isMap03()) {
            return getMap03Waypoints();
        }
        if (isMap04()) {
            return getMap04Waypoints();
        }

        if (isMap05()) {
            return getMap05Waypoints();
        }
        return world.getWaypoints();
    }

    private boolean isMap05() {
        return world.getMapName().equals("map05");
    }

    private boolean isMap03() {
        return world.getMapName().equals(MAP_03);
    }

    private boolean isMap04() {
        return world.getMapName().equals(MAP_04);
    }

    private String f(double v) {
        return String.format("%.2f", v);
    }

    private void log(String string) {
        if (DEBUG)
            System.out.println(world.getTick() + " - " + string);
    }

    private void doMove() {
        doWheelTurn();

        if (isBrakeNeed()) {
            move.setBrake(true);
        } else if (isTimeForNitro()) {
            move.setUseNitro(true);
            log("!!! use nitro!");
        }

        if (distanceToWaypoint < game.getTrackTileSize() && isCorner(curWaypointInd) && self.getOilCanisterCount() > 0 && notLast()) {
            move.setSpillOil(true);
            log("use oil!!1");
        }
        for (Car car : world.getCars()) {
            if (!car.isTeammate() && car.getDurability() > 0d && self.getDistanceTo(car) < game.getTrackTileSize() * 1.5f && abs(self.getAngleTo(car)) < 0.1f && self.getProjectileCount() > 0) {
                move.setThrowProjectile(true);
                log("!!!use projectiles");
            }
        }
    }

    private boolean notLast() {
        for (Player player : world.getPlayers()) {
            if (player.getScore() < world.getMyPlayer().getScore()) {
                return true;
            }
        }
        return false;
    }

    private boolean isBrakeNeed() {
        boolean angleStuff = false/*speedModule * speedModule * abs(angleToWaypoint) > 2.5 * 2.5 * PI*/;
        float carefulCof = self.getDurability() < 0.25d ? 0.5f : 1f;
        float maxSpeed = 32 * carefulCof;
        boolean tooFast = speedModule > maxSpeed && curWaypointInd != 0;
        float maxSpeedOnCorner = 16 * carefulCof;
        boolean tooFastCorner = speedModule > maxSpeedOnCorner && isCorner(curWaypointInd) && self.getDistanceTo(new FPoint(nextX, nextY)) < game.getTrackTileSize() * 0.8f;
        if (tooFastCorner) log("TOO fast for corner!");
        return angleStuff || tooFast || tooFastCorner;
    }

    private boolean isCorner(int curWaypointInd) {
        int[] xy = getWaypoints()[curWaypointInd];
        TileType tileType = world.getTilesXY()[xy[0]][xy[1]];
        return tileType == TileType.LEFT_BOTTOM_CORNER || tileType == TileType.RIGHT_BOTTOM_CORNER || tileType == TileType.LEFT_TOP_CORNER || tileType == TileType.RIGHT_TOP_CORNER;
    }

    private boolean isTimeForNitro() {
        return abs(angleToWaypoint) < 0.1f && (distanceToWaypoint > game.getTrackTileSize() * 3 || curWaypointInd == 0) && game.getInitialFreezeDurationTicks() < world.getTick() && self.getNitroChargeCount() > 0;
    }

    private void doWheelTurn() {

        findNextXY();

        distanceToWaypoint = self.getDistanceTo(nextX, nextY);

        angleToWaypoint = self.getAngleTo(nextX, nextY);
        speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        if (abs(angleToWaypoint) > 2f && getMoveBackWardDelta() >= 0 && getMoveBackWardDelta() < TICKS_COUNT_FOR_DISTANCE + GAP) {
            if (angleToWaypoint > 0) {
                angleToWaypoint = angleToWaypoint - PI;
            } else {
                angleToWaypoint = PI + angleToWaypoint;
            }
            reverseMove = true;
        } else {
            reverseMove = false;
        }


        log("angleToWaypoint: " + angleToWaypoint);
        int reverseMoveFactor = reverseMove ? -1 : 1;
        int backwardSpeed = -1 * reverseMoveFactor;
        double normalSpeed = (speedModule < 15 ? 1d : 0.75d) * reverseMoveFactor * getMap03HackSpeedFactor();

        if (getMoveBackWardDelta() >= 0 && getMoveBackWardDelta() < TICKS_COUNT_FOR_DISTANCE) {
            setWheelTurn(angleToWaypoint * -1 * reverseMoveFactor);
            move.setEnginePower(backwardSpeed);
        } else {
            if (getMoveBackWardDelta() <=
                    TICKS_COUNT_FOR_DISTANCE + GAP && getMoveBackWardDelta() >= TICKS_COUNT_FOR_DISTANCE) {
                setWheelTurn(angleToWaypoint * -1 * reverseMoveFactor);
            } else {
                setWheelTurn(angleToWaypoint * reverseMoveFactor);
            }
            move.setEnginePower(normalSpeed);
        }
    }

    private double getMap03HackSpeedFactor() {
        if (!isMap03()) return 1d;
        if (world.getTick() < 380)
            return 0.3;
        return 1;
    }

    private void setWheelTurn(double v) {
        move.setWheelTurn(v * 32.0D / PI);
    }

    @SuppressWarnings("ConstantConditions")
    private void findNextXY() {
        pointTileOffset = game.getTrackTileMargin() + game.getCarHeight();

        tilesPoints = new ArrayList<>();
        for (int i = 0; i <= 3; i++) {
            int wayPointIndex = curWaypointInd + i;
            if (wayPointIndex >= getWaypoints().length) {
                wayPointIndex = wayPointIndex - getWaypoints().length;
            }
            int waypointX = getWaypoints()[(wayPointIndex)][X];
            int waypointY = getWaypoints()[(wayPointIndex)][Y];

            tilesPoints.add(getPointsFromWayPoint(waypointX, waypointY));
        }
        FPoint startPoint = new FPoint(self.getX(), self.getY());
        Map<FPoint, Double> results = new HashMap<>();
        for (FPoint root : tilesPoints.get(0)) {
            results.put(root, getMinDistance(root, 1, startPoint.getDistanceTo(root) + 0));
        }

        Map.Entry<FPoint, Double> result = null;
        for (Map.Entry<FPoint, Double> entry : results.entrySet()) {
            if (result == null || entry.getValue() < result.getValue()) {
                result = entry;
            }
        }
        nextX = result.getKey().getX();
        nextY = result.getKey().getY();
    }

    private double getMinDistance(FPoint startPoint, int level, double sum) {
        if (level >= tilesPoints.size())
            return sum;
        List<Double> sums = new ArrayList<>();
        for (FPoint point : tilesPoints.get(0)) {
            sums.add(getMinDistance(point, level + 1, startPoint.getDistanceTo(point) + sum));
        }
        Double result = null;
        for (Double distance : sums) {
            if (result == null || distance < result) {
                result = distance;
            }
        }
        return result;
    }

    private FPoint[] getPointsFromWayPoint(int waypointX, int waypointY) {

        double cornerTileOffset = 0.4D * game.getTrackTileSize();

        int topX = (int) (waypointX * game.getTrackTileSize());
        int topY = (int) (waypointY * game.getTrackTileSize());

        int botX = (int) (topX + game.getTrackTileSize() - pointTileOffset);
        int botY = (int) (topY + game.getTrackTileSize() - pointTileOffset);

        topX += pointTileOffset;
        topY += pointTileOffset;

        switch (world.getTilesXY()[waypointX][waypointY]) {
            case LEFT_TOP_CORNER:
                topX += cornerTileOffset;
                topY += cornerTileOffset;
                break;
            case RIGHT_TOP_CORNER:
                botX -= cornerTileOffset;
                topY += cornerTileOffset;
                break;
            case LEFT_BOTTOM_CORNER:
                topX += cornerTileOffset;
                botY -= cornerTileOffset;
                break;
            case RIGHT_BOTTOM_CORNER:
                botX -= cornerTileOffset;
                botY -= cornerTileOffset;
                break;
        }

        int mediumX = topX + (botX - topX) / 2;
        int mediumY = topY + (botY - topY) / 2;
        return new FPoint[]{
                new FPoint(topX, topY), new FPoint(botX, topY),
                new FPoint(botX, botY), new FPoint(topX, botY),
                new FPoint(mediumX, mediumY),
                new FPoint(botX, mediumY), new FPoint(topX, mediumY),
                new FPoint(mediumX, botY), new FPoint(mediumX, topY)
        };
    }

    private TileType getTileType() {
        return world.getTilesXY()[getNextWaypointX()][getNextWaypointY()];
    }

    private int getNextWaypointY() {
        return self.getNextWaypointY();
    }

    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================


    private int[][] getMap05Waypoints() {
        if (customWaypoints == null) {
            customWaypoints = new int[][]{
                    new int[]{5, 12},
                    new int[]{4, 12},
                    new int[]{3, 12},
                    new int[]{2, 12},
                    new int[]{0, 14},
                    new int[]{1, 14},
                    new int[]{2, 14},
                    new int[]{8, 14},
                    new int[]{8, 13},
                    new int[]{8, 12},
                    new int[]{8, 11},
                    new int[]{8, 10},
                    new int[]{8, 9},
                    new int[]{8, 8},
                    new int[]{6, 8},
                    new int[]{6, 7},
                    new int[]{6, 6},
                    new int[]{7, 6},
                    new int[]{8, 6},
                    new int[]{8, 5},
                    new int[]{8, 4},
                    new int[]{8, 3},
                    new int[]{8, 2},
                    new int[]{8, 1},
                    new int[]{8, 0},
                    new int[]{7, 0},
                    new int[]{6, 0},
                    new int[]{5, 0},
                    new int[]{1, 0},
                    new int[]{0, 0},
                    new int[]{0, 1},
                    new int[]{0, 2},
                    new int[]{0, 3},
                    new int[]{0, 8},
                    new int[]{0, 9},
                    new int[]{0, 10},
                    new int[]{1, 10},
                    new int[]{2, 9},
                    new int[]{2, 8},
                    new int[]{2, 5},
                    new int[]{2, 4},
                    new int[]{2, 3},
                    new int[]{2, 2},
                    new int[]{3, 2},
                    new int[]{5, 2},
                    new int[]{6, 3},
                    new int[]{6, 4},
                    new int[]{5, 4},
                    new int[]{4, 4},
                    new int[]{4, 5},
                    new int[]{4, 6},
                    new int[]{4, 7},
                    new int[]{4, 9},
                    new int[]{4, 9},
                    new int[]{4, 10},
                    new int[]{5, 10},
                    new int[]{6, 10},
                    new int[]{6, 11},
                    new int[]{6, 12},
            };
        }
        return customWaypoints;
    }

    private int[][] getMap04Waypoints() {
        if (customWaypoints == null) {
            customWaypoints = new int[][]{
                    new int[]{9, 2},
                    new int[]{9, 3},
                    new int[]{9, 4},
                    new int[]{9, 5},
                    new int[]{9, 6},
                    new int[]{9, 7},
                    new int[]{9, 8},
                    new int[]{9, 9},
                    new int[]{8, 9},
                    new int[]{7, 9},
                    new int[]{7, 8},
                    new int[]{7, 7},
                    new int[]{6, 7},
                    new int[]{5, 7},
                    new int[]{4, 7},
                    new int[]{4, 8},
                    new int[]{4, 9},
                    new int[]{5, 9},
                    new int[]{6, 9},
                    new int[]{6, 8},
                    new int[]{6, 7},
                    new int[]{6, 6},
                    new int[]{6, 5},
                    new int[]{6, 4},
                    new int[]{5, 4},
                    new int[]{5, 3},
                    new int[]{4, 3},
                    new int[]{3, 3},
                    new int[]{2, 3},
                    new int[]{1, 3},
                    new int[]{0, 3},
                    new int[]{0, 4},
                    new int[]{0, 5},
                    new int[]{1, 5},
                    new int[]{2, 5},
                    new int[]{2, 4},
                    new int[]{2, 3},
                    new int[]{2, 2},
                    new int[]{1, 2},
                    new int[]{0, 2},
                    new int[]{0, 0},
                    new int[]{1, 0},
                    new int[]{2, 0},
                    new int[]{3, 0},
                    new int[]{4, 0},
                    new int[]{5, 0},
                    new int[]{7, 0},
                    new int[]{8, 0},
                    new int[]{9, 1},
            };
        }
        return customWaypoints;
    }

    private int[][] getMap03Waypoints() {
        if (customWaypoints == null) {
            customWaypoints = new int[][]{
                    new int[]{2, 6},
                    new int[]{2, 5},
                    new int[]{2, 4},
                    new int[]{3, 4},
                    new int[]{3, 3},
                    new int[]{3, 2},
                    new int[]{3, 1},
                    new int[]{3, 0},
                    new int[]{4, 0},
                    new int[]{4, 1},
                    new int[]{5, 1},
                    new int[]{5, 2},
                    new int[]{5, 2},
                    new int[]{6, 2},
                    new int[]{6, 3},
                    new int[]{6, 4},
                    new int[]{6, 5},
                    new int[]{6, 6},
                    new int[]{6, 7},
                    new int[]{5, 7},
                    new int[]{4, 7},
                    new int[]{3, 7},
                    new int[]{2, 7},
            };
        }
        return customWaypoints;
    }

    //===========================================================================================


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
        return (int) (v / debugKoef) + padding;
    }

    private int toTileIndex(double v) {
        return (int) (((v - sidePadding) * debugKoef) / game.getTrackTileSize());
    }

    private int dSize(double x) {
        return dSize(x, MyStrategy.this.sidePadding);
    }

    private class MyPanel extends JPanel {

        private Graphics2D g2;
        private int rectSize;
        private int margin;
        private int size;

        public MyPanel() {
            super();
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int x = toTileIndex(e.getX());
                    int y = toTileIndex(e.getY());
                    log("new int[]{" + x + "," + y + "},");
                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g2 = (Graphics2D) g;
            g2.setColor(Color.darkGray);
            g2.fillRect(sidePadding, sidePadding, panel.getWidth() - sidePadding * 2, panel.getHeight() - sidePadding * 2);

            rectSize = dSize(game.getTrackTileSize(), 0);
            margin = dSize(game.getTrackTileMargin(), 0);

            drawWaypoints();
            drawTiles();
            drawFPoints();
            drawCars();
            drawMyLines();
        }

        private void drawWaypoints() {
            g2.setColor(new Color(0x937026));
            for (int[] xy : world.getWaypoints()) {
                g2.fillRect(dSizeW(xy[X]), dSizeW(xy[Y]), rectSize, rectSize);
            }
            g2.setColor(new Color(0xC7A66E));
            for (int[] xy : getWaypoints()) {
                g2.fillRect(dSizeW(xy[X]) + margin * 2, dSizeW(xy[Y]) + margin * 2, rectSize - margin * 4, rectSize - margin * 4);
            }
        }

        private void drawFPoints() {
            g2.setColor(new Color(0xE2FF55));
            for (FPoint[] points : tilesPoints) {
                for (FPoint point : points) {
                    size = 5;
                    g2.fillRect(dSize(point.getX()) - size / 2, dSize(point.getY()) - size / 2, size, size);
                }
            }
        }

        private void drawTiles() {
            g2.setColor(Color.gray);
            for (int x = 0; x < world.getWidth(); x++) {
                for (int y = 0; y < world.getHeight(); y++) {
                    g2.setColor(Color.gray);
                    g2.drawRect(dSizeW(x), dSizeW(y), rectSize, rectSize);

                    g2.setColor(Color.orange);
                    switch (world.getTilesXY()[x][y]) {
                        case EMPTY:
                            break;
                        case VERTICAL:
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, rectSize);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y), margin, rectSize);
                            break;
                        case HORIZONTAL:
                            g2.fillRect(dSizeW(x), dSizeW(y), rectSize, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, rectSize, margin);
                            break;
                        case LEFT_TOP_CORNER:
                            g2.fillRect(dSizeW(x), dSizeW(y), rectSize, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, rectSize);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y) + rectSize - margin, margin, margin);
                            break;
                        case RIGHT_TOP_CORNER:
                            g2.fillRect(dSizeW(x), dSizeW(y), rectSize, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y), margin, rectSize);
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, margin, margin);
                            break;
                        case LEFT_BOTTOM_CORNER:
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, rectSize);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, rectSize, margin);
                            break;
                        case RIGHT_BOTTOM_CORNER:
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y), margin, rectSize);
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, rectSize, margin);
                            break;
                        case LEFT_HEADED_T:
                            break;
                        case RIGHT_HEADED_T:
                            break;
                        case TOP_HEADED_T:
                            break;
                        case BOTTOM_HEADED_T:
                            break;
                        case CROSSROADS:
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, margin, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y) + rectSize - margin, margin, margin);
                            break;
                    }
                }
            }
        }

        private int dSizeW(int v) {
            return dSize(v * game.getTrackTileSize());
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
                int x = (int) (dSize(car.getX()));
                int y = (int) (dSize(car.getY()));

                Rectangle r = new Rectangle(
                        (int) (0 - width / 2d), (int) (0 - height / 2d),
                        width, height);

                gg.translate(x, y);
                gg.rotate(car.getAngle());
                gg.draw(r);
                gg.fill(r);

                gg.dispose();

            }
        }


    }

    private class FPoint extends Unit {
        public FPoint(double x, double y) {
            super(0, 0, x, y, 0, 0, 0, 0);
        }

        public FPoint(int curWaypointInd) {
            this(getWaypoints()[curWaypointInd][0] * game.getTrackTileSize(), getWaypoints()[curWaypointInd][1] * game.getTrackTileSize());
        }

        @Override
        public String toString() {
            return "FPoint{" + getX() + " " + getY() + "}";
        }
    }
}
