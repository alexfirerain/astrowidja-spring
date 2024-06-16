package ru.swetophor.astrowidjaspring.model.chart;

import lombok.Getter;
import ru.swetophor.astrowidjaspring.model.astro.Astra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ru.swetophor.astrowidjaspring.utils.Mechanics.zodiacFormat;

@Getter
public non-sealed class Chart extends ChartObject {

    private final List<Astra> astras = new ArrayList<>();



    public Chart(String name, List<Astra> astraList) {
        this(name);
        astraList.forEach(this::addAstra);
    }

    public Chart(String name) {
        super(name);
    }

    /**
     * Конструктор карты на основе строки ввода,
     * содержащей имя карты и, в следующих строках,
     * описание каждой астры в подобающем формате.
     *
     * @param input входная строка.
     * @return сформированную карту.
     */
    public static Chart readFromString(String input) {
        var lines = input.lines().toArray(String[]::new);
        if (lines.length == 0)
            throw new IllegalArgumentException("текст не содержит строк");

        var astras =
                Arrays.stream(lines, 1, lines.length)
                        .filter(line -> !line.isBlank()
                                && !line.startsWith("//"))
                        .map(Astra::readFromString)
                        .collect(Collectors.toList());
        return new Chart(lines[0], astras);
    }


    @Override
    public Chart[] getData() {
        return new Chart[]{ this };
    }

    /**
     * Выдаёт строку в том формате, как данные о
     * карте сохраняются в файл: в первой строке "#"
     * и название карты, затем в каждой строке
     * имя и координаты астры (как "градусы минуты секунды"),
     * а также ещё одна пустая строка в конце.
     * @return пригодное для сохранения строковое представление карты.
     */
    @Override
    public String getString() {
        return astras.stream()
                .map(Astra::getString)
                .collect(Collectors.joining("",
                        "#%s%n".formatted(name),
                        "\n"));
    }

    @Override
    public String getAstrasList() {
        StringBuilder list = new StringBuilder("%nЗодиакальные позиции (%s):%n".formatted(name));
        astras.forEach(next -> list.append(
                        "%15s\t %s%n".formatted(
                                next.getNameWithZodiacDegree(),
                                zodiacFormat(next.getZodiacPosition())
                        )
                )
        );
        return list.toString();
    }

    @Override
    public int getDimension() {
        return 1;
    }

    public void addAstra(Astra astra) {
        astra.setHeaven(this);
        for (int i = 0; i < astras.size(); i++)
            if (astras.get(i).getName().equals(astra.getName())) {
                astras.set(i, astra);
                return;
            }
        astras.add(astra);
    }

    public Astra getAstra(String name) {
        return astras.stream()
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

}
