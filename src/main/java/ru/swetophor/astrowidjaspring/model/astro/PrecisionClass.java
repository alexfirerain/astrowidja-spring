package ru.swetophor.astrowidjaspring.model.astro;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PrecisionClass {
    /**
     * Сила аспекта < 0% - отсутствует в данной гармонике
     */
    NONE("- — ", "_"),
    /**
     * Сила аспекта < 50% - присутствует только в данной гармонике
     */
    APPROXIMATE("- приблизительный ", "★"),
    /**
     * Сила аспекта 50-66% - присутствует в данной и в следующей х2
     */
    CONFIDENT("- уверенный ", "★★"),
    /**
     * Сила аспекта 67-83% - сохраняется ещё в гармониках х3, х4 и х5
     */
    DEEP("- глубокий ", "★★★"),
    /**
     * Сила аспекта 84-92% - сохраняется до гармоник х12
     */
    ACCURATE("- точный ", "★★★★"),
    /**
     * Сила аспекта 93-96% - сохраняется до х24 гармоник
     */
    PRECISE("- глубоко точный ", "★★★★"),
    /**
     * Сила аспекта > 96 % - присутствует в нескольких десятках кратных гармоник
     */
    EXACT("- крайне точный ", "✰✰✰✰✰");

    private final String depthDesc;
    private final String rating;

}
