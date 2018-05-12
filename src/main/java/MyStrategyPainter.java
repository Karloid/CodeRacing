public interface MyStrategyPainter {
    void onStartTick();

    void setMYS(MyKStrategy myStrategy);

    void onEndTick();

    void onInitializeStrategy();

    void drawMove();
}
