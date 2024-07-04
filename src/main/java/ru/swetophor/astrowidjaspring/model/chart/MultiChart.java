package ru.swetophor.astrowidjaspring.model.chart;

import ru.swetophor.astrowidjaspring.utils.Decorator;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.swetophor.astrowidjaspring.utils.Decorator.singularFrame;

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
    public String getAstrasList() {
        String[] astrasLists = Arrays.stream(moments)
                .map(chart -> Decorator.singularFrame(chart.getAstrasList()))
                .toArray(String[]::new);
        return Decorator.concatenateTables(astrasLists);
    }

    /**
     * Выдаёт строку в том формате, как данные о
     * карте сохраняются в DAW-файл.
     * Для многокарты это описание из одной строки формата
     * {@code "<название: #карта1 #карта2...>"}, содержащей ссылки на привходящие карты.
     * @return строку, фиксирующую данные многокарты для сохранения в файле.
     * @apiNote одиночные карты, входящие в многокарту, должны
     *      быть определены в том же файле, это контролируется там, где картосписок
     *      сохраняется.
     */
    @Override
    public String toStoringString() {
        return Arrays.stream(moments)
                .map(Chart::getName)
                .collect(Collectors.joining(" #",
                        "<%s: #".formatted(name),
                        ">\n"));
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
                        name + "\n",
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
