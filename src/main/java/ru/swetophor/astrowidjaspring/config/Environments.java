package ru.swetophor.astrowidjaspring.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static ru.swetophor.astrowidjaspring.utils.Decorator.console;

@Component
public class Environments {

    /**
     * Папка программы, создаваемая в пространстве пользователя.
     */
    public static Path appDir;
    /**
     * Папка с данными, создаваемая в папке программы.
     */
    public static Path baseDir;
    /**
     * Папка с отчётами, создаваемая в папке программы.
     */
    public static Path reportsDir;
    /**
     * Файл с настройками, хранимый в папке программы.
     */
    public static Path settingsSource;

    @PostConstruct
    public void prepareFolders() {
        makeDir(appDir);
        makeDir(baseDir);
        makeDir(reportsDir);
    }

    private static void makeDir(Path path) {
        if (Files.exists(path)) return;
        String msg;
        try {
            Files.createDirectory(path);
            msg = "Создали папку '%s'%n".formatted(path);
        } catch (IOException e) {
            msg = "Не удалось создать папку %s: %s%n".formatted(path, e.getLocalizedMessage());
        }
        console(msg);
    }

    @Autowired
    public void setAppDir(@Qualifier("working-dir") String workingDir) {
        appDir = Path.of(System.getProperty("user.home"), workingDir);
    }

    @Autowired
    public void setBaseDir(@Qualifier("base-dir") String base) {
        baseDir = appDir.resolve(base);
    }

    @Autowired
    public void setReportsDir(@Qualifier("reports-dir") String reports) {
        reportsDir = appDir.resolve(reports);
    }

    @Autowired
    public void setSettingsSource(@Qualifier("settings") String settingsFile) {
        settingsSource = appDir.resolve(settingsFile);
    }

}
