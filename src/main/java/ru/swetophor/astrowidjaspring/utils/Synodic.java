package ru.swetophor.astrowidjaspring.utils;

import lombok.AllArgsConstructor;

public class Synodic {
    // это написал сам смешной ИИ, просто прочитав название класса))
//    private static final double JULIAN_YEAR = 365.25;
//    private static final double JULIAN_MONTH = 30.436848;
//    private static final double JULIAN_DAY = 24.0;
//    private static final double SOLAR_MONTH = 30.32;
//    private static final double SOLAR_YEAR = SOLAR_MONTH * 12;
//    private static final double JULIAN_EPOCH = 2440587.5;
//    private static final double SOLAR_EPOCH = 2451545.0;
//    private static final double SECONDS_IN_HOUR = 3600.0;
//    private static final double SECONDS_IN_DAY = 24 * SECONDS_IN_HOUR;
//    private static final double JULIAN_TO_SOLAR_EPOCH_DIFF = JULIAN_EPOCH - SOLAR_EPOCH;
//    private static final double JULIAN_TO_SOLAR_DAYS_PER_YEAR = JULIAN_YEAR / SOLAR_YEAR;
//    private static final double SOLAR_TO_JULIAN_DAYS_PER_YEAR = 1.0 / JULIAN_TO_SOLAR_DAYS_PER_YEAR;
//    private static final double SOLAR_TO_JULIAN_DAYS_PER_MONTH = SOLAR_MONTH / JULIAN_MONTH;
//    private static final double SOLAR_TO_JULIAN_DAYS_PER_DAY = SOLAR_DAY / JULIAN_DAY;
//    private static final double JULIAN_TO_SOLAR_SECONDS_PER_DAY = SECONDS_IN_DAY / SOLAR_TO_JULIAN_DAYS_PER_YEAR;
//    private static final double SOLAR_TO_JULIAN_SECONDS_PER_MONTH = SECONDS_IN_DAY / SOLAR_TO_JULIAN_DAYS_PER_MONTH;
//    public static double getSynodicMonthDays(double julianDate) {
//        double solarDate = julianDate + JULIAN_TO_SOLAR_EPOCH_DIFF;
//        double yearDays = (solarDate - SOLAR_EPOCH) * SOLAR_TO_JULIAN_DAYS_PER_YEAR;
//        double yearDaysInMonths = yearDays / SOLAR_TO_JULIAN_DAYS_PER_MONTH;
//        double synodic
//    }

    static double YEAR = 365.256363004;
    public static AstroSideric[] objects = new AstroSideric[]{
        new AstroSideric("Меркурий", 87.969, '☿'),
        new AstroSideric("Венера", 224.701, '♀'),
        new AstroSideric("Земля", YEAR, '♁'),
        new AstroSideric("Марс", 686.98, '♂'),
        new AstroSideric("Церера", 4.600878134 * YEAR, '⚳'),
        new AstroSideric("Юпитер", 11.861775658 * YEAR, '♃'),
        new AstroSideric("Сатурн", 29.456626026 * YEAR, '♄'),
        new AstroSideric("Хирон", 50.630192997 * YEAR, '⚷'),
        new AstroSideric("Уран", 84.01058369 * YEAR, '♅'),
        new AstroSideric("Нептун", 164.788451596 * YEAR, '♆'),
        new AstroSideric("Плутон", 247.916340184 * YEAR, '⯓'),
        new AstroSideric("Эрида", 558.046408829 * YEAR, '⯰'),
        new AstroSideric("Раху", -18.612958 * YEAR, '☊'),
        new AstroSideric("Лилит", 8.850174001 * YEAR, '⚸')
    };

    public static String describe(double synodic) {
        return "%.2f дн. / %.2f г.".formatted(synodic, synodic / YEAR);
    }


    public static double calculateSynodic(double periodA, double periodB) {
        double inner = Math.min(periodA, periodB);
        double outer = Math.max(periodA, periodB);

        return 1 / (1 / inner - 1 / outer);
    }


    public static void main(String[] args) {
        for (int i = 0; i < objects.length; i++) {
            for (int j = i + 1; j < objects.length; j++) {
                double synodic = calculateSynodic(objects[i].period, objects[j].period);
                System.out.printf("%s (%c) и %s (%c) имеют синодический период: %s%n",
                        objects[i].name, objects[i].symbol, objects[j].name, objects[j].symbol, describe(synodic));
            }
        }


    }

    @AllArgsConstructor
    public static class AstroSideric {
        private String name;
        private double period;
        private char symbol = '*';
    }
}
