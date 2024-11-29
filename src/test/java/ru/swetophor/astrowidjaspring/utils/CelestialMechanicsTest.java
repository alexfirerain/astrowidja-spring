package ru.swetophor.astrowidjaspring.utils;

import org.junit.jupiter.api.Test;
import ru.swetophor.astrowidjaspring.model.astro.ZodiacPoint;

import static org.junit.jupiter.api.Assertions.*;
import static ru.swetophor.astrowidjaspring.utils.CelestialMechanics.getArc;
import static ru.swetophor.astrowidjaspring.utils.CelestialMechanics.getVectorArc;

class CelestialMechanicsArcTest {

    ZodiacPoint ZERO_ARIETIS = () -> 0;
    ZodiacPoint ZERO_GEMINI = () -> 60;
    ZodiacPoint ZERO_VIRGINIS = () -> 150;
    ZodiacPoint ZERO_SCORPII = () -> 210;
    ZodiacPoint ZERO_AQUARII = () -> 300;

    @Test
    void getArc_getsArc() {
        assertEquals(60, getArc(ZERO_ARIETIS, ZERO_GEMINI));
        assertEquals(60, getArc(ZERO_SCORPII, ZERO_VIRGINIS));
        assertEquals(0, getArc(ZERO_VIRGINIS, ZERO_VIRGINIS));
    }
    @Test
    void getArc_getsArc_over_0() {
        assertEquals(120, getArc(ZERO_AQUARII, ZERO_GEMINI));
        assertEquals(60, getArc(ZERO_GEMINI, ZERO_ARIETIS));
    }

    @Test
    void getVectorArc_getsVectorArc() {
        assertEquals(60, getVectorArc(ZERO_ARIETIS, ZERO_GEMINI));
        assertEquals(300, getVectorArc(ZERO_GEMINI, ZERO_ARIETIS));
    }
    @Test
    void getVectorArc_getsVectorArc_over_0() {
        assertEquals(120, getVectorArc(ZERO_AQUARII, ZERO_GEMINI));
        assertEquals(300, getVectorArc(ZERO_SCORPII, ZERO_VIRGINIS));
    }

    @Test
    void normalizeArc() {
    }

    @Test
    void arrangeAsChain() {
    }

    @Test
    void calculateAvg() {
        assertEquals(150, CelestialMechanics.calculateAvg(ZERO_VIRGINIS));
        assertEquals(75, CelestialMechanics.calculateAvg(ZERO_ARIETIS, ZERO_VIRGINIS));
        assertEquals(220, CelestialMechanics.calculateAvg(ZERO_SCORPII, ZERO_VIRGINIS, ZERO_AQUARII));

    }
}