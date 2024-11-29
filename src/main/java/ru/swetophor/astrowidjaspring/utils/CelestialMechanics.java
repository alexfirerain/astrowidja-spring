package ru.swetophor.astrowidjaspring.utils;

import ru.swetophor.astrowidjaspring.config.Settings;
import ru.swetophor.astrowidjaspring.model.astro.Astra;
import ru.swetophor.astrowidjaspring.model.astro.ZodiacPoint;

import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static ru.swetophor.astrowidjaspring.model.astro.ZodiacSign.zodiumIcon;

/**
 * Инструментальный класс, содержащий константы и методы
 * для вычисления координат.
 */
public final class CelestialMechanics {
    /**
     * Круг в градусах, т.е. 360.
     */
    public static final double CIRCLE = 360.0;
    /**
     * Полкруга в градусах, т.е. 180.
     */
    public static final double HALF_CIRCLE = 180.0;

    /**
     * Вычисляет эклиптическую дугу между двумя точками на большом круге
     *
     * @param a первая координата точки.
     * @param b вторая координата точки.
     * @return наименьшую дугу между двумя указанными точками дуги.
     */
    public static double getArc(double a, double b) {
        double arc = abs(normalizeCoordinate(a) - normalizeCoordinate(b));
        return arc > HALF_CIRCLE ?
                CIRCLE - arc :
                arc;
    }

    /**
     * Вычисляет эклиптическую дугу между зодиакальными точками.
     * @param a первая точка (астра).
     * @param b вторая точка (астра).
     * @return наименьшую дугу между двумя указанными астрами (от 0° до 180°).
     */
    public static double getArc(ZodiacPoint a, ZodiacPoint b) {
        return getArc(a.getZodiacPosition(), b.getZodiacPosition());
    }

    /**
     * Вычисляет эклиптическую дугу в направлении от первой
     * зодиакальной точки до второй.
     * @param a первая точка (астра).
     * @param b вторая точка (астра).
     * @return  направленную дугу от точки {@code a} к точке {@code b} (от 0° до 359°59'59").
     */
    public static double getVectorArc(ZodiacPoint a, ZodiacPoint b) {
        double arc = b.getZodiacPosition() - a.getZodiacPosition();
        return arc < 0.0 ? 360.0 + arc : arc;
    }

    /**
     * Приводит дугу к расстоянию меж ея концами.
     * @param a произвольная дуга в градусах.
     * @return  абсолютное расстояние между концами дуги.
     */
    public static double normalizeArc(double a) {
        return getArc(normalizeCoordinate(a), 0);
    }

    /**
     * Приводит координату в диапазон от 0° до 359°59'59".
     *
     * @param p нормализуемая координата.
     * @return координату от 0° до 359°59'59", равную данной.
     */
    public static double normalizeCoordinate(double p) {
        p %= CIRCLE;
        return p < 0 ? p + CIRCLE : p;
    }

    /**
     * Находит среднюю координату (мидпойнт) для двух зодиакальных позиций.
     *
     * @param positionA первая позиция.
     * @param positionB вторая позиция.
     * @return среднюю координату между двумя заданными со стороны меньшей
     * дуги между ними. Если дуга равна ровно половине Круга, возвращается точка,
     * следующая через четверть после точки, переданной первой.
     */
    public static double findMedian(double positionA, double positionB) {
        double arc = getArc(positionA, positionB);
        double backPosition =
                normalizeCoordinate(positionA + arc) == positionB ?
                        positionA : positionB;

        return normalizeCoordinate(backPosition + arc / 2);
    }

    /**
     * Выдаёт зодиакальный градус указанной зодиакальной позиции в виде «градус°символ»
     *
     * @param position зодиакальная позиция.
     * @return строковое представление градуса и знака Зодиака.
     */
    public static String zodiacDegree(double position) {

        return "%d°%s"
                .formatted(
                        (int) Math.ceil(position % 30),
                        zodiumIcon(position));
    }

