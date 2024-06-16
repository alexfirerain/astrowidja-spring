package ru.swetophor.astrowidjaspring.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static ru.swetophor.astrowidjaspring.config.Environments.settingsSource;
import static ru.swetophor.astrowidjaspring.utils.CelestialMechanics.CIRCLE;
import static ru.swetophor.astrowidjaspring.utils.Decorator.print;

/**
 * Класс-хранилище статических глобальных переменных АстроВидьи.
 */
@Component
public class Settings {

    private static final Map<String, String> settingsMap = new HashMap<>();

    private static final int EDGE_HARMONIC_DEFAULT = 108;
    private static final int ORBS_DIVISOR_DEFAULT = 30;
    private static final boolean HALF_ORBS_FOR_DOUBLES_DEFAULT = true;
    private static final boolean AUTOSAVE_DEFAULT = false;
    private static final String AUTOLOAD_FILE_DEFAULT = "autosave.daw";
    private static final boolean AUTOLOAD_ENABLED_DEFAULTS = true;

    static {
        settingsMap.put("HARMONICA_ULTIMA", String.valueOf(EDGE_HARMONIC_DEFAULT));
        settingsMap.put("ORBS_DIVISOR", String.valueOf(ORBS_DIVISOR_DEFAULT));
        settingsMap.put("ORBES_DIMIDII_DUPLICIBUS", String.valueOf(HALF_ORBS_FOR_DOUBLES_DEFAULT));
        settingsMap.put("AUTOSAVE", String.valueOf(AUTOSAVE_DEFAULT));
        settingsMap.put("AUTOLOAD_FILE", AUTOLOAD_FILE_DEFAULT);
        settingsMap.put("AUTOLOAD_ENABLED", String.valueOf(AUTOLOAD_ENABLED_DEFAULTS));
    }

    @PostConstruct
    public void loadSettings() {
        try {
            String source = Files.readString(settingsSource);

            for (String line : source.lines().toList()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                int operatorPosition = line.indexOf("=");
                if (operatorPosition == -1) continue;
                String property = line.substring(0, operatorPosition).trim();
                String value = line.substring(operatorPosition + 1).trim();
                if (property.isBlank() || value.isBlank()) continue;
                settingsMap.put(property, value);
            }

            print("Загружены настройки из " + settingsSource);
        } catch (IOException | NullPointerException e) {
            saveSettings();
            print("Создан файл с настройками по умолчанию: " + settingsSource);
        }
    }

    /*
        Методы доступа к свойствам
     */

    private static Optional<Integer> getIntProperty(String property) {
        String value = settingsMap.get(property);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
    private static Optional<Boolean> getBoolProperty(String property) {
        String value = settingsMap.get(property);
        if (value == null) return Optional.empty();
        return Optional.of(Boolean.parseBoolean(value));
    }

    private static Optional<String> getStringProperty(String property) {
        return Optional.ofNullable(settingsMap.get(property));
    }

    /*
        Получатели свойств.
     */

    public static int getEdgeHarmonic() {
        return getIntProperty("HARMONICA_ULTIMA").orElse(EDGE_HARMONIC_DEFAULT);
    }
    public static boolean isHalfOrbsForDoubles() {
        return getBoolProperty("ORBES_DIMIDII_DUPLICIBUS").orElse(HALF_ORBS_FOR_DOUBLES_DEFAULT);
    }

    public static boolean isAutosave() {
        return getBoolProperty("AUTOSAVE").orElse(AUTOSAVE_DEFAULT);
    }

    public static String getAutoloadFile() {
        return getStringProperty("AUTOLOAD_FILE").orElse(AUTOLOAD_FILE_DEFAULT);
    }

    public static int getOrbDivisor() {
        return getIntProperty("ORBS_DIVISOR").orElse(ORBS_DIVISOR_DEFAULT);
    }
    /**
     * Сообщает, какой первичный орбис следует использовать в расчётах в программе.
     * @return  размер первичного орбиса для расчётов в градусах. Задаётся через свойство
     * в настройках "Делитель орбиса". Например, при значении первичного делителя в 30
     * (значение по умолчанию), первичный орбис возвращается как 12°.
     */
    public static double getPrimalOrb() {
        return CIRCLE / getOrbDivisor();
    }

    public static boolean isAutoloadEnabled() {
        return getBoolProperty("AUTOLOAD_ENABLED").orElse(AUTOLOAD_ENABLED_DEFAULTS);
    }


    /*
        Устанавливатели свойств.
     */
    public static void setAutosave(boolean turnAutosaveOn) {
        settingsMap.put("AUTOSAVE", String.valueOf(turnAutosaveOn));
    }

    public static void setAutoloadFile(String autoloadFile) {
        if (!autoloadFile.endsWith(".daw")) autoloadFile += ".daw";
        settingsMap.put("AUTOLOAD_FILE", autoloadFile);
    }

    public static void setEdgeHarmonic(int edgeHarmonic) {
        settingsMap.put("HARMONICA_ULTIMA", String.valueOf(edgeHarmonic));
    }

    public static void setOrbDivider(int orbsDivisor) {
        settingsMap.put("ORBS_DIVISOR", String.valueOf(orbsDivisor));
    }

    public static void disableHalfOrbForDoubles() {
        settingsMap.put("ORBES_DIMIDII_DUPLICIBUS", "false");
    }

    public static void enableHalfOrbForDoubles() {
        settingsMap.put("ORBES_DIMIDII_DUPLICIBUS", "true");
    }

public static void saveSettings() {
        StringBuilder drop = new StringBuilder();
        for (Map.Entry<String, String> property : settingsMap.entrySet())
            drop.append("%s = %s%n".formatted(property.getKey(), property.getValue()));
        try (FileWriter writer = new FileWriter(settingsSource.toFile(), false)) {
            writer.write(drop.toString());
            writer.flush();
        } catch (IOException e) {
            print("Не удалось сохранить настройки: " + e.getLocalizedMessage());
        }
    }


}