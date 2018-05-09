import model.Unit;

import java.util.List;

public class Utils {
    public static double getEuclideDistanceSimple(Point2D position, Point2D position1) {
        return Math.sqrt(Math.pow(position.getX() - position1.getX(), 2) + Math.pow(position.getY() - position1.getY(), 2));
    }

    public static Point2D getPointByAzimuthDistance(Point2D point, double azimuth, double distance) {
        return new Point2D((int) (point.getX() + distance * Math.cos(azimuth)),
                (int) (point.getY() + distance * Math.sin(azimuth)), point.getContext());
    }

    static final String LOG_MOVING = "MOVING";
    public static String WARN = "WARN";

    public static String format(double v) {
        return String.format("%.2f", v);
    }


    public static double normalizeAngle(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    public static double normalizeAngleFast(double a, double center) {
        return a - (Math.PI * 2) * Math.floor((a + Math.PI - center) / (Math.PI * 2));
    }

    public static double mod(double a, double n) {
        return a - Math.floor(a / n) * n;
    }

    public static boolean containsByPosition(List<Unit> units, Unit unit) {
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if ((int) u.x == (int) unit.x && (int) u.y == (int) unit.y) {
                return true;
            }
        }
        return false;
    }

    public static void log(String str) {
        System.err.println(str);
    }

}
