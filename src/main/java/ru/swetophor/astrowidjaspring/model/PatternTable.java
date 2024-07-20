package ru.swetophor.astrowidjaspring.model;

import ru.swetophor.astrowidjaspring.model.chart.Chart;
import ru.swetophor.astrowidjaspring.utils.Decorator;

import java.util.*;

import static java.util.stream.Collectors.joining;

/**
 * Удобное представление результата анализа Карт-Объекта, одинарного или группового.
 * Таблица Узоров содержит {@link PatternAnalysis Узор-Разборы (Анализы Паттернов)}
 * по одному для каждого возможного межкартного анализа.
 * Так для одинарной карты имеется всего один Узор-Разбор: таблица аспектов между планетами
 * в этой карте. Для двойной карты таких разборов три: для первой карты, для второй
 * карты и для аспектов между одной и второй картой.
 * Архитектура объекта допускает работу в карт-объекте с любым количеством исходных карт.
 * Например, для трёх карт {А, Б, В} будут представлены такие разборы:
 * {А}, {Б}, {В}, {АБ}, {АВ}, {БВ}, {АБВ} — т.е. сначала одинарные разборы паттернов
 * для карт по отдельности, потом три варианта двойного взаимодействия, а также таблица
 * паттернов, в которые входит по крайней мере одна планета от каждой из трёх карт.
 * Если какого-то типа паттерна не будет найдено, соответствующий узор-разбор будет
 * пустым. Общее количество узор-разборов для N карт равно {@code 2^N - 1}. Порядок
 * появления разборов идёт по возрастанию мерности и в общем в соответствии с порядком
 * карт в карт-объекте.
 */
public class PatternTable {
    private final Chart[] heavens;
    private final Map<List<Chart>, PatternAnalysis> tables = new LinkedHashMap<>();

    /**
     * Строит {@link PatternTable Таблицу Узоров} для представления списков паттернов,
     * найденных по {@link AstroMatrix АстроМатрице}. Ключами становятся все возможные сочетания
     * исходных карт, а значениями ставятся объекты {@link PatternAnalysis Анализов Паттернов},
     * построенные для каждого особенного сочетания карт в многокарте.
     * Таким образом, если Матрица построена по всего лишь
     * одной карте, в сопоставлении будет только одна запись.
     * Если {@code sourceCharts} содержит карты {@code {А,Б}},
     * ключи будут {@code {А,Б,АБ}}, и т.д.
     * @param matrix  АстроМатрица для построения таблицы паттернов.
     */
    public PatternTable(AstroMatrix matrix) {
        heavens = matrix.getHeavens();
        matrix.heavenCombinations(false).forEach(combination ->
                tables.put(combination, matrix.getPatternAnalysis(combination)));
    }

    /**
     * Выдаёт текстовую репрезентацию найденных гармонических паттернов для
     * астр карты или карт, по которым построена Астроматрица.
     * @param detailed  выводить ли подробную статистику по астрам паттернов.
     * @return  заголовок общего Анализа, затем таблицы паттернов для всех комбинаций
     *  отдельных карт: сначала для каждой карты в отдельности, затем для каждого
     *  возможного их сочетания. Если это анализ по многокарте, каждая таблица
     *  анализа предваряется также заголовком.
     */
    public String getPatternReport(boolean detailed) {
        String title = detailed ?
                "Подробный анализ паттернов для: " :
                "Анализ паттернов для: ";
        StringBuilder sb = new StringBuilder(
                Decorator.doubleFrame(title +
                        Arrays.stream(heavens)
                            .map(Chart::getName)
                            .collect(joining(" и "))
                ));
        for (List<Chart> combination : tables.keySet()) {
            if (heavens.length > 1)
                sb.append(Decorator.asteriskFrame(
                        combination.stream().map(Chart::getName)
                                .collect(joining(" и ", "Таблица паттернов для ", ":"))));
            PatternAnalysis patterns = tables.get(combination);
            sb.append(detailed ?
                    patterns.getFullAnalysisRepresentation() :
                    patterns.getShortAnalysisRepresentation()
            );
        }
        return sb.toString();
    }
    /* TODO: требуется сделать настраиваемый отчёт по паттернам:
         отсечки по номеру гармоники и/или силе, виды сортировки
         а также — !! — нахождение узоров не только по одному числу,
         т.е. связанные сложным резонансом группы астр (могут быть сильными) */


    /* TODO: заголовок синастрий должен объявлять карты как А и Б (и т.д.)
        и именно таким индексом маркировать астры по ходу отчёта */
}
