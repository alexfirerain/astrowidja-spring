package ru.swetophor.astrowidjaspringshell.model;

import lombok.Getter;
import ru.swetophor.astrowidjaspringshell.config.Settings;
import ru.swetophor.astrowidjaspringshell.utils.CelestialMechanics;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.swetophor.astrowidjaspringshell.utils.CelestialMechanics.calculateStrength;
import static ru.swetophor.astrowidjaspringshell.utils.CelestialMechanics.getArcForHarmonic;

/**
 * Олицетворяет группу связанных каким-то резонансом точек
 * (астр из одной или нескольких карт).
 * Точки считаются связанными по данной гармонике, если между ними присутствует
 * соответствующий резонанс; для принадлежности паттерну точка должна иметь
 * резонанс по крайней мере с одной из входящих в него точек.
 */
@Getter
public class Pattern {

    /**
     * По какому гармоническому числу выделен паттерн.
     */
    private final int harmonic;
    /**
     * {@link AstroMatrix Матрица резонансов}, на базе которой выделяются аспекты и паттерны.
     */
    private final AstroMatrix analysis;
    /**
     * Сопоставление астр и суммы их зазоров с каждой другой астрой паттерна.
     */
    private final Map<Astra, Double> elements = new HashMap<>();
    /**
     * Набор карт (одна или несколько), к которым (или которой) принадлежат астры паттерна.
     */
    private final List<Chart> heavens = new ArrayList<>();
    /**
     * Сумма эффективных орбисов астр в паттерне.
     */
    private double totalClearance = 0.0;

//    private final List<Aspect> aspects = new ArrayList<>();

    /**
     * Задаёт новый паттерн резонансов по указанной гармонике,
     * рассчитываемый на базе указанной матрицы.
     *
     * @param harmonic указанная гармоника.
     * @param host     указанная {@link AstroMatrix АстроМатрица}.
     */
    public Pattern(int harmonic, AstroMatrix host) {
        this.harmonic = harmonic;
        this.analysis = host;
    }

    /**
     * Задаёт новый паттерн резонансов по указанной гармонике,
     * рассчитываемый на базе указанной матрицы. В него сразу
     * добавляются астры из предоставленного списка.
     *
     * @param harmonic указанная гармоника.
     * @param astras   предложенный список астр.
     * @param host     указанная {@link AstroMatrix АстроМатрица}.
     */
    public Pattern(int harmonic, List<Astra> astras, AstroMatrix host) {
        this(harmonic, host);
        astras.forEach(this::addAstra);
    }

    /**
     * Добавляет астру к паттерну, обновляя сумматоры орбисов:
     * общий для паттерна и для каждого элемента, включая добавляемый.
     * Если астра с таким именем из той же карты уже есть в паттерне,
     * она игнорируется и повторно не добавляется.
     *
     * @param astra добавляемая к паттерну астра.
     */
    public void addAstra(Astra astra) {
        for (Astra a : elements.keySet())
            if (a.isTheSame(astra)) return;
        if (!heavens.contains(astra.getHeaven()))
            heavens.add(astra.getHeaven());
        double clearanceSum = 0.0;
        for (Astra a : elements.keySet()) {
            double clearance = getArcForHarmonic(astra, a, harmonic);
            clearanceSum += clearance;
            elements.forEach((key, value) ->
                    elements.put(key, value + clearance));
            totalClearance += clearance;
        }
        elements.put(astra, clearanceSum);
    }

