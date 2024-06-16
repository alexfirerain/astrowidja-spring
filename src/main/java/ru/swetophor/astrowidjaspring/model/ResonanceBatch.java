package ru.swetophor.astrowidjaspringshell.model;

import lombok.Getter;
import lombok.Setter;
import ru.swetophor.astrowidjaspringshell.config.Settings;
import ru.swetophor.astrowidjaspringshell.utils.CelestialMechanics;
import ru.swetophor.astrowidjaspringshell.utils.Interpreter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.swetophor.astrowidjaspringshell.utils.CelestialMechanics.getArcForHarmonic;
import static ru.swetophor.astrowidjaspringshell.model.Harmonics.findMultiplier;
import static ru.swetophor.astrowidjaspringshell.utils.Mechanics.secondFormat;
import static ru.swetophor.astrowidjaspringshell.utils.Interpreter.ResonanceDescription;

/**
 * Гармонический анализ взаимодействия некоторых двух астр.
 * Содержит ссылки на две точки (астры) и {@link #aspects список} рассчитанных
 * для этой {@link #arc дуги} {@link Aspect аспектов}.
 * Если при определённом сочетании параметров для данной дуги
 * не будет определено ни одного аспекта, список аспектов будет пуст.
 */
@Setter
@Getter
public class ResonanceBatch {
    /**
     * Первая астра.
     */
    private Astra astra_1;
    /**
     * Вторая астра.
     */
    private Astra astra_2;
    /**
     * Угловое расстояние меж точками на небе, т.е. дуга, резонансы которой считаются.
     */
    private double arc;
    /**
     * Максимальный орбис для соединения, т.е. базовый допуск, с которым при расчётах считается,
     * что резонанс возник. Для каждой гармоники базовый орбис пропорционально уменьшается.
     */
    private double orb;
    /**
     * Наибольший проверяемый целочисленный резонанс.
     */
    private int ultimateHarmonic;
    /**
     * Найденные в пределах орбиса аспекты по росту гармоники.
     */
    private List<Aspect> aspects = new ArrayList<>();

    /**
     * Набор ссылок на карты, к которым принадлежат астры,
     * формирующие этот пучок резонансов.
     * Если это астры одного неба, набор содержит один элемент,
     * если же разных небес, то два элемента.
     */
    private Set<Chart> heavens = HashSet.newHashSet(2);

    /**
     * Получение массива аспектов для дуги между двумя астрами (конструктор)
     *
     * @param a                первая астра резонанса.
     * @param b                вторая астра резонанса.
     * @param primalOrb              первичный орбис резонанса для соединений
     *                              (должен предоставляться {@link Settings#getPrimalOrb()}),
     *                              сокращение для синастрий осуществляется внутри метода
     *                               на основании глобальной настройки {@link Settings#isHalfOrbsForDoubles()}.
     * @param ultimateHarmonic до какой гармоники продолжать анализ.
     */
    ResonanceBatch(Astra a, Astra b, double primalOrb, int ultimateHarmonic) {
        if (a == b)
            throw new IllegalArgumentException("Одна и та же астра не делает резонанса сама с собой");
        astra_1 = a;
        astra_2 = b;
        heavens.add(a.getHeaven());
        heavens.add(b.getHeaven());
        arc = CelestialMechanics.getArc(a, b);
        // возможно, для более чем двойных карт брать ещё пропорционально меньше? наверное всё же нет
        this.orb =
                isSynastric() && Settings.isHalfOrbsForDoubles() ?
                primalOrb / 2 :
                primalOrb;
        this.ultimateHarmonic = ultimateHarmonic;

        IntStream.rangeClosed(1, ultimateHarmonic).forEach(h -> {
            double arcInHarmonic = getArcForHarmonic(a, b, h);
            if (arcInHarmonic < orb && isNewSimple(h))
                aspects.add(new Aspect(h, arcInHarmonic, arc, orb));
        });
    }

