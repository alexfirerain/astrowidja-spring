package ru.swetophor.astrowidjaspring.model.astro;

import lombok.Getter;

import static ru.swetophor.astrowidjaspring.utils.CelestialMechanics.normalizeCoordinate;

/**
 * Представление зодиакального знака, содержит символ.
 */
@Getter
public enum ZodiacSign {
    ARIES('♈'),
    TAURUS('♉'),
    GEMINI('♊'),
    CANCER('♋'),
    LEO('♌'),
    VIRGO('♍'),
    LIBRA('♎'),
    SCORPIO('♏'),
    SAGITTARIUS('♐'),
    CAPRICORN('♑'),
    AQUARIUS('♒'),
    PISCES('♓');

    private final char symbol;

    ZodiacSign(char symbol) {
        this.symbol = symbol;
    }

    public static char zodiumIcon(double position) {
        return getZodiumOf(position).getSymbol();
    }

    public static ZodiacSign getZodiumOf(double position) {
        return values()[(int) normalizeCoordinate(position) / 30];
    }

}
