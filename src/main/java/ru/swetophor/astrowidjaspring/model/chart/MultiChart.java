package ru.swetophor.astrowidjaspring.model.chart;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public non-sealed class MultiChart extends ChartObject {

    private final Chart[] moments;

    public MultiChart(String name, Chart... moments) {
        super(name);
        this.moments = moments;
    }

    public MultiChart(String name, ChartObject... charts) {
        this(name, Arrays.stream(charts)
                .map(ChartObject::getData)
                .flatMap(Arrays::stream)
                .toArray(Chart[]::new));
    }

    public MultiChart(ChartObject... charts) {
        this("", charts);
        String autoTitle = Arrays.stream(moments)
                .map(Chart::getName)
                .collect(Collectors.joining(" + ", "Синастрия: ", ""));
        setName(autoTitle);
    }

    @Override
    public Chart[] getData() {
        return moments;
    }

    @Override
    public String getString() {
        return Arrays.stream(moments)
                .map(Chart::getString)
                .collect(Collectors.joining("",
                        "//%s:".formatted(name),
                        ""));
    }

    @Override
    public String getAstrasList() {
        return Arrays.stream(moments)
                .map(Chart::getAstrasList)
                .collect(Collectors.joining("",
                        "//%s%n:".formatted(name),
                        ""));
    }

    @Override
    public int getDimension() {
        return moments.length;
    }

    @Override
    public String getCaption() {
        return IntStream.range(0, moments.length)
                .mapToObj(i -> "%s: %s"
                        .formatted(letterFor(i), moments[i].getName()))
                .collect(Collectors.joining("\n",
                        "Множественная карта:\n",
                        ""));
    }

    private static String letterFor(int i) {
        String letterNotation = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЫЭЮЯ";
        int number = i % letterNotation.length();
        int octave = i / letterNotation.length();
        return "%s%s".formatted(
                letterNotation.charAt(number),
                octave > 0 ? String.valueOf(octave) : "");
    }
}