    /**
     * Конструктор резонанса, использующий глобальные значения
     * первичного орба и количества анализируемых гармоник.
     * @param a первая астра резонанса.
     * @param b вторая астра резонанса.
     */
    ResonanceBatch(Astra a, Astra b) {
        this(a, b, Settings.getPrimalOrb(), Settings.getEdgeHarmonic());
    }

    /**
     * Вспомогательный метод отсечения кратных гармоник при заполнении списка аспектов.
     *
     * @param aNewNumber число, которое проверяется на кратность уже найденным аспектам.
     * @return {@code истинно}, если проверяемое число не кратно никакому из {@link #aspects уже найденных} (кроме 1),
     * а также не является точным соединением, проходящим до данной гармоники. Следовательно,
     * эту гармонику надо брать в {@link #aspects набор}. Если же {@code ложно}, брать её в набор не нужно.
     */
    private boolean isNewSimple(int aNewNumber) {
        boolean isConjunction = false;

        for (Aspect next : aspects) {
            int aPreviousHarmonic = next.getNumeric();

            if (aPreviousHarmonic == 1)
                isConjunction = true;

            if (aNewNumber % aPreviousHarmonic != 0)
                continue;

            if (isConjunction &&
                    arc > orb / aNewNumber &&
                    findMultiplier(aNewNumber, arc, orb) == 1)
                continue;

            return false;
        }
        return true;
    }

    /**
     * Является ли этот аспект аспектом между астрами двух небес.
     * @return  {@code ДА}, если аспект между двумя картами.
     */
    public boolean isSynastric() {
        return heavens.size() == 2;
    }

    /**
     * Является ли этот аспект аспектом между астрами одного неба.
     * @return  {@code ДА}, если аспект внутри одной карты.
     */
    public boolean isNonSynastric() {
        return heavens.size() == 1;
    }

    /**
     * Выдаёт многостроку, в каждой обозначение аспекта (модифицированное
     * {@link Interpreter}) и описание, предоставляемое аспектным {@link Aspect#toString toString()}.
     * Если в пучке резонансов при данных настройках не распознано
     * ни одного аспекта — строку, сообщающую об этом.
     * @return  текстовое описание этого пучка резонансов.
     */
    private String aspectsInfo() {
        StringBuilder sb = new StringBuilder();

        if (aspects.isEmpty())
            sb.append("Ни одного резонанса до %d при орбисе %s%n".formatted(ultimateHarmonic, orb));

        for (Aspect aspect : getAspectsByStrength()) {
            sb.append(ResonanceDescription(aspect.getNumeric(), aspect.getMultiplicity()));
            sb.append(aspect);
        }
        return sb.toString();
    }

    /**
     * Описательная многострока, объединяющая вывод заголовка
     * и отчёта по аспектам.
     * @return  полное стандартное представление пучка резонансов.
     */
    public String resonancesOutput() {
        return getTitle() +
                aspectsReport();
    }

