package model;

/**
 * Класс, определяющий кодемобиль. Содержит также все свойства прямоугольного юнита.
 */
public class Car extends RectangularUnit {
    private final long playerId;
    private final int teammateIndex;
    private final boolean teammate;
    private final CarType type;

    private int projectileCount;
    private int nitroChargeCount;
    private int oilCanisterCount;

    private int remainingProjectileCooldownTicks;
    private int remainingNitroCooldownTicks;
    private int remainingOilCooldownTicks;

    private int remainingNitroTicks;
    private int remainingOiledTicks;

    private double durability;

    private double enginePower;
    private double wheelTurn;

    private int nextWaypointIndex;
    private int nextWaypointX;
    private int nextWaypointY;

    private boolean finishedTrack;

    public void setProjectileCount(int projectileCount) {
        this.projectileCount = projectileCount;
    }

    public void setNitroChargeCount(int nitroChargeCount) {
        this.nitroChargeCount = nitroChargeCount;
    }

    public void setOilCanisterCount(int oilCanisterCount) {
        this.oilCanisterCount = oilCanisterCount;
    }

    public void setRemainingProjectileCooldownTicks(int remainingProjectileCooldownTicks) {
        this.remainingProjectileCooldownTicks = remainingProjectileCooldownTicks;
    }

    public void setRemainingNitroCooldownTicks(int remainingNitroCooldownTicks) {
        this.remainingNitroCooldownTicks = remainingNitroCooldownTicks;
    }

    public void setRemainingOilCooldownTicks(int remainingOilCooldownTicks) {
        this.remainingOilCooldownTicks = remainingOilCooldownTicks;
    }

    public void setRemainingNitroTicks(int remainingNitroTicks) {
        this.remainingNitroTicks = remainingNitroTicks;
    }

    public void setRemainingOiledTicks(int remainingOiledTicks) {
        this.remainingOiledTicks = remainingOiledTicks;
    }

    public void setDurability(double durability) {
        this.durability = durability;
    }

    public void setEnginePower(double enginePower) {
        this.enginePower = enginePower;
    }

    public void setWheelTurn(double wheelTurn) {
        this.wheelTurn = wheelTurn;
    }

    public void setNextWaypointIndex(int nextWaypointIndex) {
        this.nextWaypointIndex = nextWaypointIndex;
    }

    public void setNextWaypointX(int nextWaypointX) {
        this.nextWaypointX = nextWaypointX;
    }

    public void setNextWaypointY(int nextWaypointY) {
        this.nextWaypointY = nextWaypointY;
    }

    public void setFinishedTrack(boolean finishedTrack) {
        this.finishedTrack = finishedTrack;
    }

    public Car(
            long id, double mass, double x, double y, double speedX, double speedY, double angle, double angularSpeed,
            double width, double height, long playerId, int teammateIndex, boolean teammate, CarType type,
            int projectileCount, int nitroChargeCount, int oilCanisterCount, int remainingProjectileCooldownTicks,
            int remainingNitroCooldownTicks, int remainingOilCooldownTicks, int remainingNitroTicks,
            int remainingOiledTicks, double durability, double enginePower, double wheelTurn, int nextWaypointIndex,
            int nextWaypointX, int nextWaypointY, boolean finishedTrack) {
        super(id, mass, x, y, speedX, speedY, angle, angularSpeed, width, height);

        this.playerId = playerId;
        this.teammateIndex = teammateIndex;
        this.teammate = teammate;
        this.type = type;
        this.projectileCount = projectileCount;
        this.nitroChargeCount = nitroChargeCount;
        this.oilCanisterCount = oilCanisterCount;
        this.remainingProjectileCooldownTicks = remainingProjectileCooldownTicks;
        this.remainingNitroCooldownTicks = remainingNitroCooldownTicks;
        this.remainingOilCooldownTicks = remainingOilCooldownTicks;
        this.remainingNitroTicks = remainingNitroTicks;
        this.remainingOiledTicks = remainingOiledTicks;
        this.durability = durability;
        this.enginePower = enginePower;
        this.wheelTurn = wheelTurn;
        this.nextWaypointIndex = nextWaypointIndex;
        this.nextWaypointX = nextWaypointX;
        this.nextWaypointY = nextWaypointY;
        this.finishedTrack = finishedTrack;
    }

