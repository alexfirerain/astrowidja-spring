package ru.swetophor.astrowidjaspring.model.astro;

import lombok.Getter;
import lombok.Setter;
import ru.swetophor.astrowidjaspring.exception.FileFormatException;
import ru.swetophor.astrowidjaspring.model.chart.Chart;
import ru.swetophor.astrowidjaspring.utils.CelestialMechanics;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.swetophor.astrowidjaspring.utils.CelestialMechanics.getArcForHarmonic;
import static ru.swetophor.astrowidjaspring.utils.CelestialMechanics.normalizeCoordinate;

/**
 * Прототип небесного те́ла — объект,
 * имеющий идентификатор (имя) и положение (карта и координата).
 */
@Getter
@Setter
public class Astra implements ZodiacPoint {
    /**
     * Идентифицирующее имя астры.
     */
    private String name;
    /**
     * Ссылка на карту неба, в которой находится астра.
     */
    private Chart heaven;
    /**
     * Зодиакальное положение астры от 0°♈ в градусах как вещественное число от 0 до 360.
     */
    private double zodiacPosition;

    // конструкторы для задания координаты с/без минут и секунд

    /**
     * Конструктор на основе координаты в виде градусов, минут и секунд.
     *
     * @param name   имя астры.
     * @param degree градусы координаты.
     * @param minute минуты координаты.
     * @param second секунды координаты
     */
    public Astra(String name, double degree, double minute, double second) {
        this(name, degree + minute / 60 + second / 3600);
    }

    /**
     * Конструктор на основе координаты в виде градусов и минут.
     *
     * @param name   имя астры.
     * @param degree градусы координаты.
     * @param minute минуты координаты.
     */
    public Astra(String name, double degree, double minute) {
        this(name, degree + minute / 60);
    }

    /**
     * Конструктор на основе координаты в виде вещественного числа.
     *
     * @param name   имя астры.
     * @param degree координата астры в double.
     */
    public Astra(String name, double degree) {
        this.name = name;
        this.zodiacPosition = normalizeCoordinate(degree);
    }

    /**
     * Статический генератор астры из имени и координаты в произвольной форме.
     * В зависимости от количества аргументов, они трактуются как (1) градусы,
     * (2) градусы и минуты, (3) градусы, минуты и секунды или (4) номер знака
     * (1-12), градусы, минуты и секунды.
     *
     * @param name       астра, которая будет построена.
     * @param coordinate одна, две, три или четыре величины, задающие координату.
     * @return созданную на основе аргументов астру.
     * @throws IllegalArgumentException если количество аргументов, задающих
     *                                  координату, не равно одному, двум, трём или четырём.
     */
    public static Astra fromData(String name, Double... coordinate) {
        return switch (coordinate.length) {
            case 0 -> throw new IllegalArgumentException("координат нет");
            case 1 -> new Astra(name, coordinate[0]);
            case 2 -> new Astra(name, coordinate[0], coordinate[1]);
            case 3 -> new Astra(name, coordinate[0], coordinate[1], coordinate[2]);
            case 4 -> {
                if (coordinate[0] > 12 || coordinate[0] < 1)
                    throw new IllegalArgumentException("номер знака от 1 до 12");
                int signNumber = (int) (coordinate[0] - 1);
                double degrees = signNumber * 30 + coordinate[1];
                yield new Astra(name, degrees, coordinate[2], coordinate[3]);
            }
            default -> throw new IllegalArgumentException("слишком много координат");
        };
    }

    /**
     * Создаёт астру из строки специального формата.
     * Если чтение не удаётся, сообщает об этом.
     *
     * @param input строка вида "астра координаты", где 'координаты' может быть
     *              градусами, градусами и минутами или градусами,
     *              минутами и секундами - через пробел.
     * @return заполненный объект Астра.
     * @throws FileFormatException если по какой-либо причине строка не
     *              читается как корректные данные об астре.
     */
    public static Astra readFromString(String input) {
        var elements = input.trim().split(" ");
        Double[] coors;

        try {
            if (elements.length == 0)
                throw new IllegalArgumentException("текст не содержит строк");

            coors = IntStream.range(1, elements.length)
                    .mapToObj(i -> Double.parseDouble(elements[i]))
                    .collect(Collectors.toCollection(() -> new ArrayList<>(4)))
                    .toArray(Double[]::new);
        } catch (RuntimeException e) {
            throw new FileFormatException("Не удалось прочитать строку '" + input + "': " + e.getMessage());
        }

        return Astra.fromData(elements[0], coors);
    }

