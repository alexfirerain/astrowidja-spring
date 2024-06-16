package ru.swetophor.astrowidjaspring.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.swetophor.astrowidjaspring.client.UserController;
import ru.swetophor.astrowidjaspring.model.AlbumInfo;
import ru.swetophor.astrowidjaspring.model.astro.Astra;
import ru.swetophor.astrowidjaspring.model.chart.Chart;
import ru.swetophor.astrowidjaspring.model.chart.ChartList;
import ru.swetophor.astrowidjaspring.model.chart.ChartObject;
import ru.swetophor.astrowidjaspring.model.chart.MultiChart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

import static ru.swetophor.astrowidjaspring.config.Environments.baseDir;
import static ru.swetophor.astrowidjaspring.utils.Decorator.console;

@Repository
@RequiredArgsConstructor
public class FileChartRepository implements ChartRepository {

    /**
     * Папка с файлами данных.
     */
    private final File base = baseDir.toFile();

    private final UserController userController;


    @Override
    public ChartList getAlbumSubstance(String albumName) {
        return readChartsFromFile(albumName);
    }

    /**
     * Прочитывает список карт из формата *.daw
     * Если файл не существует или чтение обламывается,
     * выводит об этом сообщение.
     * @param filename имя файла в папке данных.
     * @return список карт, прочитанных из файла.
     * Если файл не существует или не читается, то пустой список
     * с именем, указанным в запросе.
     */
    private ChartList readChartsFromFile(String filename) {
        if (filename == null || filename.isBlank())
            throw new IllegalArgumentException("пустое имя");
        filename = addExtension(filename);
        Path filePath = baseDir.resolve(filename);

        ChartList read = new ChartList(removeExtension(filename));
        if (!Files.exists(filePath))
            console("Не удалось обнаружить файла '%s'%n".formatted(filename));
        else
            try {
                String finalFilename = filename;
                Arrays.stream(Files.readString(filePath)
                                .split("#"))
                        .filter(s -> !s.isBlank() && !s.startsWith("//"))
                        .map(Chart::readFromString)
                        .forEach(chart -> userController.mergeChartIntoList(read, chart, finalFilename));
            } catch (IOException e) {
                console("Не удалось прочесть файл '%s': %s%n".formatted(filename, e.getLocalizedMessage()));
            }
        return read;
    }

    private ChartList readChartsFrom(String filename) {
        if (filename == null || filename.isBlank())
            throw new IllegalArgumentException("пустое имя");
        ChartList read = new ChartList(removeExtension(filename));
        Path filePath = baseDir.resolve(addExtension(filename));
        if (!Files.exists(filePath)) {
            console("Не удалось обнаружить файла '%s'%n"
                    .formatted(filename));
        } else {
            try {
                String[] lines = Files.readString(filePath)
                        .lines().toArray(String[]::new);

                Chart nextChart = null;
                boolean fillingChart = false;
                Queue<String> multiChartMarks = new ArrayDeque<>();

                for (String line : lines) {
                    if (line.isBlank() || line.startsWith("//"))
                        continue;

                    if (line.startsWith("<")) {
                        if (fillingChart) {
                            read.add(nextChart);
                            fillingChart = false;
                            nextChart = null;
                        }
                        multiChartMarks.add(line);
                        read.add(null);
                        continue;
                    }

                    if (line.startsWith("#")) {
                        if (fillingChart)
                            read.add(nextChart);
                        nextChart = new Chart(line.substring(1).trim());
                        fillingChart = true;
                        continue;
                    }

                    if (nextChart != null)
                        nextChart.addAstra(Astra.readFromString(line));
                }
                if (nextChart != null)
                    read.add(nextChart);


                Queue<MultiChart> multiCharts = new ArrayDeque<>();

                multiChartMarks.forEach(mark -> {
                    String[] parts = mark
                            .substring(mark.indexOf("<") + 1, mark.lastIndexOf(">"))
                            .split("#");
                    if (parts.length < 2) throw new IllegalArgumentException("неверный формат");
                    Chart[] components = new Chart[parts.length - 1];
                    IntStream.range(1, parts.length)
                            .forEach(i -> components[i - 1] = (Chart) read.get(parts[i].trim()));
                    multiCharts.add(new MultiChart(parts[0].trim()
                            .substring(0, mark.lastIndexOf(":")), components));
                });

                IntStream.range(0, read.size())
                        .filter(i -> read.get(i) == null)
                        .forEach(i -> read.setItem(i, multiCharts.remove()));


            } catch (IOException e) {
                console("Не удалось прочесть файла '%s': %s%n"
                        .formatted(filename, e.getLocalizedMessage()));
            }
        }
        return read;
    }


