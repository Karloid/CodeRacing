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
    public static final int MAX_SPEED = 99;
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
    private List<Point2D> path;

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
            polygonWorld = new PolygonsWorld(this, (int) (world.getWidth() * game.getTrackTileSize()), (int) (world.getHeight() * game.getTrackTileSize()));
        }
        polygonWorld.setup(self, world, game, move);
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

    private boolean isMap(String mapName) {
        return world.getMapName().equals(mapName);
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
            if (self.getOilCanisterCount() > 0 && notLast()) {
                move.setSpillOil(true);
                log("use oil!!1");
            }
        } else if (isTimeForNitro()) {
            move.setUseNitro(true);
            log("!!! use nitro!");
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
        boolean isNitroTime = self.getEnginePower() > 1.4;
        float maxSpeedOnCorner = 17 * carefulCof * (isNitroTime ? 1.5f : 1f);
        boolean tooFastCorner = (speedModule > maxSpeedOnCorner) && (
                ((path.size() > 3) && (Math.abs(self.getAngleTo(path.get(2).x, path.get(2).y) + self.getAngleTo(path.get(1).x, path.get(1).y)) > 0.35f)));

        boolean slowTile = speedModule > maxSpeedOnCorner && isSlowTile();
        if (slowTile) log("is slowTile!");


        return (angleStuff || tooFast || tooFastCorner || slowTile) && (getMoveBackWardDelta() > TICKS_COUNT_FOR_DISTANCE);
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
        return path.size() > 6 && isCorrectAngle(path.get(2)) && isCorrectAngle(path.get(3)) && isCorrectAngle(path.get(4)) && isCorrectAngle(path.get(4)) && game.getInitialFreezeDurationTicks() < world.getTick() && self.getNitroChargeCount() > 0;
    }

    private boolean isCorrectAngle(Point2D point) {
        return abs(self.getAngleTo(point.x, point.y)) < 0.15f;
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

        path = polygonWorld.getResultPath();
        if (path == null || path.size() < 3) return;
        Point2D nextPoint = path.get(1);
        log("path length: " + path.size());


        for (int i = 2 + 0; i < 3 + 1; i++) {
            Point2D point2 = path.get(i - 2);
            Point2D point1 = path.get(i - 1);
            Point2D point0 = path.get(i);
            int deltaX = (int) (point1.x - point0.x);
            int deltaY = (int) (point1.y - point0.y);
            boolean isAngle = false;
            if ((point2.x - point1.x != 0 || deltaX != 0) && (point2.y - point1.y != 0 || deltaY != 0)) {
                log("found angle at x: " + point1.x / game.getTrackTileSize() + " y: " + point1.y / game.getTrackTileSize());
                isAngle = true;
            }

            int newX = (int) ((point1.x + point0.x + point2.x) / 3);
            int newY = (int) ((point1.y + point0.y + point2.y) / 3);
            point1.x = (point1.x + newX) / 2;
            point1.y = (point1.y + newY) / 2;

            point1.x = (point1.x + newX) / 2;
            point1.y = (point1.y + newY) / 2;

            int tmpX = (int) ((point0.x + point2.x) / 2);
            int tmpY = (int) ((point0.y + point2.y) / 2);

            deltaX = (int) (point1.x - tmpX);
            deltaY = (int) (point1.y - tmpY);

            if (isAngle) {
                point2.x += deltaX * 0.7f;
                point2.y += deltaY * 0.7f;
            }

            //   point2.x += deltaX;
            //   point2.y += deltaY;

        }
        nextX = nextPoint.x;
        nextY = nextPoint.y;
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
    //====================================================             =======================================
    //=============================================           ==          ============================================
    //===========================================        ============              ====================================
    //=======================================         ===============           =====================================
    //======================================            =====================================================
    //===========================================          =====================            ===========================
    //=================================================         ==========================================
    //===========================================================================================
    //============================================================            ===============================
    //============================================================================         ===============
    //=============================================================================      ==============
    //=====================================================================     ======================
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

            panel = new MyPanel();
            frame.setContentPane(panel);
            frame.setVisible(true);
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
            g2.setColor(new Color(0xDD6C73));
            drawLine(polygonWorld.getStartPoint(), polygonWorld.getEndPoint());
            for (Link link : polygonWorld.getAllLinks()) {
                g2.setColor(new Color(0x7F75DD));
                Iterator<Point2D> iterator = link.getPoints().iterator();
                Point2D first = iterator.next();
                Point2D second = iterator.next();
                drawLine(first, second);
            }

            int i = 0;
            Point2D prevPoint = null;
            for (Point2D point : polygonWorld.getResultPath()) {
                g2.setColor(i == 0 ? Color.red : i == 1 ? Color.cyan : Color.blue);
                g2.fillOval(dSize(point.x) - 5, dSize(point.y) - 5, 10, 10);
                i++;
                if (prevPoint != null) {
                    drawLine(prevPoint, point);
                }
                prevPoint = point;
            }
        }

        private void drawLine(Point2D first, Point2D second) {
            g2.drawLine(dSize(first.x), dSize(first.y), dSize(second.x), dSize(second.y));
        }

        private void drawTexts() {
            int stringPadding = sidePadding / 2;
            g2.drawString("speed: " + f(speedModule), stringPadding, sidePadding / 2);
            g2.drawString("max_speed: " + f(MAX_SPEED), stringPadding * 5, sidePadding / 2);
            g2.drawString("enigePower: " + f(self.getEnginePower()), stringPadding * 10, sidePadding / 2);
            g2.drawString("BRAKE: " + move.isBrake(), stringPadding * 15, sidePadding / 2);
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
            if (tilesPoints == null) {
                return;
            }
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

                boolean isMyCar = car.getPlayerId() == world.getMyPlayer().getId();
                gg.setColor(isMyCar ? Color.green : Color.red);
                if (isMyCar && move.isBrake()) {
                    gg.setColor(new Color(0xFF54BF));
                }
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

        public FPoint(Point2D point) {
            super(0, 0, point.x, point.y, 0, 0, 0, 0);
        }

        public FPoint(Point2D point, double angleToPoint_1) {
            super(0, 0, point.x, point.y, 0, 0, angleToPoint_1, 0);
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


}
