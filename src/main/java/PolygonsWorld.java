import model.*;

import java.util.*;

public class PolygonsWorld {
    private MyStrategy myStrategy;
    private PathCalcer pathCalcer;
    private int height;
    private int width;
    private List<Obstacle> obstacles;
    private Obstacle currentObstacle;
    private Point startPoint;
    private Point endPoint;
    private HashSet<Link> allLinks;
    private Car self;
    private World world;
    private Game game;
    private Move move;
    private Map<LightPoint, List<Point>> allPoints;
    private Point userPoint;

    public List<LightPoint> getWaypoints() {
        return waypoints;
    }

    private List<LightPoint> waypoints;
    private List<Point> resultPath;

    public PolygonsWorld(MyStrategy myStrategy, int width, int height) {
        this.myStrategy = myStrategy;
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

    public List<Point> getResultPath() {
        return resultPath;
    }

    public void calcViewGraph() {

        userPoint = new Point(self.getX(), self.getY(), this);
        startPoint = userPoint;

        waypoints = new ArrayList<>();

        for (int[] ints : world.getWaypoints()) {
            LightPoint newPoint = new LightPoint(ints[0], ints[1]);
            waypoints.add(newPoint);

        }
        addAllPoints();

        setAllLinks(new HashSet<Link>());

        calcLinksForPoints();

    /*    for (Point waypoint : waypoints) {
            waypoint.calcLinks();
        }*/

        int x = (int) (startPoint.x / game.getTrackTileSize());
        int y = (int) (startPoint.y / game.getTrackTileSize());
        LightPoint key = new LightPoint(x, y);
        key.setTileType(world.getTilesXY()[x][y]);
        addLinks(key, startPoint);

        int nextWaypointIndex = self.getNextWaypointIndex();
        resultPath = new ArrayList<>();
        for (int i = 0; i < waypoints.size(); i++) {
            if (i == 0) {
                startPoint = userPoint;
            } else {
                startPoint = allPoints.get(waypoints.get(appendIndex(nextWaypointIndex, -1))).get(0);
            }
            endPoint = allPoints.get(waypoints.get(nextWaypointIndex)).get(0);

            boolean result = pathCalcer.calcPath();
            if (!result) {
                System.out.println("astar failed :|");
                break;
            }

            appendToResult(getPathCalcer().getPath());

            nextWaypointIndex = appendIndex(nextWaypointIndex, 1);
        }
    }

    private void appendToResult(ArrayList<Point> path) {
        for (Point newPoint : path) {
            if (resultPath.size() == 0 || resultPath.get(resultPath.size() - 1) != newPoint)
                resultPath.add(newPoint);
        }
    }

    private int appendIndex(int nextWaypointIndex, int i) {
        int result = nextWaypointIndex + i;
        if (result >= world.getWaypoints().length) {
            return 0;
        } else if (result < 0) {
            return world.getWaypoints().length - 1;
        }
        return result;
    }

    private void calcLinksForPoints() {
        for (Map.Entry<LightPoint, List<Point>> entry : allPoints.entrySet()) {
            LightPoint key = entry.getKey();
            List<Point> points = entry.getValue();
            for (Point point : points) {
                addLinks(key, point);
            }
        }
    }

    private void addLinks(LightPoint key, Point point) {
        point.calcLinks();
        switch (key.getTileType()) {
            case EMPTY:
                break;
            case VERTICAL:
                addTop(key, point);
                addBottom(key, point);
                break;
            case HORIZONTAL:
                addLeft(key, point);
                addRight(key, point);
                break;
            case LEFT_TOP_CORNER:
                addRight(key, point);
                addBottom(key, point);
                break;
            case RIGHT_TOP_CORNER:
                addLeft(key, point);
                addBottom(key, point);
                break;
            case LEFT_BOTTOM_CORNER:
                addRight(key, point);
                addTop(key, point);
                break;
            case RIGHT_BOTTOM_CORNER:
                addLeft(key, point);
                addTop(key, point);
                break;
            case RIGHT_HEADED_T:
                addRight(key, point);
                addTop(key, point);
                addBottom(key, point);
                break;
            case LEFT_HEADED_T:
                addLeft(key, point);
                addTop(key, point);
                addBottom(key, point);
                break;
            case TOP_HEADED_T:
                addLeft(key, point);
                addRight(key, point);
                addTop(key, point);
                break;
            case BOTTOM_HEADED_T:
                addLeft(key, point);
                addRight(key, point);
                addBottom(key, point);

                break;
            case CROSSROADS:
            case UNKNOWN:
                addLeft(key, point);
                addRight(key, point);
                addTop(key, point);
                addBottom(key, point);
                break;
        }
    }

    private void addTop(LightPoint key, Point value) {
        List<Point> points = allPoints.get(new LightPoint(key.x, key.y - 1));
        if (points != null) {
            for (Point point : points) {
                value.createLink(point);
            }
        }
    }

    private void addBottom(LightPoint key, Point value) {
        List<Point> points = allPoints.get(new LightPoint(key.x, key.y + 1));
        if (points != null) {
            for (Point point : points) {
                value.createLink(point);
            }
        }
    }

    private void addLeft(LightPoint key, Point value) {
        List<Point> points = allPoints.get(new LightPoint(key.x - 1, key.y));
        if (points != null) {
            for (Point point : points) {
                value.createLink(point);
            }
        }
    }

    private void addRight(LightPoint key, Point value) {
        List<Point> points = allPoints.get(new LightPoint(key.x + 1, key.y));
        if (points != null) {
            for (Point point : points) {
                value.createLink(point);
            }
        }
    }

    private double getVFromTile(int value) {
        return value * game.getTrackTileSize() + game.getTrackTileSize() / 2;
    }


    private void addAllPoints() {

        double pointTileOffset = game.getTrackTileMargin() + game.getCarWidth() / 2;

        allPoints = new HashMap<>();
        int x = 0;
        for (TileType[] tileTypes : world.getTilesXY()) {
            int y = 0;
            for (TileType tileType : tileTypes) {
                if (!tileType.equals(TileType.EMPTY)) {
                    int topX = (int) (x * game.getTrackTileSize());
                    int topY = (int) (y * game.getTrackTileSize());

                    int botX = (int) (topX + game.getTrackTileSize() - pointTileOffset);
                    int botY = (int) (topY + game.getTrackTileSize() - pointTileOffset);

                    topX += pointTileOffset;
                    topY += pointTileOffset;

                    int mediumX = topX + (botX - topX) / 2;
                    int mediumY = topY + (botY - topY) / 2;

                    List<Point> points = new ArrayList<>(9);
                    //   points.add(new Point(topX, topY, this));
                    //   points.add(new Point(botX, topY, this));
                    //  points.add(new Point(botX, botY, this));
                    //   points.add(new Point(topX, botY, this));
                    points.add(new Point(mediumX, mediumY, this));
                    //   points.add(new Point(botX, mediumY, this));
                    //   points.add(new Point(topX, mediumY, this));
                    //    points.add(new Point(mediumX, botY, this));
                    //    points.add(new Point(mediumX, topY, this));

                    LightPoint key = new LightPoint(x, y);
                    key.setTileType(tileType);
                    allPoints.put(key, points);
                }
                y++;
            }
            x++;
        }
    }

    private int dSize(int v) {
        return (int) (v * game.getTrackTileSize());
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

    public void setup(Car self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
    }

    public MyStrategy getMyStrategy() {
        return myStrategy;
    }

    public Car getSelf() {
        return self;
    }

    public World getWorld() {
        return world;
    }

    public Game getGame() {
        return game;
    }

    public Move getMove() {
        return move;
    }

    public boolean inSameTile(Point point, Point point1) {
        int trackTileSize = (int) game.getTrackTileSize();
        return point.x / trackTileSize == point1.x / trackTileSize && point.y / trackTileSize == point1.y / trackTileSize;
    }

    public Map<LightPoint, List<Point>> getAllPoints() {
        return allPoints;
    }

    public Point getUserPosition() {
        return userPoint;
    }
}