    /**
     * Обеспечивает строку, означающую имя файла, расширением ".daw"
     * (данные АстроВидьи), если оно ещё не присутствует. А если присутствует,
     * то ничего не делает.
     * @param albumName имя группы/файла/альбома с картами.
     * @return  имя с конвенциональным расширением .daw.
     */
    private static String addExtension(String albumName) {
        return albumName.endsWith(".daw") ?
                albumName :
                albumName + ".daw";
    }

    @Override
    public String deleteAlbum(String fileToDelete) {
        fileToDelete = addExtension(fileToDelete);
        String report;
        try {
            if (Files.deleteIfExists(baseDir.resolve(fileToDelete))) {
                report = "Файл %s удалён.".formatted(fileToDelete);
            } else {
                report = "не найдено файла " + fileToDelete;
            }
        } catch (Exception e) {
            report = "ошибка удаления файла %s: %s".formatted(fileToDelete, e.getLocalizedMessage());
        }
        return report;
    }


    /**
     * Выдаёт список альбомов (баз, списков карт), присутствующих в картохранилище.
     * Файловая реализация картохранилища выдаёт список файлов
     *
     * @return список имён файлов АстроВидьи, присутствующих в рабочей папке
     * в момент вызова, сортированный по дате последнего изменения.
     */
    @Override
    public List<String> albumNames() {
        File[] files = base.listFiles();
        assert files != null;
        return Arrays.stream(files)
                        .filter(this::isAstroWidjaDataFile)
                        .sorted(Comparator.comparing(File::lastModified))
                        .map(File::getName)
                        .map(FileChartRepository::removeExtension)
                        .toList();
    }


    /**
     * Прочитывает содержание всех файлов с картами.
     * Если по какой-то причине таковых не найдено, то пустой список.
     *
     * @return список списков карт, соответствующих файлам в рабочей папке.
     */
    @Override
    public List<ChartList> getAllAlbums() {
        return albumNames().stream()
                .map(this::readChartsFromFile)
                .toList();
    }


    /**
     * Выдаёт список имён карт из файла базы данных Астровидьи.
     * @param albumName имя файла, соответствующего картосписку.
     * @return   список имён карт, найденных в файле.
     */
    @Override
    public List<String> getAlbumContents(String albumName) {
        albumName = addExtension(albumName);
        List<String> list = new ArrayList<>();
        try {
            list = Files.readString(baseDir.resolve(albumName))
                    .lines()
                    .filter(line -> line.startsWith("#") && line.length() > 1)
                    .map(line -> line.substring(1).strip())
                    .toList();
        } catch (IOException e) {
            console("Файл %s не прочитался:%s".formatted(albumName, e.getLocalizedMessage()));
        }
        return list;
    }

    /**
     * Записывает содержимое картосписка (как возвращается {@link ChartList#getString()})
     * в файл по указанному адресу (относительно рабочей папки).
     * Существующий файл заменяется, несуществующий создаётся.
     * Если предложенное для сохранения имя уже оканчивается на {@code .daw},
     * так и используется, если же ещё нет, то нужное расширение добавляется.
     *
     * @param content  список карт, чьё содержимое записывается.
     * @param fileName имя файла в рабочей папке, в который сохраняется.
     * @return  строку, сообщающую, что карты или записались, или нет.
     */
    @Override
    public String saveChartsAsAlbum(ChartList content, String fileName) {
        fileName = addExtension(fileName);

        try (PrintWriter out = new PrintWriter(baseDir.resolve(fileName).toFile())) {
            out.println(content.getString());
            return "Карты { %s } записаны в файл %s."
                    .formatted(String.join(", ", content.getNames()),
                            fileName);
        } catch (FileNotFoundException e) {
            return "Запись в файл %s обломалась: %s%n"
                    .formatted(fileName, e.getLocalizedMessage());
        }
    }

    /**
     * Добавляет указанные карты к файлу с указанным названием.
     * @param file  название файла (группы), к которому добавлять.
     * @param charts карты, которые вливаются в файл. Если возникает
     *               конфликт названия, идёт интерактивное уточнение.
     * @return  изменился ли список (файл) в результате вызова функции.
     */
    @Override
    public boolean addChartsToAlbum(String file, ChartObject... charts) {
        file = addExtension(file);
        ChartList fileContent = readChartsFromFile(file);
        boolean changed = false;
        for (ChartObject c : charts)
            if (userController.mergeChartIntoList(fileContent, c, file))
                changed = true;
        if (changed)
            saveChartsAsAlbum(fileContent, file);
        return changed;
    }

