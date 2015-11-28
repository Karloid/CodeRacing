import java.util.ArrayList;

public interface PathCalcer {
    void setContext(PolygonsWorld polygonsWorld);

    void calcPath();

    public ArrayList<Point> getPath();
}
