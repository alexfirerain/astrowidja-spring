package ru.swetophor.astrowidjaspring.model;

import lombok.Getter;
import ru.swetophor.astrowidjaspring.config.Settings;
import ru.swetophor.astrowidjaspring.model.astro.Astra;
import ru.swetophor.astrowidjaspring.model.astro.ZodiacPoint;
import ru.swetophor.astrowidjaspring.model.chart.Chart;
import ru.swetophor.astrowidjaspring.utils.CelestialMechanics;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.swetophor.astrowidjaspring.utils.CelestialMechanics.*;

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

    private final List<Cluster> clusters = new ArrayList<>();

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

        boolean inCluster = false;
        // для каждого существующего кластера: попадает ли в него астра
        for (Cluster next : clusters) {
            // если попадает, добавить её к нему и успокоиться
            if (next.inConjunction(astra)) {
                next.add(astra);
                inCluster = true;
                break;
            }
        }
        // если ни один кластер не захапал астру, добавляем её в новый
        if (!inCluster)
            clusters.add(new Cluster(astra, this));
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
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
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
        return size() == 1 ? "%s (-)%n".formatted(getJustString()) :
                "%s: %.0f%% (%d):%n\t".formatted(getClusteredLines(), getAverageStrength(), size())
                        +
                        getAstrasByConnectivity().stream()
                                .map(astra -> "%s%s :%.0f%%"
                                        .formatted(astra.getSymbolWithDegree(),
                                                getDimension() > 1 ? "<%s>".formatted(astra.getHeaven().getShortenedName(4)) : "",
                                                calculateStrength(defineOrb(), elements.get(astra) / (size() - 1))))
                                .collect(Collectors.joining(" / "));
    }

