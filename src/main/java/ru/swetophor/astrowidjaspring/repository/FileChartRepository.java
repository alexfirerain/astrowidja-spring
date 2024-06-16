package ru.swetophor.astrowidjaspring.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.swetophor.astrowidjaspringshell.client.UserController;
import ru.swetophor.astrowidjaspringshell.model.Chart;
import ru.swetophor.astrowidjaspringshell.model.ChartList;
import ru.swetophor.astrowidjaspringshell.model.ChartObject;
import ru.swetophor.astrowidjaspringshell.utils.Mechanics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static ru.swetophor.astrowidjaspringshell.utils.Decorator.print;

@Repository
@RequiredArgsConstructor
public class FileChartRepository implements ChartRepository {
    private static final String baseDir = "base";
    /**
     * Рабочая папка.
     */
    private static final File base = new File(baseDir);

    private final UserController userController;

    static {
        Path basePath = Path.of(baseDir);
        if (!Files.exists(basePath)) {
            String msg;
            try {
                Files.createDirectory(basePath);
                msg = "Создали папку '%s'%n".formatted(baseDir);
            } catch (IOException e) {
                msg = "Не удалось создать папку %s: %s%n".formatted(baseDir, e.getLocalizedMessage());
            }
            print(msg);
        }
    }


    @Override
    public boolean albumExists(String albumName) {
        return false;
    }

    @Override
    public ChartList getAlbumSubstance(String albumName) {
        return readChartsFromFile(albumName);
    }

    /**
     * Прочитывает список карт из формата *.awb
     * Если файл не существует или чтение обламывается,
     * выводит об этом сообщение.
     * Ожидается, что имя файла уже содержит расширение,
     * оно не восполняется.
     * @param filename имя файла в папке данных.
     * @return список карт, прочитанных из файла.
     * Если файл не существует или не читается, то пустой список
     * с именем, указанным в запросе.
     */
    private ChartList readChartsFromFile(String filename) {
        if (filename == null || filename.isBlank())
            throw new IllegalArgumentException("пустое имя");
        String listName = filename.endsWith(".awc") || filename.endsWith(".awb") ?
                filename.substring(0, filename.length() - 4) :
                filename;

        ChartList read = new ChartList(listName);
        Path filePath = Path.of(baseDir, filename);
        if (!Files.exists(filePath))
            print("Не удалось обнаружить файла '%s'%n".formatted(filename));
        else
            try {
                Arrays.stream(Files.readString(filePath)
                                .split("#"))
                        .filter(s -> !s.isBlank() && !s.startsWith("//"))
                        .map(Chart::readFromString)
                        .forEach(chart -> read.addResolving(chart, filename));
            } catch (IOException e) {
                print("Не удалось прочесть файл '%s': %s%n".formatted(filename, e.getLocalizedMessage()));
            }
        return read;
    }


    @Override
    public String deleteAlbum(String fileToDelete) {
        // TODO: выделить функции идентификации файла/карты по номеру/имени
        String report = "Файл %s удалён.".formatted(fileToDelete);
        try {
            List<String> fileList = albumNames();
            if (fileToDelete.matches("^\\d+")) {
                int indexToDelete;
                try {
                    indexToDelete = Integer.parseInt(fileToDelete) - 1;
                    if (indexToDelete < 0 || indexToDelete >= fileList.size()) {
                        report = "в базе всего " + fileList.size() + "файлов";
                    } else {
                        String nameToDelete = fileList.get(indexToDelete);
                        if (confirmDeletion(nameToDelete)) {
                            if (!Files.deleteIfExists(Path.of(baseDir, nameToDelete))) {
                                report = "не найдено файла " + nameToDelete;
                            }
                        } else {
                            report ="отмена удаления " + nameToDelete;
                        }
                    }
                } catch (NumberFormatException e) {         // никогда не выбрасывается
                    report = "не разобрать числа, " + e.getLocalizedMessage();
                }
            } else if (fileToDelete.endsWith("***")) {
                String prefix = fileToDelete.substring(0, fileToDelete.length() - 3);
                for (String name : fileList) {
                    if (name.startsWith(prefix)) {
                        if (confirmDeletion(name)) {
                            if (!Files.deleteIfExists(Path.of(baseDir, name))) {
                                report = "не найдено файла " + name;
                            } else {
                                report = name + " удалился";
                            }
                        } else {
                            report = "отмена удаления " + name;
                        }
                    }
                }
            } else if (fileToDelete.matches("^[\\p{L}\\-. !()+=_\\[\\]№\\d]+$")) {
                // TODO: нормальную маску допустимого имени файла
                if (!fileToDelete.endsWith(".awb") && !fileToDelete.endsWith(".awc")) {
                    if (Files.exists(Path.of(baseDir, fileToDelete + ".awc"))) {
                        fileToDelete = fileToDelete + ".awc";
                    }
                    else if (Files.exists(Path.of(baseDir, fileToDelete + ".awb"))) {
                        fileToDelete = fileToDelete + ".awb";
                    }
                }
                if (!Files.deleteIfExists(Path.of(baseDir, fileToDelete))) {
                    report = "не найдено файла " + fileToDelete;
                }
            } else {
                report = "скорее всего, недопустимое имя файла";
            }
        } catch (IOException e) {
            report = "ошибка чтения базы, %s".formatted(e.getLocalizedMessage());
        } catch (Exception e) {
            report = "ошибка удаления файла %s: %s".formatted(fileToDelete, e.getLocalizedMessage());
        }
        return report;
    }

