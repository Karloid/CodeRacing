public class LibGdxPainter implements MyStrategyPainter {
    private final LibGdxShower shower;
    private MyKStrategy mys;
    private LibGdxDataToPaint data;

    public LibGdxPainter(LibGdxShower shower) {
        this.shower = shower;
    }

    @Override
    public void onStartTick() {
        data = new LibGdxDataToPaint();
        data.game = mys.game;
    }

    @Override
    public void setMYS(MyKStrategy myStrategy) {
        this.mys = myStrategy;
    }

    @Override
    public void onEndTick() {
        data.move = mys.getCurrentMove();

        data.world = mys.world;

        data.mainUnit = mys.self;

        shower.setObjects(data);
    }

    @Override
    public void onInitializeStrategy() {

    }

    @Override
    public void drawMove() {

    }
}