//    /**
//     * Выдаёт многострочное представление паттерна.
//     * Первая строка докладывает среднюю силу связанности паттерна и кол-во астр.
//     * Каждая следующая строка (смещённая вправо табуляцией) содержит
//     * символ астры и (в скобках) среднюю силу её связанности с другими элементами паттерна.
//     * Если паттерн включает астры из более чем одной карты, вслед
//     * за именем и координатой астры отображается также краткое имя владельца.
//     * Если паттерн содержит только одну астру (такой паттерн не считается
//     * валидным, и такой случай не должен возникать), представление имеет вид
//     * {@code "{символ} (-)"}.
//     *
//     * @return многостроку с представлением паттерна.
//     */
//    public String getConnectivityReport() {
//        return size() == 1 ? "%s (-)%n".formatted(getJustString()) :
//                "%s: %.0f%% (%d):%n\t".formatted(getClusteredString(), getAverageStrength(), size())
//                        +
//                        getAstrasByConnectivity().stream()
//                                .map(astra -> "%s%s :%.0f%%"
//                                        .formatted(astra.getSymbolWithDegree(),
//                                                getDimension() > 1 ? "<%s>".formatted(astra.getHeaven().getShortenedName(4)) : "",
//                                                calculateStrength(defineOrb(), elements.get(astra) / (size() - 1))))
//                                .collect(Collectors.joining(" / "));
//    }

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
        // TODO: использование сокращённого орба в синастрическом паттерне
        //  приводит к неправильной оценке силы аспектов между астрами одной карты
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
    public String getJustString() {
        return elements.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(Astra::getSymbol)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    /**
     * Выдаёт представляющую паттерн строку, в которой
     * астры, пребывающие в соединении в одной из карт,
     * выделены фигурными скобками.
     * @return  строковое представление паттерна с отмеченными
     * кластерами соединений.
     */
    public String getClusteredString() {
        return clusters.stream()
                .sorted(Comparator.comparing(Cluster::getAverageConnectivity))
                .map(Cluster::toString)
                .collect(Collectors.joining());
    }
    // TODO: исправить отображение кластеров в синастрических узорах

    /**
     * Строит представление паттерна, где астры каждого неба
     * вынесены в отдельную строку, при этом друг над другом
     * находятся те астры или кластеры, которые в реальном соединении.
     * @return  хитропостроенную строку, представляющую узор.
     */
    public String getClusteredLines() {
        // колонки виртуальной таблицы представления
        var columns = new ArrayList<List<Cluster>>();

        CelestialMechanics.arrangeAsChain(clusters);
        // какие кластеры уже размещены в квазитаблице
        boolean[] placed = new boolean[clusters.size()];
        // перебираем кластеры
        for (int i = 0; i < clusters.size(); i++) {
            // ещё не размещённые
            if (placed[i]) continue;

            // почнём новую колонку
            List<Cluster> column = new ArrayList<>();
            column.add(clusters.get(i));
            placed[i] = true;

            // смотрим, попадёт ли в ту же колонку ещё что-то
            for (int j = i + 1; j < getClusters().size(); j++) {
                Cluster further = clusters.get(j);

                for (Cluster alreadyPlaced : column) {
                    // если кластер в соединении
                    if (alreadyPlaced.conjunctingCluster(further)) {
                        // добавим в колонку
                        column.add(further);
                        placed[j] = true;
                        break;
                    }
                }
            }
            // очередная колонка определена
            columns.add(column);
        }

        // определим ширины квазистолбцов
        int[] widths = columns.stream()
                .mapToInt(column -> column.stream()
                        .mapToInt(cl -> cl.toString().length())
                        .max().orElse(0)
                ).toArray();

        // построим строки квазитаблицы
        var rows = new String[getDimension()];

        for (int i = 0; i < rows.length; i++) {
            Chart owner = heavens.get(i);
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < columns.size(); j++) {
                List<Cluster> col = columns.get(j);
                boolean clusterPresent = false;
                for (Cluster cl : col)
                    if (cl.getHeaven() == owner) {
                        String cell = cl.toString();
                        row.append(cell)
                                .append("-"
                                        .repeat(widths[j] - cell.length()));
                        clusterPresent = true;
                    }
                if (!clusterPresent)
                    row.append("-".repeat(widths[j]));
            }
            row.append("\n");
            rows[i] = row.toString();
        }

        return String.join("", rows);
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

    /**
     * Вспомогательная функция, определяющая, сколько
     * парных сочетаний возможно для входящих астр.
     * @return  число, равное {@code N * (N - 1) / 2},
     * где N — количество астр в узоре (размерность).
     */
    private int possiblePairs() {
        return size() * (size() - 1) / 2;
    }

    /**
     * Выдаёт список небес астр паттерна
     * в виде набора для быстрой проверки наличия.
     * @return список небес в виде набора.
     */
    public Set<Chart> checkHeavens() {
        return new HashSet<>(heavens);
    }

    /**
     * Удостоверяет, что астры паттерна принадлежат тому же набору
     * небес, с которым происходит сверка.
     * @param heavens   сверяемый набор небес.
     * @return  {@code ДА}, если астры паттерна принадлежат тому
     * же множеству небес, с каким сверяется.
     */
    public boolean ofHeavenSet(Collection<Chart> heavens) {
        return getDimension() == heavens.size() &&
                checkHeavens().containsAll(new HashSet<>(heavens));
    }


    /**
     * Группа астр, находящаяся в соединении в карте радикса.
     */
    private class Cluster implements ZodiacPoint {
        /**
         * К какому паттерну относится кластер.
         */
        Pattern pattern;
        /**
         * Астры, входящие в кластер, т.е. находящиеся в соединении.
         */
        Set<Astra> conjuncted = new HashSet<>();

        public Cluster(Astra astra, Pattern pattern) {
            this.pattern = pattern;
            add(astra);
        }

        public void add(Astra astra) {
            conjuncted.add(astra);
        }

        /**
         * Сообщает, в чьей карте у нас такой красивый кластер
         * @return
         */
        public Chart getHeaven() {
            return conjuncted.iterator().next().getHeaven();
        }


        private boolean inConjunction(Astra astra) {
            return conjuncted.stream()
                    .anyMatch(a -> CelestialMechanics.areConjuncted(a, astra));
        }

        private boolean conjunctingCluster(Cluster another) {
            return conjuncted.stream()
                    .anyMatch(a -> another.conjuncted.stream()
                            .anyMatch(b ->
                                    !Astra.ofSameHeaven(a, b) && CelestialMechanics.conjuncting(a, b)
                            )
                    );
        }

        @Override
        public String toString() {
            if (conjuncted.size() == 1)
                return String.valueOf(conjuncted.iterator().next().getSymbol());

            String line = astrasInCelestialOrder().stream()
                    .map(Astra::getSymbol)
                    .map(String::valueOf)
                    .collect(Collectors.joining());
            return pattern.getHarmonic() == 1 ?
                    line :
                    "{%s}".formatted(line);
        }

        public double getAverageConnectivity() {
            return conjuncted.stream()
                    .mapToDouble(a -> pattern.elements.get(a))
                    .sum()
                    / conjuncted.size();
        }

        /**
         * Выдаёт координату точки, являющейся центром тяжести
         * для кластера, т.е. среднюю между образующими кластер точками.
         * @return
         */
        @Override
        public double getZodiacPosition() {
            return calculateAvg(conjuncted.toArray(new ZodiacPoint[0]));
        }

        public List<Astra> astrasInCelestialOrder() {
            return CelestialMechanics
                    .getArrangedAsChain(conjuncted.stream().toList()).stream()
                    .map(z -> (Astra) z)
                    .toList();
        }
    }

}
