package ru.swetophor.astrowidjaspring.repository;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Repository;
import ru.swetophor.astrowidjaspring.client.UserController;
import ru.swetophor.astrowidjaspring.exception.FileFormatException;
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


    @SneakyThrows
    @Override
    public ChartList getAlbumSubstance(String albumName) {
        return readChartsFromDAW(albumName);
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
    private ChartList readChartsFrom(String filename) {
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

    /**
     * Восстанавливает из строкового представления в daw-файле
     * картосписок, идентичный сохранённому в тот файл.
     * @param filename  имя открываемого файла аз папке базы данных.
     * @return  картосписок из карт и многокарт.
     * @throws FileNotFoundException    если файл не обнаружился.
     */
    private ChartList readChartsFromDAW(String filename) throws FileNotFoundException {
        if (filename == null || filename.isBlank())
            throw new IllegalArgumentException("пустое имя");
        // новый пустой картосписок, который будет заполнен и отдан
        ChartList read = new ChartList(removeExtension(filename));
        Path filePath = baseDir.resolve(addExtension(filename));
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Не удалось обнаружить файла '%s'%n"
                    .formatted(filename));
        } else {
            try {
                // строки из файла
                String[] lines = Files.readString(filePath)
                        .lines().toArray(String[]::new);

                // текущая карта, в которую добавляем астры (пока никакая)
                Chart nextChart = null;
                // идёт ли процесс заполнения (пока нет)
                boolean fillingChart = false;
                // сюда будем складывать строки, содержащие определения мультикарт
                Queue<String> multiChartDescriptions = new ArrayDeque<>();

                // перебираем по строке
                for (String line : lines) {
                    // пустые и закомментированные не интересуют
                    if (line.isBlank() || line.startsWith("//"))
                        continue;

                    // ежели это определение мультикарты
                    if (line.startsWith("<")) {
                        // если шло заполнение какой-то карты
                        if (fillingChart) {
                            // значит оно закончено
                            fillingChart = false;
                            // и карта готова добавляться к результирующему картосписку
                            read.add(nextChart);
                            // теперь будем заполнять новую (но не раньше, чем она объявится)
                            nextChart = null;
                        }
                        // отложим эту строку в сторонку
                        multiChartDescriptions.add(line.substring(1, line.lastIndexOf(">")).trim());
                        // и зарезервируем в строимом картосписке местечко
                        read.add(null);
                        continue;
                    }

                    // если это определение новой карты
                    if (line.startsWith("#")) {
                        // если идёт заполнение какой-то карты
                        if (fillingChart)
                            // значит оно закончено, можно её добавить
                            read.add(nextChart);
                        // инициируем заполнять новую карту, создаём её по имени
                        nextChart = new Chart(line.substring(1).trim());
                        // если заполнение карты ещё не шло, теперь точно идёт
                        fillingChart = true;
                        continue;
                    }

                    // если строка не начинается ни с '<', ни с '#', ни с '//'
                    // и какая-то карта заполняется
                    if (nextChart != null)
                        // значит это строка с определением астры для этой карты
                        nextChart.addAstra(Astra.readFromString(line));
                }
                // прочитав все строки
                // если заполнялась какая-то карта
                if (nextChart != null)
                    // значит и она готова, добавим её
                    read.add(nextChart);

                // список мультикарт, которые восстановим
                Queue<MultiChart> multiCharts = new ArrayDeque<>();

                // перебираем строки с определениями мультикарт
                for (String string : multiChartDescriptions) {
                    // разбиваем определение на фрагменты
                    String[] parts = string.split("#");
                    if (parts.length < 2)
                        throw new FileFormatException("В строке не определены входящие карты: " + string);
                    // сейчас будем находить привходящие карты
                    Chart[] components = new Chart[parts.length - 1];
                    // переберём найденные фрагменты определения, кроме первого
                    for (int i = 1; i < parts.length; i++) {
                        // это ничто иное как имя карты, которая уже должна быть в наполняемом
                        Chart chart = (Chart) read.get(parts[i].trim());
                        // но если её там нет
                        if (chart == null)
                            // то это нарушение формата
                            throw new FileFormatException("Входящая карта " + parts[i] + " не определена в этом альбоме.");
                        // а если есть, то это одна из карт воссоздаваемой многокарты
                        components[i - 1] = chart;
                    }
                    // и мы восстанавливаем из файла объект многокарты (название в первом фрагменте)
                    multiCharts.add(new MultiChart(parts[0].trim().substring(0, string.lastIndexOf(":")),
                            components));
                }

                // пройдёмся по воссоздаваемому картосписку
                IntStream.range(0, read.size())
                        // и где оставлены свободные места
                        .filter(i -> read.get(i) == null)
                        // заполним по порядку воссозданными многокартами
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
    public String deleteAlbum(String albumToDelete) {
        String fileToDelete = addExtension(albumToDelete);
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
     * Записывает содержимое картосписка (как возвращается {@link ChartList#getString()})
     * в файл по указанному адресу (относительно рабочей папки).
     * Существующий файл заменяется, несуществующий создаётся.
     * Если предложенное для сохранения имя уже оканчивается на {@code .daw},
     * так и используется, если же ещё нет, то нужное расширение добавляется.
     *
     * @param content  список карт, чьё содержимое записывается.
     * @param albumName имя файла в рабочей папке, в который сохраняется.
     * @return  строку, сообщающую, что карты или записались, или нет.
     */
    @Override
    public String saveChartsAsAlbum(ChartList content, String albumName) {
        String fileName = addExtension(albumName);

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
    @SneakyThrows
    @Override
    public boolean addChartsToAlbum(String file, ChartObject... charts) {
        file = addExtension(file);
        ChartList fileContent = readChartsFromDAW(file);
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
    @SneakyThrows
    @Override
    public String addChartsToAlbum(ChartList table, String target) {
        target = addExtension(target);
        String result;
        ChartList fileContent = readChartsFromDAW(target);
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
                    .filter(line -> ((line.startsWith("#") || line.startsWith("<")) && line.length() > 1))
                    .map(line -> line.startsWith("<") ?
                            line.substring(1, line.lastIndexOf(":")).strip() :
                            line.substring(1).strip())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Чтение файла '%s' обломалось: %s."
                    .formatted(file.toString(), e));
        }
        return new AlbumInfo(removeExtension(file.getName()),
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
                .sorted(Comparator.comparing(AlbumInfo::modified))
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
                .sorted(Comparator.comparing(AlbumInfo::modified))
                .toList();
    }

    /**
     * Проверяет, является ли данный файл файлом данных АстроВидьи.
     * @param file проверяемый файл.
     * @return {@code true} если файл не является папкой и оканчивает своё
     * имя на ".daw", иначе {@code false}.
     */
    private boolean isAstroWidjaDataFile(File file) {
        return !file.isDirectory() && file.getName().endsWith(".daw");
    }
}
