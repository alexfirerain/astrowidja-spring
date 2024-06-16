package ru.swetophor.astrowidjaspringshell.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.swetophor.astrowidjaspringshell.client.UserController;
import ru.swetophor.astrowidjaspringshell.model.ChartList;
import ru.swetophor.astrowidjaspringshell.model.ChartObject;
import ru.swetophor.astrowidjaspringshell.repository.ChartRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class LibraryService {

    /**
     * Реализация картохранилища, хранящего список списков карт.
     */
    private final ChartRepository chartRepository;

    private final UserController userController;
    /**
     * Имена актуальных списков (групп) карт.
     */
    private final List<String> groupNames = new ArrayList<>();
    /**
     * Отображение в памяти базы данных карт, группированных по спискам.
     */
    private final List<List<String>> chartCatalogue = new ArrayList<>();

    /**
     * Очищает отображение структуры библиотеки в памяти (имена альбомов
     * и списки имён карт) и заново записывает его на основании данных от {@link #chartRepository картохранилища}.
     * Процедура должна выполняться при инициализации сервиса и
     * в конце всякого модифицирующего обращения к картохранилщу.
     */
    public void rereadLibrary() {
        groupNames.clear();
        groupNames.addAll(chartRepository.albumNames());
        chartCatalogue.clear();
        chartCatalogue.addAll(groupNames.stream()
                .map(chartRepository::getAlbumContents)
                .toList());
    }

    @PostConstruct
    public void buildChartIndex() {
        rereadLibrary();
    }

    /**
     * Выдаёт строковое представление групп карт в библиотеке.
     *
     * @return нумерованный (с 1) список групп.
     */
    public String listLibrary() {
        return IntStream.range(0, groupNames.size())
                .mapToObj(i -> "%d. %s%n"
                        .formatted(i + 1, groupNames.get(i)))
                .collect(Collectors.joining());
    }

    /**
     * Выдаёт строковое представление содержимого библиотеки
     * (как оно отображается в памяти).
     *
     * @return нумерованный (с 1) список групп, вслед каждой группе -
     * нумерованный (с 1) список карт в ней.
     */
    public String libraryListing() {
        StringBuilder output = new StringBuilder();
        IntStream.range(0, groupNames.size())
                .forEach(g -> {
                    output.append("%d. %s:%n"
                            .formatted(g + 1, groupNames.get(g)));
                    var list = chartCatalogue.get(g);
                    IntStream.range(0, list.size())
                            .mapToObj(i -> "\t%d. %s%n"
                                    .formatted(i + 1, list.get(i)))
                            .forEach(output::append);
                });
        return output.toString();
    }

    /**
     * Находит в базе (через её отображение в памяти) альбом карт по
     * его названию или текущему номеру в списке, как он непосредственно
     * перед этим отображался соответствующими функциями.
     *
     * @param chartListOrder строка ввода, как номер, название
     *                       или первые символы названия.
     * @return список карт с указанным номером или названием.
     * @throws IllegalArgumentException если по вводу не опознан список.
     */
    public ChartList findList(String chartListOrder) {
        int groupIndex;
        try {
            groupIndex = defineIndexFromInput(chartListOrder, groupNames);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Списка не найдено: " + e);
        }
        return chartRepository.getAlbumSubstance(groupNames.get(groupIndex));
    }

    /**
     * Определяет индекс (от 0) строкового элемента в списке,
     * указанного через номер (от 1), имя или первые буквы имени.
     * Сначала проверяет, нет ли в списке элемента с тождественным запросу названием;
     * затем, если запрос состоит только из цифр, — нет ли в списке элемента с
     * таким номером (при нумерации с 1);
     * затем — нет ли элемента, начинающегося с тех символов, из которых состоит запрос.
     * Выдаётся первое найденное совпадение.
     *
     * @param input строка, анализируемая как источник индекса.
     * @param list  список строк, индекс в котором ищется.
     * @return индекс элемента в списке, если ввод содержит корректный номер или
     * присутствующий элемент.
     * @throws IllegalArgumentException если ни по номеру, ни по имени элемента
     *                                  не найдено в списке, а также если аргументы пусты.
     */
    private int defineIndexFromInput(String input, List<String> list) {
        if (input == null || input.isBlank())
            throw new IllegalArgumentException("Элемент не указан.");
        if (list == null || list.isEmpty())
            throw new IllegalArgumentException("Список не указан.");

        for (int i = 0; i < list.size(); i++)
            if (input.equals(list.get(i)))
                return i;

        if (input.matches("^[0-9]+$")) {
            int index;
            try {
                index = Integer.parseInt(input) - 1;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Ошибка распознавания числа.");
            }
            if (index < 0 || index >= list.size())
                throw new IllegalArgumentException("Элемент %d отсутствует: всего %d элементов."
                        .formatted(index + 1, list.size()));
            return index;
        }

        for (int i = 0; i < list.size(); i++)
            if (list.get(i).startsWith(input))
                return i;

        throw new IllegalArgumentException("Не обнаружено элементов, опознанных по вводу \"%s\""
                                    .formatted(input));
    }

    /**
     * Добавляет карты из указанного альбома к указанному картосписку.
     * @param loadFile  имя файла или группы, карты откуда добавляются.
     * @param desk  картосписок, к которому добавляются карты.
     * @return  сообщение, что карты из альбома загружены.
     */
    public String loadAlbum(String loadFile, ChartList desk) {
        chartRepository.getAlbumSubstance(loadFile)
                .forEach(c -> userController.mergeChartIntoList(desk, c, "на столе"));
        return "Загружены карты из " + loadFile;
    }

    /**
     * Распоряжается репозиторию сохранить указанный список карт
     * как новую группу (в новый файл), название которого автоматическое
     * с текущей датой.
     * @param desk автосохраняемый список
     * @return
     */
    public String autosave(ChartList desk) {
        return chartRepository.addChartsToAlbum(desk, ChartRepository.newAutosaveName());
    }

    public String deleteAlbum(String filename) {
        return chartRepository.deleteAlbum(filename);
    }

    public void saveChartsAsAlbum(ChartList charts, String filename) {
        chartRepository.saveChartsAsAlbum(charts, filename);
    }

    public String addChartListToAlbum(ChartList content, String filename) {
        return chartRepository.addChartsToAlbum(content, filename);
    }

    public void addChartsToAlbum(String filename, ChartObject... charts) {
        chartRepository.addChartsToAlbum(filename, charts);
    }
}
