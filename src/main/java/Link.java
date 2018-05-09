import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Link {
    private final double length;
    private HashSet<Point2D> points;

    public Link(Point2D point1, Point2D point2) {
        this.points = new HashSet<Point2D>();
        points.add(point1);
        points.add(point2);
        length = Utils.getEuclideDistanceSimple(point1, point2);
    }

    public boolean contain(Point2D point, Point2D point2) {
        return points.contains(point) && points.contains(point2);
    }

    public Set<Point2D> getPoints() {
        return points;
    }

    public Point2D getAnotherPoint(Point2D pointParam) {
        Iterator<Point2D> iter = points.iterator();
        Point2D point1 = iter.next();
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
