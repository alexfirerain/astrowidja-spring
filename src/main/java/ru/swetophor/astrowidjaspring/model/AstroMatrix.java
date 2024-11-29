package ru.swetophor.astrowidjaspring.model;

import lombok.Getter;
import ru.swetophor.astrowidjaspring.config.Settings;
import ru.swetophor.astrowidjaspring.model.astro.Astra;
import ru.swetophor.astrowidjaspring.model.chart.Chart;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.IntStream.range;

@Getter
public class AstroMatrix {
    /**
     * Карты, на основе которых рассчитана Матрица,
     * в том порядке, как переданы аргументы в конструктор.
     */
    private final Chart[] heavens;
    /**
     * Объединённый массив астр из всех карт. Соответствующие картам блоки
     * следуют в порядке, как в {@link #heavens}, в каждом блоке астры
     * следуют в порядке, возвращённом {@link Chart#getAstras() getAstras()}.
      */
    private final List<Astra> allAstras;
    /**
     * Мапа в ОЗУ для быстрого получения номера астры в массиве {@link #allAstras}.
     * Реально ли это быстрее, чем {@code indexOf()}, не факт, но всё же.
     */
    private final Map<Chart, Map<Astra, Integer>> index;
    /**
     * Треугольный двумерный массив, отражающий все возможные парные отношения
     * между всеми астрами Матрицы. Если общее количество астр N,
     * то длина первого ряда равна N - 1, каждого последующего — на один меньше.
     */
    private final ResonanceBatch[][] matrix;

    /**
     * Создание матрицы резонансов для некоторого количества
     * астрологических карт.
     * При этом для любой пары астр простраиваются пучки резонансов
     * до гармоники, указанной в {@link Settings#getEdgeHarmonic()}.
     * @param charts карты, предоставляющие наборы астр для анализа.
     */
    public AstroMatrix(Chart... charts) {
        // фиксация массива карт
        heavens = charts;

        System.out.println("Строим новую АстроМатрицу для [" +
                Arrays.stream(heavens).map(Chart::getName)
                        .collect(Collectors.joining(", ")) + "]"); // monitor

        // создание общего массива астр
        allAstras = Arrays.stream(heavens)
                .flatMap(c -> c.getAstras().stream())
                .toList();

        long before = System.nanoTime();    // monitor
        // построение индекса астр
        index = new HashMap<>();
        int counter = 0;
        for (Chart chart : heavens) {
            index.put(chart, new HashMap<>());
            for (Astra astra : chart.getAstras())
                index.get(chart).put(astra, counter++);
        }
        System.out.println("Индекс построен за " + ((double) (System.nanoTime() - before) / 1000.0) + " мс."); // monitor

        // построение матрицы резонансов
        matrix = new ResonanceBatch[allAstras.size()][allAstras.size()];
        for (int i = 0; i < allAstras.size() - 1; i++)
            for (int j = i + 1; j < allAstras.size(); j++)
                matrix[i][j] = new ResonanceBatch(allAstras.get(i), allAstras.get(j));
    }

    /**
     * Внутренний метод определения номера астры в {@link #allAstras}
     * с помощью {@link #index}
     * @param astra ссылка на астру.
     * @return  номер указанной астры в объединённом массиве. Если указанной астры
     * нет ни в одной карте, то -1.
     */
    private int astraIndex(Astra astra) {
        Integer i;
        try {
            i = index.get(astra.getHeaven()).get(astra);
        } catch (NullPointerException e) {
            return -1;
        }
        return i != null ? i : -1;
    }

    /**
     * Выдаёт рассчитанный для пары астр резонанс.
     * @param a первая астра резонанса.
     * @param b вторая астра резонанса.
     * @return  объект резонанса, рассчитанный в Матрице для двух указанных астр.
     * @throws IllegalArgumentException если вдруг хотя бы одна из астр не найдена
     *          или астры идентичны между собой.
     */
    public ResonanceBatch getResonanceFor(Astra a, Astra b) {
        if (a == b) throw new IllegalArgumentException("Астра не делает резонанса сама с собой");
        int iA = astraIndex(a), iB = astraIndex(b);
        if (iA == -1 || iB == -1) throw new IllegalArgumentException("Астра %s не найдена"
                .formatted(iA == -1 ?
                                iB == -1 ?
                                "%s и %s".formatted(a.getSymbolWithOwner(), b.getSymbolWithOwner()) :
                                "%s".formatted(a.getSymbolWithOwner()) :
                            "%s".formatted(b.getSymbolWithOwner())));
        return iA < iB ?
                matrix[iA][iB] :
                matrix[iB][iA];
    }

