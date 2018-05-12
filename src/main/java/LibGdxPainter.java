public class LibGdxPainter implements MyStrategyPainter {
    private final LibGdxShower shower;
    private MyKStrategy myStrategy;
    private LibGdxDataToPaint data;

    public LibGdxPainter(LibGdxShower shower) {
        this.shower = shower;
    }

    @Override
    public void onStartTick() {
        data = new LibGdxDataToPaint();

    }

    @Override
    public void setMYS(MyKStrategy myStrategy) {

        this.myStrategy = myStrategy;
    }

    @Override
    public void onEndTick() {
        data.move = myStrategy.getCurrentMove();

        shower.setObjects(data);
    }

    @Override
    public void onInitializeStrategy() {

    }

    @Override
    public void drawMove() {

    }
}
