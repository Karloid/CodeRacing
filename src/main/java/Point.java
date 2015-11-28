import model.TileType;

import java.util.HashSet;
import java.util.List;

public class Point {
    private static final double DISTANCE = 3000;
    public final int id;
    public int x;
    public int y;
    public static int maxId;

    static {
        maxId = 0;
    }

    private HashSet<Link> links;
    private PolygonsWorld context;
    private Obstacle obstacle;
    private int tileType;

    public Point(int x, int y, PolygonsWorld context) {
        setX(x);
        setY(y);
        id = maxId;
        maxId++;
        setContext(context);
    }

    public Point(double x, double y, PolygonsWorld context) {
        this((int) x, (int) y, context);
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
        links = new HashSet<>();
        checkAllPoints();
     //   checkWaypoints();
    }

    private void checkWaypoints() {
      /*  for (Point point : context.getWaypoints()) {
            addLinkToIfCan2(point);
        }*/
    }

    private void checkAllPoints() {
        for (List<Point> points : context.getAllPoints().values()) {
            for (Point point : points) {
                addLinkToIfCan2(point);
            }
        }
    }

    private void addLinkToIfCan2(Point point) {
        if (point != this && context.inSameTile(this, point)) {
            createLink(point);
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

    public void createLink(Point pointGoal) {
        if (pointGoal == null) return;
        if (links == null) {
            links = new HashSet<Link>();
        }
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
        if (links == null)
            links = new HashSet<>();
        return links;
    }

    public int getTileType() {
        return tileType;
    }
}