    /**
     * Превращает координату дуги из градусов в массив [градусы, минуты, секунды].
     *
     * @param position дуга в градусах и дробных долях градуса.
     * @return массив из трёх целых величин: градусы, минуты и секунды.
     */
    public static int[] degreesToCoors(double position) {
        int[] coors = new int[3];
        int inSeconds = (int) round((normalizeCoordinate(position) * 3600));
        coors[0] = inSeconds / 3600;
        coors[1] = inSeconds % 3600 / 60;
        coors[2] = inSeconds % 60;
        return coors;
    }

    /**
     * Превращает координату дуги из градусов в массив {@code [знак, градусы, минуты, секунды]},
     * где {@code знак} это натуральное число от 1 до 12.
     * @param position  дуга в градусах.
     * @return  массив из четырёх целых величин: номер знака, градусы, минуты и секунды.
     */
    public static int[] degreesToSignAndCoors(double position) {
        int[] coors = new int[4];
        int[] degreesCoors = degreesToCoors(position);
        coors[0] = degreesCoors[0] / 30 + 1;
        coors[1] = degreesCoors[0] % 30;
        coors[2] = degreesCoors[1];
        coors[3] = degreesCoors[2];
        return coors;
    }

    /**
     * Определяет, находится ли вторая координата в первой половине круга,
     * считая от первой координаты.
     * Например, если Луна находится в первой половине круга от Солнца,
     * она является растущей, иначе – убывающей.
     *
     * @param fromCoordinate координата, от которой считается.
     * @param coordinate     координата, для которой определяется фаза.
     * @return {@code true}, если вторая координата равна первой или отстоит
     * от неё по ходу движения Зодиака менее, чем на 180°.
     * {@code false}, если вторая координата находится по ходу
     * движения Зодиака в 180° или более от первой.
     */
    public static boolean isAhead(double fromCoordinate, double coordinate) {
        double delta = coordinate - fromCoordinate;
        return delta >= 0 && delta < HALF_CIRCLE || delta < -HALF_CIRCLE;
    }

    /**
     * Определяет, находится ли вторая астра в первой половине круга,
     * считая от первой астры.
     * Например, если Луна находится в первой половине круга от Солнца,
     * она является растущей, иначе – убывающей.
     *
     * @param fromPlanet астра, от которой считается.
     * @param planet     астра, для которой определяется фаза.
     * @return {@code true}, если вторая астра находится в соединении
     * с первой или менее, чем в 180° от неё по ходу движения Зодиака.
     * {@code false}, если вторая астра находится в 180° или более от
     * первой по ходу движения Зодиака.
     */
    public static boolean isAhead(ZodiacPoint fromPlanet, ZodiacPoint planet) {
        return isAhead(fromPlanet.getZodiacPosition(), planet.getZodiacPosition());
    }

    /**
     * Сообщает расстояние между астрами в карте некоторой гармоники.
     * @param a первая астра.
     * @param b вторая астра.
     * @param harmonic  номер гармоники.
     * @return  угловое расстояние между астрами в карте указанной гармоники.
     */
    public static double getArcForHarmonic(ZodiacPoint a, ZodiacPoint b, int harmonic) {
        return normalizeArc(getArc(a, b) * harmonic);
    }

    /**
     * Рассчитывает условную силу в процентах: насколько точность аспекта близка к экзакту,
     * а именно: насколько разность дуги с чистым аспектом (зазор) близка к нулю.
     * Если даётся зазор в гармонике, предполагается, что максимальный орбис равен
     * {@link Settings#getPrimalOrb()} (или его половине для двойных, если включено);
     * если же зазор даётся в абсолютной разности зодиакальных долгот,
     * то аналогично подразумевается абсолютный орбис для данной гармоники.
     *
     * @param orb       максимальный орбис, при котором аспект срабатывает действующим в карте гармоники.
     * @param clearance зазор между дугой и аспектом, т.е. эффективный орбис.
     * @return 100, если аспект точный, 0, если точность аспекта на грани максимального орбиса для
     * данной гармоники, отрицательное значение, если зазор превышает максимальный орбис
     * (до -100, если зазор равен 180°).
     */
    public static double calculateStrength(double orb, double clearance) {
        double delta = orb - clearance;
        return delta >= 0 ?
                delta / orb * 100 :
                delta / (HALF_CIRCLE - orb) * 100;
    }

