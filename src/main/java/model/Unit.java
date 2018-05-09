package model;

import static java.lang.StrictMath.*;

/**
 * Базовый класс для определения объектов (<<юнитов>>) на игровом поле.
 */
public abstract class Unit {
    private long id;
    private double mass;
    public double x;
    public double y;
    private double speedX;
    private double speedY;
    private double angle;
    private double angularSpeed;

    protected Unit(
            long id, double mass, double x, double y, double speedX, double speedY, double angle, double angularSpeed) {
        this.id = id;
        this.mass = mass;
        this.x = x;
        this.y = y;
        this.speedX = speedX;
        this.speedY = speedY;
        this.angle = angle;
        this.angularSpeed = angularSpeed;
    }

    public Unit(Unit other) {
        this.id = other.id;
        this.mass = other.mass;
        this.x = other.x;
        this.y = other.y;
        this.speedX = other.speedX;
        this.speedY = other.speedY;
        this.angle = other.angle;
        this.angularSpeed = other.angularSpeed;
    }

    /**
     * @return Возвращает уникальный идентификатор объекта.
     */
    public long getId() {
        return id;
    }

    /**
     * @return Возвращает массу объекта в единицах массы.
     */
    public double getMass() {
        return mass;
    }

    /**
     * @return Возвращает X-координату центра объекта. Ось абсцисс направлена слева направо.
     */
    public double getX() {
        return x;
    }

    /**
     * @return Возвращает Y-координату центра объекта. Ось ординат направлена сверху вниз.
     */
    public final double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setSpeedX(double speedX) {
        this.speedX = speedX;
    }

    public void setSpeedY(double speedY) {
        this.speedY = speedY;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public void setAngularSpeed(double angularSpeed) {
        this.angularSpeed = angularSpeed;
    }

    /**
     * @return Возвращает X-составляющую скорости объекта. Ось абсцисс направлена слева направо.
     */
    public final double getSpeedX() {
        return speedX;
    }

    /**
     * @return Возвращает Y-составляющую скорости объекта. Ось ординат направлена сверху вниз.
     */
    public final double getSpeedY() {
        return speedY;
    }

    /**
     * @return Возвращает угол поворота объекта в радианах. Нулевой угол соответствует направлению оси абсцисс.
     * Положительные значения соответствуют повороту по часовой стрелке.
     */
    public final double getAngle() {
        return angle;
    }

    /**
     * @return Возвращает скорость вращения объекта.
     * Положительные значения соответствуют вращению по часовой стрелке.
     */
    public double getAngularSpeed() {
        return angularSpeed;
    }

    /**
     * @param x X-координата точки.
     * @param y Y-координата точки.
     * @return Возвращает ориентированный угол [{@code -PI}, {@code PI}] между направлением
     * данного объекта и вектором из центра данного объекта к указанной точке.
     */
    public double getAngleTo(double x, double y) {
        double absoluteAngleTo = atan2(y - this.y, x - this.x);
        double relativeAngleTo = absoluteAngleTo - angle;

        while (relativeAngleTo > PI) {
            relativeAngleTo -= 2.0D * PI;
        }

        while (relativeAngleTo < -PI) {
            relativeAngleTo += 2.0D * PI;
        }

        return relativeAngleTo;
    }

    /**
     * @param unit Объект, к центру которого необходимо определить угол.
     * @return Возвращает ориентированный угол [{@code -PI}, {@code PI}] между направлением
     * данного объекта и вектором из центра данного объекта к центру указанного объекта.
     */
    public double getAngleTo(Unit unit) {
        return getAngleTo(unit.x, unit.y);
    }

    /**
     * @param x X-координата точки.
     * @param y Y-координата точки.
     * @return Возвращает расстояние до точки от центра данного объекта.
     */
    public double getDistanceTo(double x, double y) {
        return hypot(x - this.x, y - this.y);
    }

    /**
     * @param unit Объект, до центра которого необходимо определить расстояние.
     * @return Возвращает расстояние от центра данного объекта до центра указанного объекта.
     */
    public double getDistanceTo(Unit unit) {
        return getDistanceTo(unit.x, unit.y);
    }
}
