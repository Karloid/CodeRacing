import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Link {
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