    /**
     * Выдаёт список распознанных в этом пучке аспектов,
     * сортированный по убыванию силы (или росту зазора).
     * @return  список {@link Aspect аспектов}.
     */
    private List<Aspect> getAspectsByStrength() {
        return aspects.stream()
                .sorted(Comparator.comparing(Aspect::getStrength).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Выдаёт текстовое описание резонансов (аспектов), описанных для этой дуги.
     * @return многострочник, где каждая строка характеризует аспект (резонансное
     * число и множитель) и его силу (в нескольких возможных представлениях).
     * Если резонанс пуст, то строку с сообщением об этом.
     */
    public String aspectsReport() {
        StringBuilder sb = new StringBuilder();

        if (aspects.isEmpty())
            sb.append("Ни одного резонанса до %d при орбисе %s%n".formatted(ultimateHarmonic, orb));
        aspects.stream()
                .sorted(Comparator.comparing(Aspect::getStrength).reversed())
                .forEach(aspect -> {
                    sb.append(ResonanceDescription(aspect.getNumeric(), aspect.getMultiplicity()));
                    sb.append("Резонанс %d/%d %s (%.0f%%) --- %.2f %n".formatted(
                            aspect.getMultiplicity(),
                            aspect.getNumeric(),
                            aspect.strengthRating(),
                            aspect.getStrength(),
                            aspect.getStrength() / Math.pow(Math.log(aspect.getNumeric() + 1.0), 0.5)));
        });
        return sb.toString();
    }

    /**
     * Сообщает, что запрашиваемое число присутствует
     * среди множителей хотя бы одного из аспектов в резонансе.
     * @param harmonic проверяемое на кратность гармоническое число.
     * @return  {@code true}, если резонансное число хотя бы одного аспекта, определённого
     * для этого резонанса, является кратным запрашиваемому числу, {@code false}в ином случае.
     */
    public boolean hasResonanceElement(int harmonic) {
        return aspects.stream()
                .anyMatch(a -> a.hasMultiplier(harmonic));
    }

    /**
     * Сообщает, что среди аспектов в резонансе присутствует указанное резонансное число.
     * @param harmonic проверяемая на наличие резонанса гармоника.
     * @return  {@code true}, если среди аспектов, определённых для этого резонанса,
     * присутствует указанное число, {@code false} в ином случае.
     * При этом, например, аспект трин хоть и присутствует в карте 6-й гармоники как соединение,
     * но для harmonic = 6 имеющий его объект резонанса вернёт {@code false}.
     */
    public boolean hasGivenHarmonic(int harmonic) {
        return aspects.stream()
                .anyMatch(a -> a.getNumeric() == harmonic);
    }

    /**
     * Сообщает, что в указанной гармонике этот резонанс предстаёт соединением.
     * @param harmonic проверяемое на наличие резонанса число.
     * @return  {@code true}, если астры резонанса имеют связь (резонанс) по
     * указанной гармонике, как определяется методом {@link Aspect#hasResonance(int) hasResonance()}.
     */
    public boolean hasHarmonicPattern(int harmonic) {
        return aspects.stream()
                .anyMatch(a -> a.hasResonance(harmonic));
    }

    /**
     * Возвращает ту астру резонанса, которая не равна указанной.
     *
     * @param taken указанная астра резонанса.
     * @return {@code null}, если указана астра, которой нет в резонансе,
     * если же указана одна из астр в наличии, возвращается вторая.
     */
    public Astra getCounterpart(Astra taken) {
        if (astra_1.equals(taken))
            return astra_2;
        if (astra_2.equals(taken))
            return astra_1;
        return null;
    }

    /**
     * <p>Выдаёт строку, описывающую резонанс в виде:</p>
     * <p>для аспекта внутри одной карты:</p>
     * {@code * Дуга между {символ} {координата} и {символ} {координата} ({карта}) = {дуга}}
     * <p>для аспекта между разными картами:</p>
     * {@code * Дуга между {символ} {координата} ({карта}) и {символ} {координата} ({карта}) = {дуга}}
     * @return строку, рассказывающую, между какими астрами это дуга,
     * по которой вычислены аспекты, и какая у ней величина в градусах.
     */
    public String getTitle() {
        return isSynastric() ?

                "%n* Дуга между %c %s (%s) и %c %s (%s) = %s%n".formatted(
                astra_1.getSymbol(), astra_1.getZodiacDegree(),
                astra_1.getHeaven().getName(),
                astra_2.getSymbol(), astra_2.getZodiacDegree(),
                astra_2.getHeaven().getName(),
                secondFormat(arc, true)) :

                "%n* Дуга между %c %s и %c %s (%s) = %s%n".formatted(
                astra_1.getSymbol(), astra_1.getZodiacDegree(),
                astra_2.getSymbol(), astra_2.getZodiacDegree(),
                astra_1.getHeaven().getName(),
                secondFormat(arc, true));
    }

}
