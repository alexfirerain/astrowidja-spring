package ru.swetophor.astrowidjaspring.service;

import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static ru.swetophor.astrowidjaspring.config.Environments.reportsDir;
import static ru.swetophor.astrowidjaspring.utils.Decorator.print;

@Service
public class ExportService {

    public void exportReport(String report, String fileName) {

        try (PrintWriter out = new PrintWriter(reportsDir.resolve(fileName).toFile(), StandardCharsets.UTF_8)) {
            out.println(report);
            print("Записан отчёт %s."
                    .formatted(fileName));
        } catch (FileNotFoundException e) {
            print("Запись отчёта %s обломалась: %s"
                    .formatted(fileName, e.getLocalizedMessage()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
