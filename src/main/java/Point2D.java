import model.Unit;

import java.util.HashSet;
import java.util.List;

public class Point2D {
    private static final double DISTANCE = 3000;
    private double val;
    public int id;
    public double x;
    public double y;
    public static int maxId;

    static {
        maxId = 0;
    }

    private HashSet<Link> links;
    private PolygonsWorld context;
    private Obstacle obstacle;
    private int tileType;

    public Point2D(int x, int y, PolygonsWorld context) {
        setX(x);
        setY(y);
        id = maxId;
        maxId++;
        setContext(context);
    }

    public Point2D(double x, double y, PolygonsWorld context) {
        this((int) x, (int) y, context);
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Point2D getCopy() {
        return new Point2D(getX(), getY(), context);
    }

    public void calcLinks() {
        links = new HashSet<>();
        checkAllPoints();
        //   checkWaypoints();
    }

    private void checkWaypoints() {
      /*  for (Point2D point : context.getWaypoints()) {
            addLinkToIfCan2(point);
        }*/
    }

    private void checkAllPoints() {
        for (List<Point2D> points : context.getAllPoints().values()) {
            for (Point2D point : points) {
                addLinkToIfCan2(point);
            }
        }
    }

    private void addLinkToIfCan2(Point2D point) {
        if (point != this && context.inSameTile(this, point)) {
            createLink(point);
        }
    }

    private void addLinkToIfCan(Point2D pointGoal) {
        Obstacle shareObstacle = null;
        if (getObstacle() != null && pointGoal.getObstacle() != null && getObstacle() == pointGoal.getObstacle()) {
            shareObstacle = getObstacle();
            List<Point2D> points = getObstacle().getPoints();
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
            Point2D prevPoint = null;
            for (Point2D pointObstacle : obstacle.getPoints()) {
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

    private boolean checkLineInObstacle(Point2D point, Point2D pointGoal, Obstacle obstacle) {
        int x = (int) ((point.getX() + pointGoal.getX()) / 2);
        int y = (int) ((point.getY() + pointGoal.getY()) / 2);
        Point2D pointOnLine = new Point2D(x, y, getContext());
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

    private boolean checkPointInObstacle(Obstacle obstacle, Point2D pointOnLine, double azimuth) {
        Point2D pointOnLineVector = Utils.getPointByAzimuthDistance(pointOnLine, azimuth, DISTANCE);
        Point2D prevPoint = null;
        int countIntersect = 0;
        for (Point2D pointObstacle : obstacle.getPoints()) {
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

    public void createLink(Point2D pointGoal) {
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

    private boolean checkIntersect(Point2D a1, Point2D a2, Point2D b1, Point2D b2) {
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

        Point2D point = (Point2D) o;

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

    public static double angle(double x, double y) {
        return FastMath.atan2((float) y, (float) x);
    }

    public static double squareDistance(Unit value, Point2D point2D) {
        return squareDistance(value.x, value.y, point2D.x, point2D.y);
    }

    Point2D(double x, double y) {
        this.x = x;
        this.y = y;
        val = 0;
    }

    public double getX() {
        return x;
    }

    public int getIntX() {
        return (int) x;
    }

    public int getIntY() {
        return (int) y;
    }


    public double getY() {
        return y;
    }

    public double getDistanceTo(double x, double y) {
        return getDistance(this.x, this.y, x, y);
    }

    public static double getDistance(Unit unit1, Unit unit2) {
        return getDistance(unit1.getX(), unit1.getY(), unit2.getX(), unit2.getY());
    }

    public static double getDistance(double x1, double y1, double x2, double y2) {
        return FastMath.hypot(x1 - x2, y1 - y2);
    }

    /*   public static double getDistance(double x1, double y1, double x2, double y2) {
           double dx = x1 - x2;
           double dy = y1 - y2;
           return FastMath.hypot(dx, dy);
       }
   */
    public double getDistanceTo(Point2D point) {
        return getDistanceTo(point.x, point.y);
    }

    public double getDistanceTo(Unit unit) {
        return getDistanceTo(unit.x, unit.y);
    }


    public static Point2D vector(double fromX, double fromY, double toX, double toY) {
        return new Point2D(toX - fromX, toY - fromY);
    }


    public Point2D add(double x, double y) {
        return new Point2D(this.x + x, this.y + y);
    }

    public Point2D() {
        x = 0;
        y = 0;
    }

    public Point2D(Point2D v) {
        this.x = v.x;
        this.y = v.y;
        this.val = v.val;
    }

    public Point2D(double angle) {
        this.x = Math.cos(angle);
        this.y = Math.sin(angle);
    }

    public Point2D copy() {
        return new Point2D(this);
    }

    public Point2D sub(Point2D v) {
        return new Point2D(x - v.x, y - v.y);
    }

    public Point2D sub(double dx, double dy) {
        return new Point2D(x - dx, y - dy);
    }

    public Point2D mul(double f) {
        return new Point2D(x * f, y * f);
    }

    public double length() {
//        return hypot(x, y);
        return FastMath.hypot(x, y);
    }

    public double getDistance(Point2D v) {

//        return hypot(x - v.x, y - v.y);
        return FastMath.hypot(x - v.x, y - v.y);
    }

    public double squareDistance(Point2D v) {
        double tx = x - v.x;
        double ty = y - v.y;
        return tx * tx + ty * ty;
    }

    public double squareDistance(double x, double y) {
        double tx = this.x - x;
        double ty = this.y - y;
        return tx * tx + ty * ty;
    }

    public static double squareDistance(double x, double y, double x2, double y2) {
        double tx = x2 - x;
        double ty = y2 - y;
        return tx * tx + ty * ty;
    }

    public static double squareDistance(Unit u1, Unit u2) {
        return squareDistance(u1.x, u1.y, u2.x, u2.y);
    }

    public double squareLength() {
        return x * x + y * y;
    }

    public Point2D reverse() {
        return new Point2D(-x, -y);
    }

    public Point2D normalize() {
        double length = this.length();
        if (length == 0.0D) {
            return new Point2D(0, 0);
        } else {
            return new Point2D(x / length, y / length);
        }
    }

    public Point2D length(double length) {
        double currentLength = this.length();
        if (currentLength == 0.0D) {
            return this;
        } else {
            return this.mul(length / currentLength);
        }
    }

    public Point2D leftPerpendicular() {
        return new Point2D(y, -x);
    }

    public Point2D rightPerpendicular() {
        return new Point2D(-y, x);
    }

    public double dotProduct(Point2D vector) {
        return x * vector.x + y * vector.y;
    }

    public float angle() {
        //return Math.atan2(y, x);
        return FastMath.atan2((float) y, (float) x);
    }

    public boolean nearlyEqual(Point2D potentialIntersectionPoint, double epsilon) {
        return Math.abs(x - potentialIntersectionPoint.x) < epsilon && Math.abs(y - potentialIntersectionPoint.y) < epsilon;
    }

    public Point2D rotate(Point2D angle) {
        double newX = angle.x * x - angle.y * y;
        double newY = angle.y * x + angle.x * y;
        return new Point2D(newX, newY);
    }

    public Point2D rotateBack(Point2D angle) {
        double newX = angle.x * x + angle.y * y;
        double newY = angle.x * y - angle.y * x;
        return new Point2D(newX, newY);
    }

    public Point2D div(double f) {
        return new Point2D(x / f, y / f);
    }

    public Point2D add(Point2D point) {
        return add(point.x, point.y);
    }

    public float getFX() {
        return (float) x;
    }

    public float getFY() {
        return (float) y;
    }

    public Point2D rotate(double angle) {

        float x1 = (float) (this.x * Math.cos(angle) - this.y * Math.sin(angle));

        float y1 = (float) (this.x * Math.sin(angle) + this.y * Math.cos(angle));

        return new Point2D(x1, y1);
    }

    public double getDistance(Unit s) {
        return getDistanceTo(s.getX(), s.getY());
    }

    public Point2D toPotential(int cellSize) {
        return div(cellSize);
    }
}