    private boolean confirmDeletion(String nameToDelete) {
        return userController
                .confirmationAnswer("Точно удалить " + nameToDelete + "?");
    }

    /**
     * Выдаёт список альбомов (баз, списков карт), присутствующих в картохранилище.
     *
     * @return список имён файлов АстроВидьи, присутствующих в рабочей папке
     * в момент вызова, сортированный по дате последнего изменения.
     */
    @Override
    public List<String> albumNames() {
        File[] files = base.listFiles();
        assert files != null;
        return Arrays.stream(files)
                        .filter(file -> !file.isDirectory())
                        .filter(file -> file.getName().endsWith(".awb") || file.getName().endsWith(".awc"))
                        .sorted(Comparator.comparing(File::lastModified))
                        .map(File::getName)
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
     * @throws IOException при проблемах чтения файла.
     */
    @Override
    public List<String> getAlbumContents(String albumName) {
        List<String> list = new ArrayList<>();
        try {
            list = Files.readString(Path.of(baseDir, albumName))
                    .lines()
                    .filter(line -> line.startsWith("#") && line.length() > 1)
                    .map(line -> line.substring(1).strip())
                    .toList();
        } catch (IOException e) {
            print("Файл %s не прочитался:%s".formatted(albumName, e.getLocalizedMessage()));
        }
        return list;
    }

    /**
     * Записывает содержимое картосписка (как возвращается {@link ChartList#getString()})
     * в файл по указанному адресу (относительно рабочей папки).
     * Существующий файл заменяется, несуществующий создаётся.
     * Если предложенное для сохранения имя оканчивается на {@code .awc} или {@code .awb},
     * используется оно. Если не оканчивается, то к нему добавляется {@code .awb}
     * или (если сохраняемый список содержит только одну карту) {@code .awc}.
     *
     * @param content список карт, чьё содержимое записывается.
     * @param fileName    имя файла в рабочей папке, в который сохраняется.
     */
    @Override
    public void saveChartsAsAlbum(ChartList content, String fileName) {
        fileName = Mechanics.extendFileName(fileName, content.size() == 1);

        try (PrintWriter out = new PrintWriter(Path.of(baseDir, fileName).toFile())) {
            out.println(content.getString());
            System.out.printf("Карты {%s} записаны в файл %s.%n",
                    String.join(", ", content.getNames()),
                    fileName);
        } catch (FileNotFoundException e) {
            System.out.printf("Запись в файл %s обломалась: %s%n", fileName, e.getLocalizedMessage());
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
        String result;
        ChartList fileContent = readChartsFromFile(target);
        if (table.isEmpty() || !fileContent.addAll(table)) {
            result = "Никаких новых карт в файл не добавлено.";
        } else {
            String drop = fileContent.getString();

            try (PrintWriter out = new PrintWriter(Path.of(baseDir, target).toFile())) {
                out.println(drop);
                result = "Строка {%n%s%n} записана в %s%n".formatted(drop, target);
            } catch (FileNotFoundException e) {
                result = "Запись в файл %s обломалась: %s%n".formatted(target, e.getLocalizedMessage());
            }
        }
        return result;
    }
}
