package ru.swetophor.astrowidjaspringshell.model;

import ru.swetophor.astrowidjaspringshell.utils.Decorator;

import java.util.*;

import static java.util.stream.Collectors.joining;

public class AspectTable {
    private final Chart[] heavens;
    private final Map<List<Chart>, List<ResonanceBatch>> tables = new LinkedHashMap<>();

    public AspectTable(AstroMatrix matrix) {
        heavens = matrix.getHeavens();
        matrix.heavenCombinations(true)
                .forEach(combination -> tables.put(combination, new ArrayList<>()));
        matrix.stream().forEach(this::addResonance);
    }

    public void addResonance(ResonanceBatch resonance) {
        tables.keySet().stream()
                .filter(scope -> new HashSet<>(scope).equals(resonance.getHeavens()))
                .findFirst()
                .ifPresentOrElse(scope -> tables.get(scope).add(resonance),
                        () -> { throw new IllegalArgumentException("Добавление резонанса не в ту таблицу"); });
    }

    public String getAspectReport() {
        StringBuilder sb = new StringBuilder(
                Decorator.doubleFrame("Анализ резонансов для: "
                        + Arrays.stream(heavens).map(Chart::getName).collect(joining(" и "))
                ));
        for (List<Chart> combination : tables.keySet()) {
            if (heavens.length > 1)
                sb.append(Decorator.asteriskFrame(
                        combination.stream().map(Chart::getName)
                                .collect(joining(" и ", "Аспекты для ", ":"))));
            tables.get(combination).stream()
                    .map(ResonanceBatch::resonancesOutput)
                    .forEach(sb::append);
        }
        return sb.toString();
    }

}