    /**
     * Сообщает, что между этими астрами присутствует
     * номинальный резонанс по указанной гармонике, как это сообщается
     * {@link ResonanceBatch#hasExactHarmonic(int) hasExactHarmonic()}.
     * @param a первая проверяемая астра.
     * @param b вторая проверяемая астра.
     * @param harmonic  гармоника, явная связь по которой проверяется.
     * @return  {@code true}, если между указанными астрами существует
     * явный резонанс по указанной гармонике, в противном случае {@code false}.
     */
    public boolean inResonance(Astra a, Astra b, int harmonic) {
        return getResonanceFor(a,b).hasExactHarmonic(harmonic);
    }

    /**
     * Выдаёт список {@link ResonanceBatch Резонансов}, которые указанная астра
     * делает со всеми остальными астрами в Матрице (из одной или разных карт).
     * @param a астра, резонансы которой интересуют.
     * @return  список резонансов указанной астры с каждой из остальных астр Матрицы.
     */
    public List<ResonanceBatch> resonancesFor(Astra a, boolean[] actualAstrasMask) {
        List<ResonanceBatch> list = new ArrayList<>();
        int index = astraIndex(a);
        int i = 0, j = index;
        boolean turning = false;
        while (i + j < allAstras.size() + index - 1) {
            if (turning) j++;
            if (i == j) {
                turning = true;
                continue;
            }
            if (actualAstrasMask[i] && actualAstrasMask[j]) {
                list.add(matrix[i][j]);
            }
            if (!turning) i++;
        }
        return list;
    }

    /**
     * Выплёскивает поток существующих в АстроМатрице резонансов,
     * проход матрицы осуществляем "косынкой": [0][1]→[0][2]→[0][3]→[1][2]→[1][3]→[2][3].
     * Т.е. по резонансу для всех возможных пар между астрами анализируемой карты или карт.
     * @return  поток объектов-резонансов, начиная с первой планеты первой карты.
     */
    public Stream<ResonanceBatch> stream() {
        return IntStream.range(0, allAstras.size() - 1)
                .mapToObj(i -> Arrays.asList(matrix[i])
                        .subList(i + 1, allAstras.size()))
                .flatMap(Collection::stream);
    }

    /**
     * Возвращает в виде списка содержимое потока {@link AstroMatrix#stream()}.
     * @return  список всех резонансов между всеми астрами АстроМатрицы
     * в порядке, предоставляемом методом {@link #stream()}.
     */
    public List<ResonanceBatch> getAllResonances() {
        return stream().collect(Collectors.toCollection(ArrayList::new));
    }

    public List<ResonanceBatch> getResonancesFor(Chart chart) {
        return stream()
                .filter(ResonanceBatch::isNonSynastric)
                .filter(r -> r.getHeavens().contains(chart))
                .toList();
    }

    public List<ResonanceBatch> getResonancesFor(Chart chart1, Chart chart2) {
        return stream()
                .filter(ResonanceBatch::isSynastric)
                .filter(r -> r.getHeavens().containsAll(Set.of(chart1, chart2)))
                .toList();
    }

    /**
     * Находит и возвращает список всех паттернов, образованных астрами
     * данной карты или карт по указанной гармонике.
     *
     * @param harmonic гармоника, по которой выделяются паттерны.
     * @param activeCharts из каких карт следует рассматривать астры.
     * @return список паттернов из астр этой карты или карт, резонирующих
     * по указанной гармонике, сортированный по средней силе.
     * Если ни одного паттерна не обнаруживается, то пустой список.
     */
    public List<Pattern> findPatterns(int harmonic, List<Chart> activeCharts) {
        boolean[] acceptable = getAcceptanceMask(activeCharts);
        boolean[] analyzed = new boolean[allAstras.size()];
        return range(0, allAstras.size())
                .filter(i -> acceptable[i] && !analyzed[i])
                .mapToObj(i -> gatherResonants(i, harmonic, analyzed, acceptable))
                .filter(Pattern::isValid)
                .sorted(Comparator.comparingDouble(Pattern::getAverageStrength).reversed())
                .toList();
    }

    /**
     * Создаёт битовую маску на список всех астр, отмечающую,
     * какие из них должны рассматриваться в некотором анализе.
     * @param charts карты, астры которых должны быть отмечены.
     * @return  булев массив, где {@code true} означает, что астра
     * в таким номером должна рассматриваться, а {@code false} — что нет.
     */
    private boolean[] getAcceptanceMask(List<Chart> charts) {
        boolean[] mask = new boolean[allAstras.size()];
        range(0, allAstras.size())
                .filter(i -> charts.contains(allAstras.get(i).getHeaven()))
                .forEach(i -> mask[i] = true);
        return mask;
    }