    /**
     * Определяет, что две переданные астры принадлежат одной карте и
     * находятся в соединении. Орбис берётся из глобальных настроек.
     * @param a     одна астра.
     * @param b     другая астра.
     * @return  {@code ДА}, если обе астры расположены в одном небе
     * и расстояние между ними не больше, чем стандартный определяемый
     * в программе глобальный первичный орбис.
     */
    public static boolean areConjuncted(Astra a, Astra b) {
        return Astra.ofSameHeaven(a, b) &&
                getArc(a, b) <= Settings.getPrimalOrb();
    }

    /**
     * Сообщает, находятся ли астры в соединении. Орб смотрится соответственно
     * глобальным настройкам с различением для синастрических и нет.
     * @param a одна астра
     * @param b другая астра
     * @return  {@code ДА}, если расстояние между астрами
     * (из одной или разных карт) не превышает первичного орба соединения.
     */
    public static boolean conjuncting(Astra a, Astra b) {
        double orb = !Astra.ofSameHeaven(a, b) && Settings.isHalfOrbsForDoubles() ?
                Settings.getPrimalOrb() / 2 : Settings.getPrimalOrb();
        return getArc(a, b) <= orb;
    }

    /**
     * Сортирует список зодиакальных координат так, чтобы
     * все они шли в порядке возрастания зодиакальной долготы,
     * при этом расстояние между первой и последней больше,
     * чем между любыми соседними парами.
     * @param astras список точек, который нужно сортировать.
     */
    public static void arrangeAsChain(List<? extends ZodiacPoint> astras) {
        if (astras.size() > 1) {
            astras.sort(Comparator.comparing(ZodiacPoint::getZodiacPosition));

            double maxDist = getVectorArc(astras.getLast(), astras.getFirst());
            int maxDistIndex = 0;

            for (int i = 1; i < astras.size(); i++) {
                double dist = getVectorArc(astras.get(i - 1), astras.get(i));
                if (dist > maxDist) {
                    maxDistIndex = i;
                    maxDist = dist;
                }
            }
            Collections.rotate(astras, -maxDistIndex);
        }
    }

    /**
     * Сортирует список зодиакальных координат так, чтобы
     * все они шли в порядке возрастания зодиакальной долготы,
     * при этом расстояние между первой и последней больше,
     * чем между любыми соседними парами.
     * @param astras    список точек, который нужно сортировать.
     * @return тот же список, отсортированный по порядку
     * явления этих точек в Зодиаке.
     */
    public static List<ZodiacPoint> getArrangedAsChain(Collection<? extends ZodiacPoint> astras) {
        List<ZodiacPoint> newList = new ArrayList<>(astras);
        arrangeAsChain(newList);
        return newList;
    }

    /**
     * Вычисляет "центр тяжести" (центроид) для нескольких точек
     * на зодиакальном круге.
     * @param points    точки, для которых считается средняя.
     * @return  координату на окружности, являющуюся средней для данных точек.
     */
    public static double calculateAvg(ZodiacPoint... points) {
        if (points.length == 1)
            return points[0].getZodiacPosition();

        List<ZodiacPoint> astras = new ArrayList<>(List.of(points));
        arrangeAsChain(astras);

        ZodiacPoint first = astras.getFirst();
        double sum = IntStream.range(1, astras.size())
                .mapToDouble(i -> getVectorArc(first, astras.get(i)))
                .sum();
        return normalizeCoordinate(first.getZodiacPosition() + sum / astras.size());
    }

}