    public Car(Car other) {
        super(other);
        this.playerId = other.playerId;
        this.teammateIndex = other.teammateIndex;
        this.teammate = other.teammate;
        this.type = other.type;
        this.projectileCount = other.projectileCount;
        this.nitroChargeCount = other.nitroChargeCount;
        this.oilCanisterCount = other.oilCanisterCount;
        this.remainingProjectileCooldownTicks = other.remainingProjectileCooldownTicks;
        this.remainingNitroCooldownTicks = other.remainingNitroCooldownTicks;
        this.remainingOilCooldownTicks = other.remainingOilCooldownTicks;
        this.remainingNitroTicks = other.remainingNitroTicks;
        this.remainingOiledTicks = other.remainingOiledTicks;
        this.durability = other.durability;
        this.enginePower = other.enginePower;
        this.wheelTurn = other.wheelTurn;
        this.nextWaypointIndex = other.nextWaypointIndex;
        this.nextWaypointX = other.nextWaypointX;
        this.nextWaypointY = other.nextWaypointY;
        this.finishedTrack = other.finishedTrack;
    }

    /**
     * @return Возвращает идентификатор игрока, которому принадлежит кодемобиль.
     */
    public long getPlayerId() {
        return playerId;
    }

    /**
     * @return Возвращает 0-индексированный номер кодемобиля среди юнитов одного игрока.
     */
    public int getTeammateIndex() {
        return teammateIndex;
    }

    /**
     * @return Возвращает {@code true}, если и только если данный кодемобиль принадлежит вам.
     */
    public boolean isTeammate() {
        return teammate;
    }

    /**
     * @return Возвращает тип кодемобиля.
     */
    public CarType getType() {
        return type;
    }

    /**
     * @return Возвращает количество метательных снарядов.
     */
    public int getProjectileCount() {
        return projectileCount;
    }

    /**
     * @return Возвращает количество зарядов для системы закиси азота.
     */
    public int getNitroChargeCount() {
        return nitroChargeCount;
    }

    /**
     * @return Возвращает количество канистр с мазутом.
     */
    public int getOilCanisterCount() {
        return oilCanisterCount;
    }

    /**
     * @return Возвращает количество тиков, по прошествии которого кодемобиль может запустить очередной снаряд,
     * или {@code 0}, если кодемобиль может совершить данное действие в текущий тик.
     */
    public int getRemainingProjectileCooldownTicks() {
        return remainingProjectileCooldownTicks;
    }

    /**
     * @return Возвращает количество тиков, по прошествии которого кодемобиль может использовать очередной заряд системы
     * закиси азота, или {@code 0}, если кодемобиль может совершить данное действие в текущий тик.
     */
    public int getRemainingNitroCooldownTicks() {
        return remainingNitroCooldownTicks;
    }

    /**
     * @return Возвращает количество тиков, по прошествии которого кодемобиль может разлить очередную лужу мазута,
     * или {@code 0}, если кодемобиль может совершить данное действие в текущий тик.
     */
    public int getRemainingOilCooldownTicks() {
        return remainingOilCooldownTicks;
    }

    /**
     * @return Возвращает количество оставшихся тиков действия системы закиси азота.
     */
    public int getRemainingNitroTicks() {
        return remainingNitroTicks;
    }

    /**
     * @return Возвращает количество тиков, оставшихся до полного высыхания кодемобиля, попавшего в лужу мазута.
     */
    public int getRemainingOiledTicks() {
        return remainingOiledTicks;
    }

    /**
     * @return Возвращает текущую прочность кодемобиля в интервале [{@code 0.0}, {@code 1.0}].
     */
    public double getDurability() {
        return durability;
    }

    /**
     * @return Возвращает относительную мощность двигателя кодемобиля. Значение находится в интервале
     * [{@code -1.0}, {@code 1.0}] кроме случаев, когда кодемобиль использует ускорение <<нитро>>.
     */
    public double getEnginePower() {
        return enginePower;
    }

    /**
     * @return Возвращает относительный угол поворота колёс (или руля, что эквивалентно) кодемобиля в интервале
     * [{@code -1.0}, {@code 1.0}].
     */
    public double getWheelTurn() {
        return wheelTurn;
    }

    /**
     * @return Возвращает индекс следующего ключевого тайла в массиве {@code world.waypoints}.
     */
    public int getNextWaypointIndex() {
        return nextWaypointIndex;
    }

    /**
     * @return Возвращает компоненту X позиции следующего ключевого тайла.
     * Конвертировать позицию в точные координаты можно, используя значение {@code game.trackTileSize}.
     */
    public int getNextWaypointX() {
        return nextWaypointX;
    }

    /**
     * @return Возвращает компоненту Y позиции следующего ключевого тайла.
     * Конвертировать позицию в точные координаты можно, используя значение {@code game.trackTileSize}.
     */
    public int getNextWaypointY() {
        return nextWaypointY;
    }

    /**
     * @return Возвращает {@code true}, если и только если данный кодемобиль финишировал. Финишировавший кодемобиль
     * перестаёт управляться игроком, а также участвовать в столкновениях с другими юнитами.
     */
    public boolean isFinishedTrack() {
        return finishedTrack;
    }
}
