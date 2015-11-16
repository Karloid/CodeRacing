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
    private static final String MAP_02 = "map02";
    private static final String MAP_01 = "map01";
    private static final String MAP_DEFAULT = "default";
    private static final String MAP_06 = "map06";
    public static final String MAP_05 = "map05";
    public static final int MAX_SPEED = 32;
    private static final String MAP_07 = "map07";
    private static final String MAP_08 = "map08";

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

    private int[] lastWaypoint;
    private int lastWaypointInd;
    private HashMap<String, List<int[]>> slowTilesMap;
    private HashMap<String, List<int[]>> nitroTilesMap;
    private Map<String, int[][]> customMapWaypoints;
    private int[] tmpWaypointTile;

    @Override
    public void move(Car self, World world, Game game, Move move) {

        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        findCurrentWaypoint();

        doMove();

        doStatistics();
        log("speed + " + f(speedModule) + " brake: " + move.isBrake());

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
        if (haveWaypoints()) {


            int[] currentWaypoint = getWaypoints()[this.curWaypointInd];

            int previousInd = this.curWaypointInd - 1;
            if (previousInd < 0) {
                previousInd = getWaypoints().length - 1;
            }
            int[] previousTile = getWaypoints()[previousInd];

            if (getCurTileX() == currentWaypoint[0] && getCurTileY() == currentWaypoint[1]) {
                lastWaypoint = currentWaypoint;
                lastWaypointInd = curWaypointInd;
                this.curWaypointInd++;
                if (this.curWaypointInd >= getWaypoints().length) {
                    this.curWaypointInd = 0;
                }


            } else if (lastWaypoint != null && (getCurTileX() != lastWaypoint[X] || getCurTileY() != lastWaypoint[Y])) {
                this.curWaypointInd = lastWaypointInd;
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

    private boolean haveWaypoints() {
        return isMap(MAP_DEFAULT) || isMap(MAP_01) || isMap(MAP_07) || isMap(MAP_08) || isMap(MAP_02) || isMap(MAP_03) || isMap04() || isMap05() || isMap(MAP_06);
    }

    private int getCurTileY() {
        return (int) (self.getY() / game.getTrackTileSize());
    }

    private int getCurTileX() {
        return (int) (self.getX() / game.getTrackTileSize());
    }

    private int getNextWaypointX() {
        return self.getNextWaypointX();
    }

    private int[][] getWaypoints() {
        if (customMapWaypoints == null) {
            customMapWaypoints = new HashMap<>();
            customMapWaypoints.put(MAP_DEFAULT, getMapDefaultWaypoints());
            customMapWaypoints.put(MAP_01, getMap01Waypoints());
            customMapWaypoints.put(MAP_02, getMap02Waypoints());
            customMapWaypoints.put(MAP_03, getMap03Waypoints());
            customMapWaypoints.put(MAP_04, getMap04Waypoints());
            customMapWaypoints.put(MAP_05, getMap05Waypoints());
            customMapWaypoints.put(MAP_06, getMap06Waypoints());
            customMapWaypoints.put(MAP_07, getMap07Waypoints());
            customMapWaypoints.put(MAP_08, getMap08Waypoints());
        }
        int[][] waypoints = customMapWaypoints.get(world.getMapName());
        if (waypoints != null)
            return waypoints;
        return world.getWaypoints();
    }

    private int[][] getMap08Waypoints() {
        return new int[][]{
                new int[]{10, 11},
                new int[]{9, 11},
                new int[]{8, 11},
                new int[]{7, 11},
                new int[]{6, 11},
                new int[]{5, 11},
                new int[]{4, 11},
                new int[]{3, 11},
                new int[]{3, 10},
                new int[]{3, 9},
                new int[]{4, 9},
                new int[]{5, 9},
                new int[]{6, 9},
                new int[]{7, 9},
                new int[]{8, 9},
                new int[]{8, 10},
                new int[]{8, 11},
                new int[]{7, 11},
                new int[]{6, 11},
                new int[]{5, 11},
                new int[]{4, 11},
                new int[]{3, 11},
                new int[]{2, 11},
                new int[]{1, 11},
                new int[]{0, 11},
                new int[]{0, 10},
                new int[]{0, 9},
                new int[]{0, 8},
                new int[]{0, 7},
                new int[]{0, 6},
                new int[]{0, 5},
                new int[]{0, 4},
                new int[]{0, 3},
                new int[]{0, 2},
                new int[]{0, 1},
                new int[]{1, 1},
                new int[]{1, 0},
                new int[]{2, 0},
                new int[]{3, 0},
                new int[]{4, 0},
                new int[]{5, 0},
                new int[]{6, 0},
                new int[]{7, 0},
                new int[]{8, 0},
                new int[]{9, 0},
                new int[]{10, 0},
                new int[]{11, 0},
                new int[]{11, 1},
                new int[]{11, 2},
                new int[]{11, 3},
                new int[]{10, 3},
                new int[]{9, 3},
                new int[]{8, 3},
                new int[]{8, 4},
                new int[]{8, 5},
                new int[]{8, 6},
                new int[]{8, 7},
                new int[]{9, 7},
                new int[]{10, 7},
                new int[]{11, 7},
                new int[]{11, 8},
                new int[]{11, 9},
                new int[]{11, 10},
                new int[]{11, 11},
        };
    }

    private int[][] getMap07Waypoints() {
        return new int[][]{new int[]{0, 13},
                new int[]{0, 12},
                new int[]{0, 11},
                new int[]{0, 10},
                new int[]{1, 10},
                new int[]{1, 9},
                new int[]{2, 9},
                new int[]{2, 8},
                new int[]{3, 8},
                new int[]{3, 7},
                new int[]{4, 7},
                new int[]{4, 6},
                new int[]{5, 6},
                new int[]{5, 5},
                new int[]{6, 5},
                new int[]{6, 4},
                new int[]{7, 4},
                new int[]{7, 3},
                new int[]{8, 3},
                new int[]{8, 2},
                new int[]{9, 2},
                new int[]{9, 1},
                new int[]{10, 1},
                new int[]{10, 0},
                new int[]{11, 0},
                new int[]{12, 0},
                new int[]{13, 0},
                new int[]{14, 0},
                new int[]{14, 1},
                new int[]{15, 1},
                new int[]{15, 2},
                new int[]{15, 3},
                new int[]{15, 4},
                new int[]{15, 5},
                new int[]{15, 6},
                new int[]{15, 7},
                new int[]{15, 8},
                new int[]{15, 9},
                new int[]{15, 10},
                new int[]{15, 11},
                new int[]{15, 12},
                new int[]{15, 13},
                new int[]{15, 14},
                new int[]{14, 14},
                new int[]{14, 15},
                new int[]{13, 15},
                new int[]{12, 15},
                new int[]{11, 15},
                new int[]{10, 15},
                new int[]{9, 15},
                new int[]{8, 15},
                new int[]{7, 15},
                new int[]{6, 15},
                new int[]{5, 15},
                new int[]{4, 15},
                new int[]{3, 15},
                new int[]{2, 15},
                new int[]{1, 15},
                new int[]{1, 14},
                new int[]{0, 14},};
    }


    private boolean isMap05() {
        return isMap(MAP_05);
    }

    private boolean isMap(String mapName) {
        return world.getMapName().equals(mapName);
    }

    private boolean isMap04() {
        return isMap(MAP_04);
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

        if (distanceToWaypoint < game.getTrackTileSize() / 1.5f && isCorner(curWaypointInd) && self.getOilCanisterCount() > 0 && notLast()) {
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
        float carefulCof = self.getDurability() < 0.15d ? 0.8f : 1f;
        float maxSpeed = MAX_SPEED * carefulCof;
        boolean tooFast = speedModule > maxSpeed && curWaypointInd != 0;
        float maxSpeedOnCorner = 14 * carefulCof;
        boolean tooFastCorner = speedModule > maxSpeedOnCorner && isCorner(curWaypointInd) && self.getDistanceTo(new FPoint(nextX, nextY)) < game.getTrackTileSize() * 0.8f;
        if (tooFastCorner) log("TOO fast for corner!");

        boolean slowTile = speedModule > maxSpeedOnCorner && isSlowTile();
        if (slowTile) log("is slowTile!");

        return (angleStuff || tooFast || tooFastCorner || slowTile) && (!haveWaypoints() || !isNitroTitle());
    }

    private boolean isSlowTile() {
        int[] curTile = getCurTile();
        for (int[] slowTile : getSlowTiles()) {
            if (tilesIsEqual(curTile, slowTile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNitroTitle() {
        int[] curTile = getCurTile();
        for (int[] nitroTile : getNitroTiles()) {
            if (tilesIsEqual(curTile, nitroTile)) {
                return true;
            }
        }
        return false;
    }

    private boolean tilesIsEqual(int[] curTile, int[] slowTile) {
        return slowTile[X] == curTile[X] && slowTile[Y] == curTile[Y];
    }

    private int[] getCurTile() {
        return new int[]{getCurTileX(), getCurTileY()};
    }

    private boolean isCorner(int curWaypointInd) {
        int[] xy = getWaypoints()[curWaypointInd];
        TileType tileType = world.getTilesXY()[xy[0]][xy[1]];
        return tileType == TileType.LEFT_BOTTOM_CORNER || tileType == TileType.RIGHT_BOTTOM_CORNER || tileType == TileType.LEFT_TOP_CORNER || tileType == TileType.RIGHT_TOP_CORNER;
    }

    private boolean isTimeForNitro() { //TODO
        if (haveWaypoints()) {
            return abs(angleToWaypoint) < 0.15f && isNitroTitle() && game.getInitialFreezeDurationTicks() < world.getTick() && self.getNitroChargeCount() > 0;
        }
        return abs(angleToWaypoint) < 0.1f && (distanceToWaypoint > game.getTrackTileSize() * 3 || curWaypointInd == 0) && game.getInitialFreezeDurationTicks() < world.getTick() && self.getNitroChargeCount() > 0;
    }

    private void doWheelTurn() {

        findNextXY();

        distanceToWaypoint = self.getDistanceTo(nextX, nextY);

        angleToWaypoint = self.getAngleTo(nextX, nextY);
        speedModule = hypot(self.getSpeedX(), self.getSpeedY());

  /*      if (abs(angleToWaypoint) > 2f && getMoveBackWardDelta() >= 0 && getMoveBackWardDelta() < TICKS_COUNT_FOR_DISTANCE + GAP) {
            if (angleToWaypoint > 0) {
                angleToWaypoint = angleToWaypoint - PI;
            } else {
                angleToWaypoint = PI + angleToWaypoint;
            }
            reverseMove = true;
        } else {
            reverseMove = false;
        }*/


        // log("angleToWaypoint: " + angleToWaypoint);
        int reverseMoveFactor = reverseMove ? -1 : 1;
        int backwardPower = -1 * reverseMoveFactor;
        double normalPower = (isSlowTile() ? 0.75d : 1d) * reverseMoveFactor * getMap03HackSpeedFactor();

        if (getMoveBackWardDelta() >= 0 && getMoveBackWardDelta() < TICKS_COUNT_FOR_DISTANCE) {
            setWheelTurn(angleToWaypoint * -1 * reverseMoveFactor);
            move.setEnginePower(backwardPower);
        } else {
            if (getMoveBackWardDelta() <=
                    TICKS_COUNT_FOR_DISTANCE + GAP && getMoveBackWardDelta() >= TICKS_COUNT_FOR_DISTANCE) {
                setWheelTurn(angleToWaypoint * -1 * reverseMoveFactor);
            } else {
                setWheelTurn(angleToWaypoint * reverseMoveFactor);
            }
            move.setEnginePower(normalPower);
        }
    }

    private double getMap03HackSpeedFactor() {
        if (!isMap(MAP_03) || world.getMyPlayer().getId() == 4) return 1d;
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

        double cornerTileOffset = 0.5D * game.getTrackTileSize();

        int topX = (int) (waypointX * game.getTrackTileSize());
        int topY = (int) (waypointY * game.getTrackTileSize());

        int botX = (int) (topX + game.getTrackTileSize() - pointTileOffset);
        int botY = (int) (topY + game.getTrackTileSize() - pointTileOffset);

        topX += pointTileOffset;
        topY += pointTileOffset;

        TileType tileType = world.getTilesXY()[waypointX][waypointY];

        tmpWaypointTile = new int[]{waypointX, waypointY};

        if (isMap(MAP_03) && tilesIsEqual(new int[]{3, 0}, tmpWaypointTile)) {
            tileType = TileType.LEFT_TOP_CORNER;
        } else if (isMap(MAP_06)) {
            tileType = getHackyTileType(tileType, new int[]{7, 13}, TileType.RIGHT_BOTTOM_CORNER);
            tileType = getHackyTileType(tileType, new int[]{9, 13}, TileType.LEFT_BOTTOM_CORNER);
            tileType = getHackyTileType(tileType, new int[]{14, 14}, TileType.LEFT_TOP_CORNER);
            tileType = getHackyTileType(tileType, new int[]{14, 14}, TileType.LEFT_TOP_CORNER);
            tileType = getHackyTileType(tileType, new int[]{13, 13}, TileType.RIGHT_BOTTOM_CORNER);
            tileType = getHackyTileType(tileType, new int[]{6, 13}, TileType.LEFT_TOP_CORNER);
        } else if (isMap(MAP_08)) {
            if (curWaypointInd > 12)
                tileType = getHackyTileType(tileType, new int[]{8, 11}, TileType.RIGHT_BOTTOM_CORNER);
            if (curWaypointInd < 8)
                tileType = getHackyTileType(tileType, new int[]{3, 11}, TileType.LEFT_BOTTOM_CORNER);
        }

        double cornerTileSideOffset = cornerTileOffset / 6;
        switch (tileType) {
            case LEFT_TOP_CORNER:
                topX += cornerTileOffset;
                topY += cornerTileOffset;
                botX += cornerTileSideOffset;
                botY += cornerTileSideOffset;
                break;
            case RIGHT_TOP_CORNER:
                botX -= cornerTileOffset;
                topY += cornerTileOffset;
                topX -= cornerTileSideOffset;
                botY += cornerTileSideOffset;
                break;
            case LEFT_BOTTOM_CORNER:
                topX += cornerTileOffset;
                botY -= cornerTileOffset;
                botX += cornerTileSideOffset;
                topY -= cornerTileSideOffset;
                break;
            case RIGHT_BOTTOM_CORNER:
                botX -= cornerTileOffset;
                botY -= cornerTileOffset;
                topX -= cornerTileSideOffset;
                topY -= cornerTileSideOffset;
                break;
       /*     default:
                topX += cornerTileOffset/4;
                topY += cornerTileOffset/4;
                botX -= cornerTileOffset/4;
                botY -= cornerTileOffset/4;*/
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

    private TileType getHackyTileType(TileType origin, int[] checkOn, TileType type) {
        if (tilesIsEqual(checkOn, tmpWaypointTile)) {
            origin = type;
        }
        return origin;
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
        private Color fontColor = new Color(0xE1FFF1);

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

            drawTexts();
            drawWaypoints();
            drawTiles();
            drawFPoints();
            drawCars();
            drawMyLines();
        }

        private void drawTexts() {
            int stringPadding = sidePadding / 2;
            g2.drawString("speed: " + f(speedModule), stringPadding, sidePadding / 2);
            g2.drawString("max_speed: " + f(MAX_SPEED), stringPadding * 5, sidePadding / 2);
            g2.drawString("enigePower: " + f(self.getEnginePower()), stringPadding * 10, sidePadding / 2);
            //  g2.drawString("angle: " + f(angleToWaypoint), stringPadding * 10, sidePadding/2);
        }

        private void drawWaypoints() {
            int i = 0;
            for (int[] xy : world.getWaypoints()) {
                int x = dSizeW(xy[X]);
                int y = dSizeW(xy[Y]);
                g2.setColor(new Color(0x937026));
                g2.fillRect(x, y, rectSize, rectSize);
                g2.setColor(fontColor);

                g2.drawString(i + "", x + rectSize - 20, y + 25);
                i++;
            }
            i = 0;
            g2.setColor(new Color(0xC7A66E));
            for (int[] xy : getWaypoints()) {
                int x = dSizeW(xy[X]) + margin * 2;
                int y = dSizeW(xy[Y]) + margin * 2;
                g2.setColor(new Color(0xC7A66E));
                g2.fillRect(x, y, rectSize - margin * 4, rectSize - margin * 4);
                g2.setColor(fontColor);
                g2.drawString(i + "", x + 3, y + 15);
                i++;
            }

            g2.setColor(new Color(0xC78FB3));
            for (int[] xy : getSlowTiles()) {
                g2.fillRect(dSizeW(xy[X]) + margin * 3, dSizeW(xy[Y]) + margin * 3, rectSize - margin * 6, rectSize - margin * 6);
            }

            g2.setColor(new Color(0xDD2CC9));
            for (int[] xy : getNitroTiles()) {
                g2.fillOval(dSizeW(xy[X]) + margin * 3, dSizeW(xy[Y]) + margin * 3, rectSize - margin * 6, rectSize - margin * 6);
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
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y), margin, rectSize);
                            break;
                        case RIGHT_HEADED_T:
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, rectSize);
                            break;
                        case TOP_HEADED_T:
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, rectSize, margin);
                            break;
                        case BOTTOM_HEADED_T:
                            g2.fillRect(dSizeW(x), dSizeW(y), rectSize, margin);
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

            g2.setColor(new Color(0xFF00D7));
            g2.drawLine(dSize(self.getX()), dSize(self.getY()), dSize(self.getX() + self.getSpeedX() * 15), dSize(self.getY() + self.getSpeedY() * 15));
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

    private int[][] getMap06Waypoints() {
        return new int[][]{
                new int[]{13, 15},
                new int[]{12, 15},
                new int[]{11, 15},
                new int[]{10, 15},
                new int[]{9, 15},
                new int[]{8, 15},
                new int[]{7, 15},
                new int[]{6, 15},
                new int[]{5, 15},
                new int[]{4, 15},
                new int[]{3, 15},
                new int[]{2, 15},
                new int[]{1, 15},
                new int[]{0, 15},
                new int[]{0, 14},
                new int[]{0, 13},
                new int[]{0, 12},
                new int[]{0, 11},
                new int[]{0, 10},
                new int[]{0, 9},
                new int[]{0, 8},
                new int[]{0, 7},
                new int[]{0, 6},
                new int[]{0, 5},
                new int[]{0, 4},
                new int[]{0, 3},
                new int[]{0, 2},
                new int[]{0, 1},
                new int[]{0, 0},
                new int[]{1, 0},
                new int[]{2, 0},
                new int[]{2, 1},
                new int[]{2, 2},
                new int[]{2, 3},
                new int[]{2, 4},
                new int[]{2, 5},
                new int[]{2, 6},
                new int[]{2, 7},
                new int[]{2, 8},
                new int[]{2, 9},
                new int[]{2, 10},
                new int[]{2, 11},
                new int[]{2, 12},
                new int[]{2, 13},
                new int[]{2, 14},
                new int[]{3, 14},
                new int[]{4, 14},
                new int[]{5, 14},
                new int[]{6, 14},
                new int[]{6, 13},
                new int[]{7, 13},
                new int[]{7, 12},
                new int[]{8, 12},
                new int[]{9, 12},
                new int[]{9, 13},
                new int[]{10, 13},
                new int[]{11, 13},
                new int[]{12, 13},
                new int[]{13, 13},
                new int[]{13, 12},
                new int[]{14, 12},
                new int[]{15, 12},
                new int[]{15, 13},
                new int[]{15, 14},
                new int[]{14, 14},
                new int[]{14, 15},
        };
    }

    private int[][] getMapDefaultWaypoints() {
        return new int[][]{
                new int[]{0, 6},
                new int[]{0, 5},
                new int[]{0, 4},
                new int[]{0, 3},
                new int[]{0, 2},
                new int[]{0, 1},
                new int[]{0, 0},
                new int[]{1, 0},
                new int[]{2, 0},
                new int[]{3, 0},
                new int[]{4, 0},
                new int[]{5, 0},
                new int[]{6, 0},
                new int[]{7, 0},
                new int[]{7, 1},
                new int[]{7, 2},
                new int[]{7, 3},
                new int[]{7, 4},
                new int[]{7, 5},
                new int[]{7, 6},
                new int[]{7, 7},
                new int[]{6, 7},
                new int[]{5, 7},
                new int[]{4, 7},
                new int[]{3, 7},
                new int[]{2, 7},
                new int[]{1, 7},
                new int[]{0, 7},
        };
    }

    private int[][] getMap01Waypoints() {
        return new int[][]{
                new int[]{0, 6},
                new int[]{0, 5},
                new int[]{0, 4},
                new int[]{1, 4},
                new int[]{2, 4},
                new int[]{3, 4},
                new int[]{4, 4},
                new int[]{5, 4},
                new int[]{6, 4},
                new int[]{7, 4},
                new int[]{7, 3},
                new int[]{7, 2},
                new int[]{7, 1},
                new int[]{7, 0},
                new int[]{6, 0},
                new int[]{5, 0},
                new int[]{4, 0},
                new int[]{3, 0},
                new int[]{3, 1},
                new int[]{3, 2},
                new int[]{3, 3},
                new int[]{3, 4},
                new int[]{3, 5},
                new int[]{3, 6},
                new int[]{3, 7},
                new int[]{2, 7},
                new int[]{1, 7},
                new int[]{0, 7},
        };
    }

    private int[][] getMap02Waypoints() {
        return new int[][]{
                new int[]{3, 6},
                new int[]{3, 5},
                new int[]{3, 4},
                new int[]{3, 3},
                new int[]{3, 2},
                new int[]{3, 1},
                new int[]{3, 0},
                new int[]{2, 0},
                new int[]{1, 0},
                new int[]{0, 0},
                new int[]{0, 1},
                new int[]{0, 2},
                new int[]{0, 3},
                new int[]{1, 3},
                new int[]{2, 3},
                new int[]{3, 3},
                new int[]{4, 3},
                new int[]{5, 3},
                new int[]{6, 3},
                new int[]{7, 3},
                new int[]{7, 2},
                new int[]{7, 1},
                new int[]{7, 0},
                new int[]{6, 0},
                new int[]{5, 0},
                new int[]{4, 0},
                new int[]{4, 1},
                new int[]{4, 2},
                new int[]{4, 3},
                new int[]{4, 4},
                new int[]{4, 5},
                new int[]{4, 6},
                new int[]{4, 7},
                new int[]{5, 7},
                new int[]{6, 7},
                new int[]{7, 7},
                new int[]{7, 6},
                new int[]{7, 5},
                new int[]{7, 4},
                new int[]{6, 4},
                new int[]{5, 4},
                new int[]{4, 4},
                new int[]{3, 4},
                new int[]{2, 4},
                new int[]{1, 4},
                new int[]{0, 4},
                new int[]{0, 5},
                new int[]{0, 6},
                new int[]{0, 7},
                new int[]{1, 7},
                new int[]{2, 7},
                new int[]{3, 7}
        };
    }


    private int[][] getMap05Waypoints() {
        customWaypoints = new int[][]{
                new int[]{5, 12},
                new int[]{4, 12},
                new int[]{3, 12},
                new int[]{2, 12},
                new int[]{1, 12},
                new int[]{0, 12},
                new int[]{0, 13},
                new int[]{0, 14},
                new int[]{1, 14},
                new int[]{2, 14},
                new int[]{2, 14},
                new int[]{3, 14},
                new int[]{4, 14},
                new int[]{5, 14},
                new int[]{6, 14},
                new int[]{7, 14},
                new int[]{8, 14},
                new int[]{8, 13},
                new int[]{8, 12},
                new int[]{8, 11},
                new int[]{8, 10},
                new int[]{8, 9},
                new int[]{8, 8},
                new int[]{7, 8},
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
                new int[]{4, 0},
                new int[]{3, 0},
                new int[]{2, 0},
                new int[]{1, 0},
                new int[]{0, 0},
                new int[]{0, 1},
                new int[]{0, 2},
                new int[]{0, 3},
                new int[]{0, 3},
                new int[]{0, 4},
                new int[]{0, 5},
                new int[]{0, 6},
                new int[]{0, 7},
                new int[]{0, 8},
                new int[]{0, 9},
                new int[]{0, 10},
                new int[]{1, 10},
                new int[]{2, 10},
                new int[]{2, 9},
                new int[]{2, 8},
                new int[]{2, 7},
                new int[]{2, 6},
                new int[]{2, 5},
                new int[]{2, 4},
                new int[]{2, 3},
                new int[]{2, 2},
                new int[]{3, 2},
                new int[]{4, 2},
                new int[]{5, 2},
                new int[]{6, 2},
                new int[]{6, 3},
                new int[]{6, 4},
                new int[]{5, 4},
                new int[]{4, 4},
                new int[]{4, 5},
                new int[]{4, 6},
                new int[]{4, 7},
                new int[]{4, 8},
                new int[]{4, 9},
                new int[]{4, 9},
                new int[]{4, 10},
                new int[]{5, 10},
                new int[]{6, 10},
                new int[]{6, 11},
                new int[]{6, 12},
        };
        return customWaypoints;
    }

    private int[][] getMap04Waypoints() {
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
                new int[]{0, 1},
                new int[]{0, 0},
                new int[]{1, 0},
                new int[]{2, 0},
                new int[]{3, 0},
                new int[]{4, 0},
                new int[]{5, 0},
                new int[]{6, 0},
                new int[]{7, 0},
                new int[]{8, 0},
                new int[]{8, 1},
                new int[]{9, 1},
        };
        return customWaypoints;
    }

    private int[][] getMap03Waypoints() {
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
        return customWaypoints;
    }


    private List<int[]> getSlowTiles() {
        if (slowTilesMap == null) {
            slowTilesMap = new HashMap<>();
            slowTilesMap.put(MAP_03, Arrays.asList(
                    new int[]{3, 1},
                    new int[]{3, 0},
                    new int[]{4, 0},
                    new int[]{4, 1},
                    new int[]{5, 1},
                    new int[]{5, 2},
                    new int[]{6, 2},
                    new int[]{2, 5},
                    new int[]{2, 4}
            ));
            slowTilesMap.put(MAP_06, Arrays.asList(
                    new int[]{7, 12},
                    new int[]{8, 12},
                    new int[]{9, 12},
                    new int[]{9, 13},
                    new int[]{3, 13},
                    new int[]{2, 13},
                    new int[]{2, 14},
                    new int[]{3, 12},
                    new int[]{10, 13},
                    new int[]{1, 15},
                    new int[]{0, 1}
            ));
        }
        List<int[]> slowTiles = slowTilesMap.get(world.getMapName());
        if (slowTiles == null) {
            slowTiles = new ArrayList<>();
        }
        return slowTiles;
    }

    private List<int[]> getNitroTiles() {
        if (nitroTilesMap == null) {
            nitroTilesMap = new HashMap<>();
            nitroTilesMap.put(MAP_06, Arrays.asList(
                    new int[]{12, 15},
                    new int[]{11, 15},
                    new int[]{10, 15},
                    new int[]{9, 15},
                    new int[]{8, 15},
                    new int[]{7, 15},
                    new int[]{0, 12},
                    new int[]{0, 11},
                    new int[]{0, 10},
                    new int[]{0, 9},
                    new int[]{0, 8},
                    new int[]{2, 2},
                    new int[]{2, 3},
                    new int[]{2, 4},
                    new int[]{2, 5},
                    new int[]{2, 6},
                    new int[]{2, 7}
            ));
            nitroTilesMap.put(MAP_07, Arrays.asList(
                    new int[]{9, 7},
                    new int[]{0, 14},
                    new int[]{0, 13},
                    new int[]{0, 12},
                    new int[]{11, 0},
                    new int[]{15, 1},
                    new int[]{15, 2},
                    new int[]{15, 3},
                    new int[]{15, 4},
                    new int[]{15, 5},
                    new int[]{15, 6},
                    new int[]{15, 7},
                    new int[]{15, 8},
                    new int[]{15, 9},
                    new int[]{15, 10},
                    new int[]{15, 11},
                    new int[]{13, 15},
                    new int[]{12, 15},
                    new int[]{11, 15},
                    new int[]{10, 15},
                    new int[]{9, 15},
                    new int[]{8, 15},
                    new int[]{7, 15},
                    new int[]{6, 15},
                    new int[]{5, 15},
                    new int[]{4, 15},
                    new int[]{10, 0},
                    new int[]{3, 15},
                    new int[]{1, 14},
                    new int[]{1, 15}
            ));
            nitroTilesMap.put(MAP_05, Arrays.asList(
                    new int[]{4, 14},
                    new int[]{2, 14},
                    new int[]{3, 14},
                    new int[]{8, 13},
                    new int[]{8, 12},
                    new int[]{8, 5},
                    new int[]{8, 4},
                    new int[]{7, 0},
                    new int[]{6, 0},
                    new int[]{5, 0},
                    new int[]{0, 1},
                    new int[]{0, 2},
                    new int[]{0, 3},
                    new int[]{0, 4},
                    new int[]{2, 9},
                    new int[]{2, 8},
                    new int[]{2, 7},
                    new int[]{5, 12},
                    new int[]{4, 5},
                    new int[]{4, 6},
                    new int[]{4, 7}
            ));

            nitroTilesMap.put(MAP_04, Arrays.asList(
                    new int[]{9, 5},
                    new int[]{9, 4},
                    new int[]{9, 3},
                    new int[]{9, 2},
                    new int[]{6, 8},
                    new int[]{4, 3},
                    new int[]{3, 3},
                    new int[]{1, 0},
                    new int[]{2, 0},
                    new int[]{3, 0}
            ));

            nitroTilesMap.put(MAP_03, Arrays.asList(
                    new int[]{6, 3},
                    new int[]{6, 4},
                    new int[]{5, 7},
                    new int[]{5, 7},
                    new int[]{4, 7}
            ));

            nitroTilesMap.put(MAP_02, Arrays.asList(
                    new int[]{3, 6},
                    new int[]{3, 5},
                    new int[]{3, 4},
                    new int[]{1, 3},
                    new int[]{2, 3},
                    new int[]{3, 3},
                    new int[]{4, 1},
                    new int[]{4, 2},
                    new int[]{4, 3},
                    new int[]{6, 4},
                    new int[]{5, 4},
                    new int[]{4, 4}
            ));

            nitroTilesMap.put(MAP_01, Arrays.asList(
                    new int[]{1, 4},
                    new int[]{2, 4},
                    new int[]{3, 4},
                    new int[]{7, 3},
                    new int[]{6, 0},
                    new int[]{3, 1},
                    new int[]{3, 2},
                    new int[]{3, 3}
            ));

            nitroTilesMap.put(MAP_DEFAULT, Arrays.asList(
                    new int[]{0, 6},
                    new int[]{0, 5},
                    new int[]{0, 4},
                    new int[]{1, 0},
                    new int[]{3, 0},
                    new int[]{7, 1},
                    new int[]{7, 2},
                    new int[]{7, 3},
                    new int[]{6, 7},
                    new int[]{5, 7},
                    new int[]{4, 7}
            ));
        }
        List<int[]> slowTiles = nitroTilesMap.get(world.getMapName());
        if (slowTiles == null) {
            slowTiles = new ArrayList<>();
        }
        return slowTiles;
    }

}