    /**
     * Выдаёт список астр, входящих в паттерн.
     * Список сортирован по убыванию средней силы номинального аспекта астры
     * к другим астрам паттерна, соответствующей возрастанию суммы зазоров
     * этой астры с остальными астрами паттерна.
     *
     * @return список астр в паттерне, сортированный по возрастанию силы связанности.
     */
    public List<Astra> getAstrasByConnectivity() {
        return elements.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Выдаёт многострочное представление паттерна.
     * Первая строка докладывает среднюю силу связанности паттерна и кол-во астр.
     * Каждая следующая строка (смещённая вправо табуляцией) содержит
     * символ астры и (в скобках) среднюю силу её связанности с другими элементами паттерна.
     * Если паттерн включает астры из более чем одной карты, вслед
     * за именем и координатой астры отображается также краткое имя владельца.
     * Если паттерн содержит только одну астру (такой паттерн не считается
     * валидным, и такой случай не должен возникать), представление имеет вид
     * {@code "{символ} (-)"}.
     *
     * @return многостроку с представлением паттерна.
     */
    public String getConnectivityReport() {
        return size() == 1 ? "%s (-)%n".formatted(getString()) :
                "\t%.0f%% (%d):%n".formatted(getAverageStrength(), size())
                        +
                        getAstrasByConnectivity().stream()
                                .map(astra -> "\t\t%s%s (%.0f%%)%n"
                                        .formatted(astra.getSymbolWithDegree(),
                                                getDimension() > 1 ? "<%s>".formatted(astra.getHeaven().getShortenedName(3)) : "",
                                                calculateStrength(defineOrb(), elements.get(astra) / (size() - 1))))
                                .collect(Collectors.joining());
    }

    // TODO: сделать (скорее всего в МногоКарте или в Астро Матрице)
    //  умное сокращение названий карт

    /**
     * @return количество астр в паттерне.
     */
    public int size() {
        return elements.size();
    }

    /**
     * Сообщает размерность, т.е. скольки картам принадлежат
     * входящие в паттерн астры.
     *
     * @return количество карт, к которым относятся элементы паттерна.
     */
    public int getDimension() {
        return heavens.size();
    }

    /**
     * Сообщает, не пуст ли паттерн.
     *
     * @return {@code true}, если в паттерне нет ни одной астры. И наоборот.
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Добавляет к паттерну все астры из другого паттерна.
     *
     * @param pattern другой паттерн, астры из которого добавляются.
     */
    public void addAllAstras(Pattern pattern) {
        pattern.getElements().keySet().forEach(this::addAstra);
    }

    /**
     * Сообщает среднюю силу аспектов между астрами паттерна.
     * Если это моногармонический паттерн, предполагается, что между
     * всеми астрами в нём равные или кратные аспекты с общим орбисом.
     * Метод вычисляет среднюю силу всех аспектов между всеми астрами.
     *
     * @return условную силу паттерна от -100 до 100, согласно конвенции
     * {@link CelestialMechanics#calculateStrength(double, double) calculateStrength()}
     */
    public double getAverageStrength() {
        return size() < 2 ?
                0.0 :
                calculateStrength(defineOrb(), totalClearance / possiblePairs());
    }

    /**
     * Инструментальный метод, определяющий орб, используемый для
     * суждения о силе аспектов-резонансов.
     *
     * @return стандартный первичный орбис из настроек, ополовиненный,
     * если содержит астры разных небес
     * в случае соответствующей настройки для двойных карт.
     */
    private double defineOrb() {
        return getDimension() > 1 && Settings.isHalfOrbsForDoubles() ?
                Settings.getPrimalOrb() / 2 :
                Settings.getPrimalOrb();
    }

    /**
     * Текстовая репрезентация паттерна.
     *
     * @return строку, состоящую из символов входящих в паттерн астр,
     * упорядоченных по убыванию средней связанности.
     */
    public String getString() {
        return elements.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(Astra::getSymbol)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    /**
     * Предикат, удостоверяющий, что в группе астр наличествует
     * номинальный аспект в явном виде для хотя бы одной пары.
     *
     * @return {@code false}, если паттерн пуст или содержит только
     * одну астру, или если ни в одной из пар элементов нет номинального резонанса.
     * {@code true}, если хотя бы в одной паре номинальный резонанс
     * наличествует.
     */
    public boolean isValid() {

        List<Astra> astras = elements.keySet().stream().toList();

        return IntStream.range(0, astras.size() - 1)
                .anyMatch(i -> IntStream.range(i + 1, astras.size())
                        .anyMatch(j ->
                                analysis.inResonance(astras.get(i), astras.get(j), harmonic)));
    }

    private int possiblePairs() {
        return size() * (size() - 1) / 2;
    }

    public Set<Chart> checkHeavens() {
        return new HashSet<>(heavens);
    }


}
