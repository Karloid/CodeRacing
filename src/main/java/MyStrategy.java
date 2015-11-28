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
    public static final int TICKS_COUNT_FOR_DISTANCE = 90;
    public static final int GAP = 40;
    private static final String MAP_02 = "map02";
    private static final String MAP_01 = "map01";
    private static final String MAP_DEFAULT = "default";
    private static final String MAP_06 = "map06";
    public static final String MAP_05 = "map05";
    public static final int MAX_SPEED = 32;
    private static final String MAP_07 = "map07";
    private static final String MAP_08 = "map08";
    private static final String MAP_09 = "map09";

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
    private LinkedHashMap<Integer, Double> distanceQueue;
    private int moveBackwardPoint = -TICKS_COUNT_FOR_DISTANCE;
    private boolean reverseMove;

    private MyTile lastWaypoint;
    private int lastWaypointInd;
    private HashMap<String, List<MyTile>> slowTilesMap;
    private HashMap<String, List<MyTile>> nitroTilesMap;
    private Map<String, MyTile[]> customMapWaypoints;
    private MyTile tmpWaypointTile;

    private PolygonsWorld polygonWorld;

    @Override
    public void move(Car self, World world, Game game, Move move) {

        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        initAStar();

        findCurrentWaypoint();

        doMove();

        doStatistics();
        log("speed + " + f(speedModule) + " brake: " + move.isBrake());

        drawWindow();
    }

    private void initAStar() {
        if (polygonWorld == null) {
            polygonWorld = new PolygonsWorld(30, 20);
        }
        polygonWorld.setStartPoint(new Point(0, 0, polygonWorld));
        polygonWorld.setEndPoint(new Point(5, 5, polygonWorld));
        polygonWorld.calcViewGraph();
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
            if (sum < 140 && getMoveBackWardDelta() > TICKS_COUNT_FOR_DISTANCE * 3) {
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


            MyTile currentWaypoint = getWaypoints().get(this.curWaypointInd);

            int previousInd = this.curWaypointInd - 1;
            if (previousInd < 0) {
                previousInd = getWaypoints().size() - 1;
            }
            MyTile previousTile = getWaypoints().get(previousInd);

            if (getCurTileX() == currentWaypoint.x && getCurTileY() == currentWaypoint.y) {
                lastWaypoint = currentWaypoint;
                lastWaypointInd = curWaypointInd;
                this.curWaypointInd++;
                if (this.curWaypointInd >= getWaypoints().size()) {
                    this.curWaypointInd = 0;
                }


            } else if (lastWaypoint != null && (getCurTileX() != lastWaypoint.x || getCurTileY() != lastWaypoint.y)) {
                this.curWaypointInd = lastWaypointInd;
            }
        } else {
            boolean find = false;
            int realNextX = getNextWaypointX();
            int realNextY = getNextWaypointY();
            for (; curWaypointInd < getWaypoints().size(); curWaypointInd++) {
                MyTile xy = getWaypoints().get(curWaypointInd);
                if (xy.x == realNextX && xy.y == realNextY) {
                    find = true;
                    break;
                }
            }
            if (!find) {
                curWaypointInd = 0;
                MyTile xy = getWaypoints().get(curWaypointInd);
                if (xy.x != realNextX || xy.y != realNextY) {
                    log("SUPER ERROR: Incorrect waypoint index");
                }
            }
        }
    }

    private boolean haveWaypoints() {
        return isMap(MAP_DEFAULT) || isMap(MAP_01) || isMap(MAP_07) || isMap(MAP_08) || isMap(MAP_09) || isMap(MAP_02) || isMap(MAP_03) || isMap04() || isMap05() || isMap(MAP_06);
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

    private <E extends Enum<E>> List<MyTile> getWaypoints() {
       /* if (customMapWaypoints == null) {
            customMapWaypoints = new HashMap<>();
          
        }*/
        List<MyTile> waypoints = Collections.emptyList();
        List<MyTile> result = new ArrayList<>();
        for (int[] xy : world.getWaypoints()) {
            result.add(new MyTile(xy));
        }
        return result;
        //return waypoints;
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
        MyTile curTile = getCurTile();
        for (MyTile slowTile : getSlowTiles()) {
            if (curTile.equals(slowTile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNitroTitle() {
        MyTile curTile = getCurTile();
        for (MyTile tile : getNitroTiles()) {
            if (curTile.equals(tile)) {
                return true;
            }
        }
        return false;
    }

    private MyTile getCurTile() {
        return new MyTile(getCurTileX(), getCurTileY());
    }

    private boolean isCorner(int curWaypointInd) {
        MyTile xy = getWaypoints().get(curWaypointInd);
        return isCorner(world.getTilesXY()[xy.x][xy.y]);
    }

    private boolean isCorner(TileType tileType) {
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
        if (isNitroTitle() && !isCorner(world.getTilesXY()[getCurTileX()][getCurTileY()]) && speedModule > 10) {
            move.setWheelTurn(v);
        } else {
            move.setWheelTurn(v * 32.0D / PI);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void findNextXY() {
        pointTileOffset = game.getTrackTileMargin() + game.getCarHeight();

        tilesPoints = new ArrayList<>();
        for (int i = 0; i <= 3; i++) {
            int wayPointIndex = curWaypointInd + i;
            if (wayPointIndex >= getWaypoints().size()) {
                wayPointIndex = wayPointIndex - getWaypoints().size();
            }
            int waypointX = getWaypoints().get(wayPointIndex).x;
            int waypointY = getWaypoints().get(wayPointIndex).y;

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

        tmpWaypointTile = new MyTile(waypointX, waypointY);

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

    private int dSize(double v) {
        return dSize(v, MyStrategy.this.sidePadding);
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
                    log("new MyTile{" + x + "," + y + "},");
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
            drawAStar();
        }

        private void drawAStar() {
            log("drawAStar");
            g2.setColor(new Color(0xDD6C73));
            drawLine(polygonWorld.getStartPoint(), polygonWorld.getEndPoint());
            for (Link link : polygonWorld.getAllLinks()) {
                g2.setColor(Color.BLUE);
                Point first = link.getPoints().iterator().next();
                Point second = link.getPoints().iterator().next();
                drawLine(first, second);
            }
        }

        private void drawLine(Point first, Point second) {
            log("drawLine: ");
            g2.drawLine(dSizeW(first.x), dSizeW(first.y), dSizeW(second.x), dSizeW(second.y));
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
            for (MyTile xy : getWaypoints()) {
                int x = dSizeW(xy.x) + margin * 2;
                int y = dSizeW(xy.y) + margin * 2;
                g2.setColor(new Color(0xC7A66E));
                g2.fillRect(x, y, rectSize - margin * 4, rectSize - margin * 4);
                g2.setColor(fontColor);
                g2.drawString(i + "", x + 3, y + 15);
                i++;
            }

            g2.setColor(new Color(0xC78FB3));
            for (MyTile tile : getSlowTiles()) {
                g2.fillRect(dSizeW(tile.x) + margin * 3, dSizeW(tile.y) + margin * 3, rectSize - margin * 6, rectSize - margin * 6);
            }

            g2.setColor(new Color(0xDD2CC9));
            for (MyTile tile : getNitroTiles()) {
                g2.fillOval(dSizeW(tile.x) + margin * 3, dSizeW(tile.y) + margin * 3, rectSize - margin * 6, rectSize - margin * 6);
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
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, margin, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y) + rectSize - margin, margin, margin);
                            break;
                        case RIGHT_HEADED_T:
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, rectSize);
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, margin, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y) + rectSize - margin, margin, margin);
                            break;
                        case TOP_HEADED_T:
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, rectSize, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, margin, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y) + rectSize - margin, margin, margin);
                            break;
                        case BOTTOM_HEADED_T:
                            g2.fillRect(dSizeW(x), dSizeW(y), rectSize, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y), margin, margin);
                            g2.fillRect(dSizeW(x), dSizeW(y) + rectSize - margin, margin, margin);
                            g2.fillRect(dSizeW(x) + rectSize - margin, dSizeW(y) + rectSize - margin, margin, margin);
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

    private List<MyTile> getNitroTiles() {
        return Collections.emptyList();
    }

    private List<MyTile> getSlowTiles() {
        return Collections.emptyList();
    }

    private class FPoint extends Unit {
        public FPoint(double x, double y) {
            super(0, 0, x, y, 0, 0, 0, 0);
        }

        public FPoint(int curWaypointInd) {
            this(getWaypoints().get(curWaypointInd).x * game.getTrackTileSize(), getWaypoints().get(curWaypointInd).y * game.getTrackTileSize());
        }

        @Override
        public String toString() {
            return "FPoint{" + getX() + " " + getY() + "}";
        }
    }

    private class MyTile {
        public int x;
        public int y;

        public MyTile(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public MyTile(int[] xy) {
            x = xy[0];
            y = xy[1];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyTile myTile = (MyTile) o;

            return x == myTile.x && y == myTile.y;

        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }
    }


    ///##########################################################################################
    ///####################        ######################################################################
    ///##################     ##     ######################################################################
    ///#################    ####A      AA#####################################################################
    ///###############     ####AAAA      #######################################################################
    ///##############                     ##AAAA##########################################################################
    ///#############     #AAAA#######     #####################################################################
    ///############     AAAA##########       ####################################################################
    ///##########     #################       #############################################################


    public class AStarPathCalcer implements PathCalcer {
        public static final int INITIAL_CAPACITY = 300;
        private PolygonsWorld context;
        private double length;

        @Override
        public void setContext(PolygonsWorld context) {
            this.context = context;
        }

        @Override
        public void calcPath() {
            aStarCalc();
        }

        public AStarPathCalcer() {
            path = new ArrayList<Point>();
        }

        private static final int START_NODE = 1;
        private static final int COMMON_NODE = 0;
        private static final double RANDOM_WAY_RATIO = 0f;
        private static final double MOVE_COST = 1;
        private static final boolean BREAK_TIES = false;
        private static final int MAX_LENGTH_PATH = 999;
        private PriorityQueue<Node> openNodes;
        private ArrayList<Node> closedNodes;
        private Point goalPosition;
        private Node startNode;
        private ArrayList<Point> path;

        private void aStarCalc() {
            goalPosition = context.getEndPoint();
            closedNodes = new ArrayList<Node>();
            openNodes = new PriorityQueue<Node>(INITIAL_CAPACITY, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    if (o1.getF() < o2.getF()) {
                        return -1;
                    } else if (o1.getF() == o2.getF()) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
            });
            startNode = new Node(context.getStartPoint());
            calcF(startNode);
            openNodes.add(startNode);
            while (!openNodes.peek().getPosition().equals(goalPosition)
                    && !(openNodes.peek().getParentsCount() > MAX_LENGTH_PATH)) {
                //  System.out.println("openNodes count: " + openNodes.size());
                Node current = openNodes.peek();
                openNodes.remove(current);
                closedNodes.add(current);
                for (Node neighbor : getNeighbors(current)) {
                    double costG = current.getG() + getEuclideDistance(current.getPosition(), neighbor.getPosition());
                    if (openNodes.contains(neighbor) && costG < neighbor.getG()) {
                        openNodes.remove(neighbor);
                    }
                    if (closedNodes.contains(neighbor) && costG < neighbor.getG()) {
                        closedNodes.remove(neighbor);
                    }
                    if (!openNodes.contains(neighbor) && !closedNodes.contains(neighbor)) {
                        neighbor.setG(costG);
                        neighbor.recalcF();
                        openNodes.add(neighbor);
                        //        sortOpenNodes();
                        neighbor.setParent(current);
                    }
                }
            }
            System.out.println();
            System.out.println("======+=+======");
            System.out.println("done A* " + ":" + openNodes.peek().getParentsCount());
            savePath();
        }

        private void savePath() {
            length = 0;
            path = new ArrayList<Point>();
            Node node = openNodes.peek();
            while (true) {
                path.add(node.getPosition());
                if (node.getParent() != null)
                    length += Utils.getEuclideDistanceSimple(node.getPosition(), node.getParent().getPosition());
                node = node.getParent();
                if (node == null) {
                    break;
                }
            }
            System.out.println(" A* length = " + length);
            Collections.reverse(path);
        }

        private List<Node> getNeighbors(Node node) {
            List<Node> neighbors = new ArrayList<Node>();
            for (Link link : node.getPosition().getLinks()) {
                Point pointFromLink = link.getAnotherPoint(node.getPosition());
                Node newNode = new Node(pointFromLink);
                newNode.setParent(newNode);
                calcF(newNode);
                neighbors.add(newNode);
            }
            return neighbors;
        }


        private Node findNodeByPosition(Point point) {
            for (Node node : openNodes) {
                if (node.getPosition().equals(point)) {
                    return node;
                }
            }
            for (Node node : closedNodes) {
                if (node.getPosition().equals(point)) {
                    return node;
                }
            }
            return null;
        }

        private void calcF(Node node) {
            //  double heuristik = getManhattanDistance(node.getPosition(), goalPosition);
            double heuristik = getEuclideDistance(node.getPosition(), goalPosition);
            double pathCost = (node.getParent() == null ? 0 : node.getParent().getG()
                    + context.getLink(node.getPosition(), node.getParent().getPosition()).getLength());
            double f = heuristik + pathCost;
            node.setF(f);
            node.setG(pathCost);
            node.setH(heuristik);
        }

        private double getEuclideDistance(Point position, Point position1) {
            double distance = Utils.getEuclideDistanceSimple(position, position1);
            if (BREAK_TIES && startNode != null) {
                double dx1 = position.getX() - position1.getX();
                double dy1 = position.getY() - position1.getY();
                double dx2 = startNode.getPosition().getX() - position1.getX();
                double dy2 = startNode.getPosition().getY() - position1.getY();
                double cross = Math.abs(dx1 * dy2 - dx2 * dy1);
                return MOVE_COST * (distance) + cross * 0.001d;
            } else {
                return MOVE_COST * (distance);
            }
        }

        private double getManhattanDistance(Point position, Point position1) {
            double dx = Math.abs(position.getX() - position1.getX());
            double dy = Math.abs(position.getY() - position1.getY());
            if (BREAK_TIES && startNode != null) {
                double dx1 = position.getX() - position1.getX();
                double dy1 = position.getY() - position1.getY();
                double dx2 = startNode.getPosition().getX() - position1.getX();
                double dy2 = startNode.getPosition().getY() - position1.getY();
                double cross = Math.abs(dx1 * dy2 - dx2 * dy1);
                return MOVE_COST * (dx + dy) + cross * 0.001d;
            } else {
                return MOVE_COST * (dx + dy);
            }
        }

        public Point getGoalPosition() {
            return goalPosition;
        }

        public PriorityQueue<Node> getOpenNodes() {
            return openNodes;
        }

        public ArrayList<Node> getClosedNodes() {
            return closedNodes;
        }

        public ArrayList<Point> getPath() {
            return path;
        }

        public class Node {
            private double f;
            private double g;
            private double h;
            private Node parent;

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Node node = (Node) o;

                if (!position.equals(node.position)) return false;

                return true;
            }

            @Override
            public int hashCode() {
                return position.hashCode();
            }

            public Point getPosition() {
                return position;
            }

            private final Point position;

            public Node(Point position) {
                this.position = position;
            }

            public double getF() {
                return f;
            }

            public void setF(double f) {
                this.f = f;
            }

            public void setG(double g) {
                this.g = g;
            }

            public double getG() {
                return g;
            }

            public void setH(double h) {
                this.h = h;
            }

            public double getH() {
                return h;
            }

            public Node getParent() {
                return parent;
            }

            public void setParent(Node parent) {
                this.parent = parent;
            }

            @Override
            public String toString() {
                return "f: " + getF() + "; g: " + getG() + "; h: " + getH() + "; pos: " + getPosition();
            }

            public void recalcF() {
                setF(getG() + getH());
            }

            public String printParents() {
                if (getParent() != null) {
                    return this + " > " + getParent().printParents();
                } else {
                    return this.toString();
                }
            }

            public int getParentsCount() {
                int count = 0;
                Node node = this;
                while (true) {
                    node = node.getParent();
                    if (node == null) {
                        return count;
                    }
                    count++;
                }
            }
        }
    }

    public static class Link {
        private final double length;
        private HashSet<Point> points;

        public Link(Point point1, Point point2) {
            this.points = new HashSet<Point>();
            points.add(point1);
            points.add(point2);
            length = Utils.getEuclideDistanceSimple(point1, point2);
        }

        public boolean contain(Point point, Point point2) {
            return points.contains(point) && points.contains(point2);
        }

        public Set<Point> getPoints() {
            return points;
        }

        public Point getAnotherPoint(Point pointParam) {
            Iterator<Point> iter = points.iterator();
            Point point1 = iter.next();
            if (point1 != pointParam) {
                return point1;
            } else {
                return iter.next();
            }
        }

        public double getLength() {
            return length;
        }

        @Override
        public String toString() {
            return "Link{" +
                    "length=" + length +
                    ", points=" + points +
                    '}';
        }
    }

    public static class Utils {
        public static double getEuclideDistanceSimple(Point position, Point position1) {
            return Math.sqrt(Math.pow(position.getX() - position1.getX(), 2) + Math.pow(position.getY() - position1.getY(), 2));
        }

        public static Point getPointByAzimuthDistance(Point point, double azimuth, double distance) {
            return new Point((int) (point.getX() + distance * Math.cos(azimuth)),
                    (int) (point.getY() + distance * Math.sin(azimuth)), point.getContext());
        }
    }

    public static class Obstacle {
        private static final float RADIUS_POINT = 5;
        public static final Color NOT_CURRENT_COLOR = Color.GRAY;
        public static final Color CURRENT_OBSTACLE_COLOR = Color.GREEN;
        private static final int OFFSET = 0;
        public static final Color COLOR_FILL_POLYGON = Color.PINK;
        private final List<Point> points;
        private PolygonsWorld context;


        public Obstacle(PolygonsWorld context) {
            points = new ArrayList<Point>();
            setContext(context);
        }

        public void addPoint(Point point) {
            points.add(point);
            point.setObstacle(this);
            calcPoly();
        }

        private void calcPoly() {
            float a = 100;
            float b = 100;
            float[] polygonPoints = new float[points.size() * 2];

            int i = 0;
            for (Point point : points) {
                polygonPoints[i] = point.getX();
                i++;
                polygonPoints[i] = point.getY();
                i++;
            }

            int indexPoint = 2;
            if (points.size() < 3) {
                return;
            }
            short[] triangles = new short[(points.size() - indexPoint) * 2 * 3];
            int indexValueInTriangle = 0;
            while (indexPoint <= points.size() - 1 && points.size() > 2) {
                triangles[indexValueInTriangle] = (short) polygonPoints[(indexPoint - 2) * 2];
                indexValueInTriangle++;
                triangles[indexValueInTriangle] = (short) polygonPoints[(indexPoint - 2) * 2 + 1];
                indexValueInTriangle++;

                triangles[indexValueInTriangle] = (short) polygonPoints[(indexPoint - 1) * 2];
                indexValueInTriangle++;
                triangles[indexValueInTriangle] = (short) polygonPoints[(indexPoint - 1) * 2 + 1];
                indexValueInTriangle++;
                triangles[indexValueInTriangle] = (short) polygonPoints[(indexPoint - 0) * 2];
                indexValueInTriangle++;
                triangles[indexValueInTriangle] = (short) polygonPoints[(indexPoint - 0) * 2 + 1];
                indexValueInTriangle++;

                indexPoint++;
            }
        }

        public void removeLastPoint() {
            if (points.size() > 0) {
                Point point = points.get(points.size() - 1);
                points.remove(point);
                point.setObstacle(null);

            }
        }

        public void setContext(PolygonsWorld context) {
            this.context = context;
        }

        public PolygonsWorld getContext() {
            return context;
        }

        public List<Point> getPoints() {
            return points;
        }
    }

    public static class Point {
        private static final double DISTANCE = 3000;
        private final int id;
        private int x;
        private int y;
        private static int maxId;

        static {
            maxId = 0;
        }

        private HashSet<Link> links;
        private PolygonsWorld context;
        private Obstacle obstacle;

        public Point(int x, int y, PolygonsWorld context) {
            setX(x);
            setY(y);
            id = maxId;
            maxId++;
            setContext(context);
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return y;
        }

        public Point getCopy() {
            return new Point(getX(), getY(), context);
        }

        public void calcLinks() {
            links = new HashSet<Link>();
            if (this == context.getStartPoint()) {
                addLinkToIfCan(context.getEndPoint());
            } else if (this == context.getEndPoint()) {
                addLinkToIfCan(context.getStartPoint());
            } else {
                addLinkToIfCan(context.getEndPoint());
                addLinkToIfCan(context.getStartPoint());
            }
            for (Obstacle obstacle : getContext().getObstacles()) {
                for (Point point : obstacle.getPoints()) {
                    if (point != this) {
                        addLinkToIfCan(point);
                    }
                }
            }

        }

        private void addLinkToIfCan(Point pointGoal) {
            Obstacle shareObstacle = null;
            if (getObstacle() != null && pointGoal.getObstacle() != null && getObstacle() == pointGoal.getObstacle()) {
                shareObstacle = getObstacle();
                List<Point> points = getObstacle().getPoints();
                int indexThis = points.indexOf(this);
                int indexGoal = points.indexOf(pointGoal);
                int deltaIndex = Math.abs(indexThis - indexGoal);
                if ((deltaIndex == 1)
                        || deltaIndex == points.size() - 1) {
                    createLink(pointGoal);
                    return;
                }

            }

            boolean haveIntersect = false;
            for (Obstacle obstacle : context.getObstacles()) {
                Point prevPoint = null;
                for (Point pointObstacle : obstacle.getPoints()) {
                    if (prevPoint == null) {
                        if (obstacle.getPoints().size() > 2) {
                            prevPoint = obstacle.getPoints().get(obstacle.getPoints().size() - 1);
                        }
                    }
                    if (prevPoint != null && !(this == prevPoint || this == pointObstacle || pointGoal == prevPoint || pointGoal == pointObstacle)
                            && checkIntersect(this, pointGoal, prevPoint, pointObstacle)) {
                        haveIntersect = true;
                        break;
                    }
                    if (obstacle == shareObstacle) {
                        //  System.out.println("check point from obstacle");
                        haveIntersect = checkLineInObstacle(this, pointGoal, obstacle);
                        if (haveIntersect) {
                            break;
                        }

                    }
                    prevPoint = pointObstacle;
                }
                if (haveIntersect) {
                    break;
                }
            }
            if (haveIntersect) {
                return;
            }

            createLink(pointGoal);
        }

        private boolean checkLineInObstacle(Point point, Point pointGoal, Obstacle obstacle) {
            int x = (point.getX() + pointGoal.getX()) / 2;
            int y = (point.getY() + pointGoal.getY()) / 2;
            Point pointOnLine = new Point(x, y, getContext());
            int n = 10;
            int count = 0;
            for (int i = 0; i < n; i++) {
                double azimuth = Math.random() * 360;
                count += (checkPointInObstacle(obstacle, pointOnLine, azimuth) ? 1 : 0);
            }
            if (count > n / 2) {
                return true;
            } else {
                return false;
            }
        }

        private boolean checkPointInObstacle(Obstacle obstacle, Point pointOnLine, double azimuth) {
            Point pointOnLineVector = Utils.getPointByAzimuthDistance(pointOnLine, azimuth, DISTANCE);
            Point prevPoint = null;
            int countIntersect = 0;
            for (Point pointObstacle : obstacle.getPoints()) {
                if (prevPoint == null) {
                    if (obstacle.getPoints().size() > 2) {
                        prevPoint = obstacle.getPoints().get(obstacle.getPoints().size() - 1);
                    }
                }
                if (prevPoint != null && !(pointOnLine == prevPoint || pointOnLine == pointObstacle ||
                        pointOnLineVector == prevPoint || pointOnLineVector == pointObstacle)
                        && checkIntersect(pointOnLine, pointOnLineVector, prevPoint, pointObstacle)) {
                    countIntersect++;
                }
                prevPoint = pointObstacle;
            }
            if (countIntersect % 2 == 0) {
                return false;
            } else {
                return true;
            }
        }

        private void createLink(Point pointGoal) {
            Link link = context.getLink(this, pointGoal);
            if (link == null) {
                link = new Link(this, pointGoal);
                context.getAllLinks().add(link);
            }
            links.add(link);
        }

        private boolean checkIntersect(Point a1, Point a2, Point b1, Point b2) {
            return segmentsIntersect(a1.getX(), a1.getY(), a2.getX(), a2.getY(), b1.getX(), b1.getY(), b2.getX(), b2.getY());
        }

        public static boolean segmentsIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
            double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
            if (d == 0) return false;
            double xi = Math.floor(((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d);
            double yi = Math.floor(((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d);
            if (xi < Math.min(x1, x2) || xi > Math.max(x1, x2)) return false;
            if (xi < Math.min(x3, x4) || xi > Math.max(x3, x4)) return false;
            if (yi < Math.min(y1, y2) || yi > Math.max(y1, y2)) return false;
            if (yi < Math.min(y3, y4) || yi > Math.max(y3, y4)) return false;
        /*Косяки с вычислениями в double*/
       /* log("[segmentsIntersect] OBSTACLE FIND!");
        log("[segmentsIntersect] x1 y1 x2 y2 x3 x4 : " + x1 + " " + y1 + " " + x2 + " " + y2 + " " + x3 + " " + y3 + " " + x4 + " " + y4);

        log("[segmentsIntersect]xi, yi : " + xi + " " + yi);
        */
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Point point = (Point) o;

            if (id != point.id) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id;
        }

        public void setContext(PolygonsWorld context) {
            this.context = context;
        }

        public PolygonsWorld getContext() {
            return context;
        }

        @Override
        public String toString() {
            return " p - id: " + id + "; x: " + x + "; y: " + y;
        }

        public void setObstacle(Obstacle obstacle) {
            this.obstacle = obstacle;
        }

        public Obstacle getObstacle() {
            return obstacle;
        }

        public HashSet<Link> getLinks() {
            return links;
        }
    }

    public class PolygonsWorld {
        private PathCalcer pathCalcer;
        private int height;
        private int width;
        private List<Obstacle> obstacles;
        private Obstacle currentObstacle;
        private Point startPoint;
        private Point endPoint;
        private HashSet<Link> allLinks;

        public PolygonsWorld(int width, int height) {
            setWidth(width);
            setHeight(height);

            obstacles = new ArrayList<Obstacle>();
            allLinks = new HashSet<Link>();
            pathCalcer = new AStarPathCalcer();
            pathCalcer.setContext(this);
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public List<Obstacle> getObstacles() {
            return obstacles;
        }

        public void setObstacles(List<Obstacle> obstacles) {
            this.obstacles = obstacles;
        }

        public void addNewPointToCurrentObstacle(int x, int y) {
            Obstacle currentObstacle = getCurrentObstacle();
            if (currentObstacle == null && obstacles.size() > 0) {
                currentObstacle = obstacles.get(0);
            } else if (currentObstacle == null) {
                currentObstacle = new Obstacle(this);
                obstacles.add(currentObstacle);
                setCurrentObstacle(currentObstacle);
            }
            currentObstacle.addPoint(new Point(x, y, this));
        }

        public Obstacle getCurrentObstacle() {
            return currentObstacle;
        }

        public void setCurrentObstacle(Obstacle currentObstacle) {
            this.currentObstacle = currentObstacle;
        }

        public void addNewStartPoint(int x, int y) {
            setStartPoint(new Point(x, y, this));
        }

        public void setStartPoint(Point startPoint) {
            this.startPoint = startPoint;
        }

        public Point getStartPoint() {
            return startPoint;
        }

        public void addNewEndPoint(int x, int y) {
            setEndPoint(new Point(x, y, this));
        }

        public void setEndPoint(Point endPoint) {
            this.endPoint = endPoint;
        }

        public Point getEndPoint() {
            return endPoint;
        }

        public void removeLastPointFromCurrentObstacle() {
            if (currentObstacle != null)
                currentObstacle.removeLastPoint();
        }

        public void previusCurrentObstacle() {

            if (currentObstacle == null) {

            } else {
                int index = obstacles.indexOf(currentObstacle);
                if (index > 0) {
                    index--;
                    currentObstacle = obstacles.get(index);
                }
            }
        }

        public void nextCurrentObstacle() {
            if (currentObstacle == null) {
                return;
            }
            int index = obstacles.indexOf(currentObstacle);
            if (index > obstacles.size() - 2) {
                Obstacle newObstacle = new Obstacle(this);
                obstacles.add(newObstacle);
                currentObstacle = newObstacle;
            } else {
                index++;
                currentObstacle = obstacles.get(index);
            }
        }

        public void calcViewGraph() {
            if (startPoint == null || endPoint == null) {
                return;
            }
            setAllLinks(new HashSet<Link>());

            startPoint.calcLinks();
            endPoint.calcLinks();
            for (Obstacle obstacle : obstacles) {
                for (Point point : obstacle.getPoints()) {
                    point.calcLinks();
                }
            }
            System.out.println("done calc view graph " + " links in graph: " + getAllLinks().size());
            calcPath();
        }

        private void calcPath() {
            pathCalcer.calcPath();
        }

        public void setAllLinks(HashSet<Link> allLinks) {
            this.allLinks = allLinks;
        }

        public HashSet<Link> getAllLinks() {
            return allLinks;
        }

        public Link getLink(Point point, Point point2) {
            for (Link link : allLinks) {
                if (link.contain(point, point2)) {
                    return link;
                }
            }
            return null;
        }

        public void cleanLinks() {
            setAllLinks(new HashSet<Link>());

        }

        public PathCalcer getPathCalcer() {
            return pathCalcer;
        }

        public void cleanPathCalcer() {
            pathCalcer = new AStarPathCalcer();
            pathCalcer.setContext(this);
        }
    }

    public interface PathCalcer {
        void setContext(PolygonsWorld polygonsWorld);

        void calcPath();
    }
}
