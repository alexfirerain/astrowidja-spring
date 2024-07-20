package ru.swetophor.astrowidjaspring.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.swetophor.astrowidjaspring.client.UserController;
import ru.swetophor.astrowidjaspring.config.Environments;
import ru.swetophor.astrowidjaspring.model.AlbumInfo;
import ru.swetophor.astrowidjaspring.model.chart.ChartList;
import ru.swetophor.astrowidjaspring.model.chart.ChartObject;
import ru.swetophor.astrowidjaspring.repository.ChartRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class LibraryService {

    /**
     * Реализация картохранилища, хранящего список списков карт.
     */
    private final ChartRepository chartRepository;

    /**
     * Компонент, реализующий взаимодействие с пользователем.
     */
    private final UserController userController;

    /**
     * Отображение в памяти информации о базе карт:
     * список карточек альбомов, в каждой название альбома,
     * список карт в альбоме и дата последнего обновления альбома.
     */
    private final List<AlbumInfo> library = new ArrayList<>();

    /**
     * Обновляет отображение структуры библиотеки в памяти (имена альбомов
     * и списки имён карт) на основании данных от {@link #chartRepository картохранилища}.
     * Процедура должна выполняться при инициализации сервиса и
     * в конце всякого модифицирующего обращения к картохранилщу.
     * <p>
     * Если в рабочую папку были добавлены файлы данных со временем
     * изменения старше, чем самый новый файл, они будут прочитаны только
     * при следующем запуске АстроВидьи. Их обновление может быть подгружено
     * инструкцией {@link #reloadLibrary()}.
     */
    public void updateLibrary() {
        if (library.isEmpty()) {
            // библиотека это отражение репозитория
            reloadLibrary();
        } else {
            // файлы, обновлённые после последнего известного обновления
            var updates = chartRepository
                    .getLibraryUpdates(library.getFirst().modified());
            // файлы, остающиеся существующими и не обновлёнными
            var remainings = library.stream()
                    .filter(info -> chartRepository.albumNames().contains(info.name()))
                    .filter(info -> updates.stream()
                            .noneMatch(newInfo -> newInfo.name().equals(info.name())))
                    .toList();
            // вместе они новое содержание библиотеки
            library.retainAll(remainings);
            library.addAll(updates);
        }
        // библиотека сортирована по убыванию свежести сохранения
        library.sort(Comparator.comparing(AlbumInfo::modified).reversed());
    }

    /**
     * Перечитывает базу карт целиком.
     */
    public void reloadLibrary() {
        library.clear();
        library.addAll(chartRepository.getLibrarySummery());
    }

    @PostConstruct
    public void initializeChartIndex() {
        reloadLibrary();
    }

    /**
     * Выдаёт строковое представление списка альбомов карт,
     * присутствующих в библиотеке.
     *
     * @return нумерованный (с 1) список альбомов (многостроку).
     * Если в базе данных нет ни одного альбома карт, то сообщение об этом.
     */
    public String listAlbums() {
        if (library.isEmpty()) return "в базе %s нет ни файла"
                .formatted(Environments.baseDir.toString());

        return IntStream.range(0, library.size())
                .mapToObj(albumIndex -> "%d. %s%n".formatted(albumIndex + 1,
                                library.get(albumIndex).name()))
                .collect(Collectors.joining());
    }

    /**
     * Выдаёт строковое представление содержимого библиотеки
     * (как оно отображается в памяти).
     *
     * @return нумерованный (с 1) список альбомов, под каждым из них —
     * нумерованный (с 1) список карт в нём.
     */
    public String listAlbumsContents() {
        if (library.isEmpty()) return "в базе %s нет ни файла"
                .formatted(Environments.baseDir.toString());

        StringBuilder output = new StringBuilder();
        IntStream.range(0, library.size())
                .forEach(albumIndex -> {
                    output.append("%d. %s:%n".formatted(albumIndex + 1,
                                    library.get(albumIndex).name()));
                    var chartNames = library.get(albumIndex).chartNames();
                    IntStream.range(0, chartNames.size())
                            .mapToObj(chartIndex -> "\t%d. %s%n".formatted(chartIndex + 1,
                                            chartNames.get(chartIndex)))
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
        List<String> albumNames = library.stream()
                .map(AlbumInfo::name)
                .toList();
        try {
            groupIndex = defineIndexFromInput(chartListOrder, albumNames);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Списка не найдено: " + e);
        }
        return chartRepository.getAlbumSubstance(albumNames.get(groupIndex));
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
     * @return  сообщение об успехе или неуспехе.
     */
    public String autosave(ChartList desk) {
        return chartRepository.saveChartsAsAlbum(desk, "autosave.daw");
    }

    public String deleteAlbum(String fileToDelete) {
        var albumNames = library.stream().map(AlbumInfo::name).toList();
        if (fileToDelete.endsWith("***")) {
            String prefix = fileToDelete.substring(0, fileToDelete.length() - 3).trim();
            library.stream()
                    .map(AlbumInfo::name)
                    .filter(name -> name.startsWith(prefix))
                    .forEach(this::deleteAlbum);
        }
        String albumToDelete;
        try {
            albumToDelete = albumNames.get(defineIndexFromInput(fileToDelete, albumNames));
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }

        if (confirmDeletion(albumToDelete)) {
            String result = chartRepository.deleteAlbum(albumToDelete);
            updateLibrary();
            return result;
        }
        return "Отмена удаления " + albumToDelete;
    }

    public void saveChartsAsAlbum(ChartList charts, String filename) {
        chartRepository.saveChartsAsAlbum(charts, filename);
        updateLibrary();
    }

    public String addChartListToAlbum(ChartList content, String filename) {
        String result = chartRepository.addChartsToAlbum(content, filename);
        updateLibrary();
        return result;
    }

    public void addChartsToAlbum(String filename, ChartObject... charts) {
        chartRepository.addChartsToAlbum(filename, charts);
        updateLibrary();
    }

    private boolean confirmDeletion(String nameToDelete) {
        return userController
                .confirmationAnswer("Точно удалить " + nameToDelete + "?");
    }


}
