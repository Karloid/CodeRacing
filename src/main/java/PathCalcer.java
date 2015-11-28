import java.util.ArrayList;

public interface PathCalcer {
    void setContext(PolygonsWorld polygonsWorld);

    boolean calcPath();

    public ArrayList<Point> getPath();
}