    /**
     * Убирает из строки расширение файла ".daw" (данные АстроВидьи),
     * если оно присутствует. Если отсутствует, то ничего не делает.
     * @param filename  имя файла / группы / альбома с картами.
     * @return  имя без конвенционального расширения.
     */
    private static String removeExtension(String filename) {
        return filename.endsWith(".daw") ?
                filename.substring(0, filename.length() - 4) :
                filename;
    }

    /**
     * Добавляет карты из указанного картосписка в файл с указанным именем.
     * Если список пуст или в ходе выполнения ни одной карты из списка не добавляется,
     * сообщает об этом и выходит. Если хотя бы одна карта добавляется,
     * переписывает указанный файл его новой версией после слияния и сообщает,
     * какое содержание было записано.
     * Если запись обламывается, сообщает и об этом.
     *
     * @param table  список карт, который надо добавить к списку в файле.
     * @param target имя файла в папке базы данных, в который нужно дописать карты.
     * @return  строку с описанием результата операции.
     */
    @Override
    public String addChartsToAlbum(ChartList table, String target) {
        target = addExtension(target);
        String result;
        ChartList fileContent = readChartsFromFile(target);
        if (table.isEmpty() || !fileContent.addAll(table)) {
            result = "Никаких новых карт в файл не добавлено.";
        } else {
            String drop = fileContent.getString();

            try (PrintWriter out = new PrintWriter(baseDir.resolve(target).toFile())) {
                out.println(drop);
                result = "Строка {%n%s%n} записана в %s%n".formatted(drop, target);
            } catch (FileNotFoundException e) {
                result = "Запись в файл %s обломалась: %s%n".formatted(target, e.getLocalizedMessage());
            }
        }
        return result;
    }

    @Override
    public AlbumInfo getAlbumSummary(String filename) {
        Path filePath = baseDir.resolve(addExtension(filename));
        List<String> chartNames;
        long lastModifiedTime;
        try {
            chartNames = Files.readString(filePath).lines()
                    .filter(line -> line.startsWith("#") && line.length() > 1)
                    .map(line -> line.substring(1).strip())
                    .toList();
            lastModifiedTime = Files.getLastModifiedTime(filePath).toMillis();
        } catch (IOException e) {
            console("Нет доступа к файлу " + filePath);
            return null;
        }
        return new AlbumInfo(removeExtension(filename), chartNames, lastModifiedTime);
    }

    /**
     * Строит для некого файла карточку краткой информации:
     * название, дата обновления, оглавление карт.
     * @param file собственно файл.
     * @return  заполненный объект {@link AlbumInfo}.
     */
    private AlbumInfo buildSummery(File file) {
        List<String> chartNames;
        try {
            chartNames = Files.readString(file.toPath()).lines()
                    .filter(line -> line.startsWith("#") && line.length() > 1)
                    .map(line -> line.substring(1).strip())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Чтение файла '%s' обломалось: %s."
                    .formatted(file.toString(), e));
        }
        return new AlbumInfo(
                            removeExtension(file.getName()),
                            chartNames,
                            file.lastModified());
    }

    @Override
    public List<AlbumInfo> getLibrarySummery() {
        File[] files = base.listFiles();
        if (files == null) throw new IllegalArgumentException();
        return Arrays.stream(files)
                .filter(this::isAstroWidjaDataFile)
                .map(file -> {
                    try {
                        return buildSummery(file);
                    } catch (Exception e) {
                        console(e.getLocalizedMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AlbumInfo::getModified))
                .toList();
    }

    @Override
    public List<AlbumInfo> getLibraryUpdates(long since) {
        File[] files = base.listFiles();
        if (files == null) throw new IllegalArgumentException();
        return Arrays.stream(files)
                .filter(this::isAstroWidjaDataFile)
                .filter(file -> file.lastModified() > since)
                .map(file -> {
                    try {
                        return buildSummery(file);
                    } catch (Exception e) {
                        console(e.getLocalizedMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AlbumInfo::getModified))
                .toList();
    }

    private boolean isAstroWidjaDataFile(File file) {
        return !file.isDirectory() && file.getName().endsWith(".daw");
    }
}
