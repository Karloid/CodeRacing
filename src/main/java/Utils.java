public class Utils {
    public static double getEuclideDistanceSimple(Point position, Point position1) {
        return Math.sqrt(Math.pow(position.getX() - position1.getX(), 2) + Math.pow(position.getY() - position1.getY(), 2));
    }

    public static Point getPointByAzimuthDistance(Point point, double azimuth, double distance) {
        return new Point((int) (point.getX() + distance * Math.cos(azimuth)),
                (int) (point.getY() + distance * Math.sin(azimuth)), point.getContext());
    }
}