    /**
     * Выдаёт паттерн, состоящий из астр данной карты, связанных с указанной астрой
     * по указанной гармонике напрямую или посредством других астр.
     * Исходная астра помещается сразу в паттерн, её номер отмечается как
     * уже проверенный во вспомогательном массиве; затем функция рекурсивно
     * запускается для каждой из ещё не проверенных астр, имеющих указанный
     * резонанс с исходной, результат каждого вызова добавляется к паттерну.
     *
     * @param astraIndex индекс исходной астры в списке астр этой Карты.
     * @param harmonic   номер гармоники, по которому надо проверить узор.
     * @param acceptable    битовая маска, выбирающая те астры, по которым
     *                      надо проводить анализ.
     * @param analyzed   битовая маска, отмечающая, какие астры
     *                   из списка астр этой Карты уже проверены на этот резонанс.
     * @return паттерн, содержащий исходную астру и все связанные с ней
     * по указанной гармонике астры из списка астр этой карты; паттерн,
     * содержащий одну исходную астру, если резонансов по этой гармонике нет.
     */
    private Pattern gatherResonants(int astraIndex, int harmonic, boolean[] analyzed, boolean[] acceptable) {
        Astra startingAstra = allAstras.get(astraIndex);
        analyzed[astraIndex] = true;
        Pattern currentPattern = new Pattern(harmonic, this);
        currentPattern.addAstra(startingAstra);
        getConnectedAstras(startingAstra, harmonic, acceptable).stream()
                .filter(a -> !analyzed[allAstras.indexOf(a)])
                .map(a -> gatherResonants(allAstras.indexOf(a), harmonic, analyzed, acceptable))
                .forEach(currentPattern::addAllAstras);
        return currentPattern;
    }

    /**
     * Выдаёт список астр, находящихся в резонансе с данной по указанной гармонике.
     * Рассматриваются все астры из всех карт, по которым построена Матрица:
     * остальные астры той же карты и каждая астра из всех остальных карт.
     *
     * @param astra      астра, резонансы с которой ищутся.
     * @param harmonic   число, по которому определяется резонанс.
     * @param acceptable    маска, какие астры следует рассматривать.
     * @return список астр, находящих в резонансе с данной по указанной гармонике.
     * Орбис для аспектов между астрами из разных карт учитывается соответственно
     * глобально определённым правилам.
     */
    public List<Astra> getConnectedAstras(Astra astra, int harmonic, boolean[] acceptable) {
        return resonancesFor(astra, acceptable)
                .stream().filter(r -> r.hasHarmonicResonance(harmonic))
                .map(r -> r.getCounterpart(astra))
                .collect(Collectors.toList());
    }

    /**
     * Строит и выдаёт список, содержащий все возможные сочетания карт,
     * между которыми простраиваются таблицы резонансов.
     * Если флажок {@code forAspects} установлен, передаются комбинации только
     * из одной или двух карт, если нет, то все комбинации вплоть до
     * варианта, включающего все карты сразу.
     * Сокращённый список будет иметь длину {@code N*(N+1)/2}, полный — {@code 2^N-1}.
     * @param forAspects ограничивать ли список только сочетанием одной-двух карт,
     *                   как требуется для передачи аспектов.
     * @return если матрица строится для одиночной карты, выдаёт список
     * из одной этой карты; если {@link #heavens} содержит {@code {А,Б}},
     * выдаёт список {@code {А,Б,АБ}} и т.д. Для списка {@code {А,Б,В}} вариант
     * для аспектов выдаст {@code {А,Б,В,АБ,АВ,БВ}}, вариант для паттернов —
     * {@code {А,Б,В,АБ,АВ,БВ,АБВ}} и т.д.
     */
    public List<List<Chart>> heavenCombinations(boolean forAspects) {
        List<List<Chart>> combinations = new ArrayList<>();

        for (int i = 1; i < Math.pow(2, heavens.length); i++) {
            List<Chart> nextCombination = new ArrayList<>();
            int cypher = i;
            int n = 0;
            while (cypher > 0) {
                if ((cypher % 2 == 1) &&
                        (!forAspects || nextCombination.size() < 2))
                    nextCombination.add(heavens[n]);
                cypher /= 2;
                n++;
            }
            combinations.add(nextCombination);
        }
        combinations.sort(Comparator.comparingInt(List::size));
        return combinations;
    }

    /**
     * Выдаёт полный гармонический анализ резонансов по карте
     * или картам, соответствующим этой АстроМатрице.
     * @return {@link PatternTable},аккуратно описывающий все
     * паттерны резонансов, найденные по этой Матрице.
     */
    public PatternTable buildPatternTable() {
        return new PatternTable(this);
    }


    public AspectTable buildAspectTable() {
        return new AspectTable(this);
    }


    public PatternAnalysis getPatternAnalysis(List<Chart> charts) {
        PatternAnalysis anal = new PatternAnalysis();

        IntStream.rangeClosed(1, Settings.getEdgeHarmonic())
                .forEach(i -> findPatterns(i, charts).stream()
                        .filter(pat -> pat.ofHeavenSet(charts))
                        .forEach(anal::addPattern));

        return anal;
    }
}
