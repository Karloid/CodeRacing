import model.Car;
import model.Game;
import model.Move;
import model.World;

import java.util.ArrayList;
import java.util.List;

public class LibGdxDataToPaint {
    public Move move;
    public Car mainUnit;
    public Game game;
    public World world;
    public List<SimContext> allSimContexts;
    public SimContext bestSimContext;
    public ArrayList<Point2D> carPoints;
}