    public static boolean ofSameHeaven(Astra a, Astra b) {
        return a.getHeaven() == b.getHeaven();
    }

    /**
     * Выдаёт инфу об астре в виде "название (градус_зодиака)"
     *
     * @return строку с названием и градусом астры.
     */
    public String getNameWithZodiacDegree() {
        return "%s (%s)".formatted(name, getZodiacDegree());
    }

    /**
     * Устанавливает зодиакальную координату, предварительно нормализуя.
     *
     * @param zodiacPosition устанавливаемая зодиакальная координата в градусах.
     */
    public void setZodiacPosition(double zodiacPosition) {
        this.zodiacPosition = normalizeCoordinate(zodiacPosition);
    }

    /**
     * Прибавляет к зодиакальной позиции указанное число.
     *
     * @param change изменение зодиакальной координаты.
     * @return эту же астру с обновлённой координатой.
     */
    public Astra advanceCoordinateBy(double change) {
        setZodiacPosition(zodiacPosition + change);
        return this;
    }

    /**
     * Выдаёт зодиакальное положение астры, как предоставляется
     * функцией {@link CelestialMechanics#zodiacDegree(double)}
     *
     * @return строковое представление зодиакального градуса, в котором расположена астра.
     */
    public String getZodiacDegree() {
        return CelestialMechanics.zodiacDegree(zodiacPosition);
    }

    /**
     * Выдаёт астрологический символ для астры, если она распознана по названию
     * в библиотеке {@link AstraEntity АстроСущность}. Если имя не найдено среди псевдонимов,
     * библиотекой возвращается '*'.
     *
     * @return определяемый классом АстроСущность астрологический символ для известных астр, '*' для неизвестных.
     */
    public char getSymbol() {
        return AstraEntity.findSymbolFor(this);
    }

    /**
     * Выдаёт строку, представляющую символ астры и её зодиакально положение.
     *
     * @return строку вида "символ (положение)",
     * где 'положение' представлено как градус знака.
     */
    public String getSymbolWithDegree() {
        return "%s (%s)".formatted(getSymbol(), getZodiacDegree());
    }

    /**
     * Выдаёт строку, представляющую символ астры и обладателя неба с нею.
     *
     * @return строку вида "символ (обладатель)", где 'обладатель' -
     * название карты, в которой расположена астра.
     */
    public String getSymbolWithOwner() {
        return "%c (%s)".formatted(getSymbol(), heaven.getShortenedName(8));
    }

    /**
     * Возвращает строку того формата, который принят для хранения
     * данных о положении астры при сохранении.
     *
     * @return строку с инфой об астре вида "название градусы минуты секунды".
     */
    public String getString() {
        int[] coors = CelestialMechanics.degreesToCoors(zodiacPosition);
        return "%s %s %s %s%n"
                .formatted(name,
                        coors[0],
                        coors[1],
                        coors[2]);
    }


    public double getArcInHarmonicWith(int harmonic, Astra counterpart) {
        return getArcForHarmonic(this, counterpart, harmonic);
    }

    /**
     * @return знак зодиака, в котором находится астра.
     */
    public ZodiacSign getZodiacSign() {
        return ZodiacSign.getZodiumOf(zodiacPosition);
    }

    /**
     * Сообщает, что эта астра и данная — одна и та же,
     * то есть астра с тем же именем и в том же небе.
     * При этом координата сравниваемой астры может отличаться.
     * @param another сравниваемая астра.
     * @return  {@code true}, если сравниваем с астрой с тем же
     * именем и в той же карте.
     */
    public boolean isTheSame(Astra another) {
        if (another == null) return false;
        return name.equals(another.getName()) && heaven == another.getHeaven();
    }

    //    public boolean isInDirectResonanceWith(Astra counterpart, int harmonic) {
//        double effectiveOrb = analysis != counterpart.getAnalysis() && isHalfOrbsForDoubles() ?
//                getPrimalOrb() / 2 : getPrimalOrb();
//        return getArcInHarmonicWith(harmonic, counterpart) <= getPrimalOrb() && harmonic != 1
//                || getArc(getZodiacPosition(), counterpart.getZodiacPosition()) <= effectiveOrb;
//    }


    @Override
    public String toString() {
        return "%s (%s) %s"
                .formatted(name,
                        heaven.getName(),
                        getZodiacDegree());
    }
}
