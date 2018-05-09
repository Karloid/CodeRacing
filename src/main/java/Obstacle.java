import java.awt.*;
import java.util.*;

public class Obstacle {
    private static final float RADIUS_POINT = 5;
    public static final Color NOT_CURRENT_COLOR = Color.GRAY;
    public static final Color CURRENT_OBSTACLE_COLOR = Color.GREEN;
    private static final int OFFSET = 0;
    public static final Color COLOR_FILL_POLYGON = Color.PINK;
    private final java.util.List<Point2D> points;
    private PolygonsWorld context;


    public Obstacle(PolygonsWorld context) {
        points = new ArrayList<Point2D>();
        setContext(context);
    }

    public void addPoint(Point2D point) {
        points.add(point);
        point.setObstacle(this);
        calcPoly();
    }

    private void calcPoly() {
        float a = 100;
        float b = 100;
        float[] polygonPoints = new float[points.size() * 2];

        int i = 0;
        for (Point2D point : points) {
            polygonPoints[i] = (float) point.getX();
            i++;
            polygonPoints[i] = (float) point.getY();
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
            Point2D point = points.get(points.size() - 1);
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

    public java.util.List<Point2D> getPoints() {
        return points;
    }
}
