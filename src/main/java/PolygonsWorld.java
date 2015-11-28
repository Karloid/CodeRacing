import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
