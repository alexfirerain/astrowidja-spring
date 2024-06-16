package ru.swetophor.astrowidjaspring.model;

import lombok.Getter;
import ru.swetophor.astrowidjaspring.utils.CelestialMechanics;

import java.util.List;

import static java.lang.Math.floor;
import static ru.swetophor.astrowidjaspring.model.Harmonics.findMultiplier;
import static ru.swetophor.astrowidjaspring.utils.Mechanics.secondFormat;

/**
 * Некоторая установленная для реальной дуги гармоническая кратность.
 * Аспект характеризуется математическим определением резонанса
 * (основное гармоническое число и множитель повторения кратности)
 * и степенью точности (добротности) резонанса, определяемой как близость
 * реального небесного расстояния к дуге точного аспекта.
 */
@Getter
public class Aspect {
    /**
     * Гармоника, в которой аспект предстаёт соединением.
     * Т.е. число, на которое делится Круг, чтобы получить
     * дугу единичного резонанса для данной гармоники.
     * Иначе говоря, резонансное число аспекта, или номер гармоники.
     * Например, дугам в 45° и 135° соответствует резонансное число 8.
     */
    private final int numeric;
    /**
     * Множитель дальности, или повторитель кратности. Т.е. то число, на которое
     * нужно умножить дугу единичного резонанса этой гармоники, чтоб получить
     * дугу чистого неединичного аспекта.
     * Например, дуга в 45° имеет множитель 1, а дуга в 135° — множитель 3.
     * Резонансное число не должно быть кратно множителю,
     * например вместо аспекта 2/8 берётся аспект 1/4.
     */
    private final int multiplicity;

    /**
     * Разность фактической дуги между астрами с дугой чистого аспекта,
     * экзакта, вычисляемой как {@code (360° / гармоника) * множитель}.
     * Т.е. эффективный орбис аспекта.
     */
    private final double clearance;

    /**
     * Эффективный орбис аспекта, выраженный в %-ах, где 100% означает полное
     * совпадение реальной дуги с математическим аспектом (экзакт, эффективный орбис 0°),
     * а 0% означает совпадение эффективного орбиса с предельным для данной гармоники.
     * Иначе говоря, точность аспекта, выраженная в %-ах.
     */
    private final double strength;

    /**
     * Глубина аспекта, т.е. его точность, выраженная через количество
     * бóльших кратных гармоник, через которые он проходит, сохраняется.
     *  Глубина равна 0, если аспект отсутствует;
     *  равна 1, если аспект присутствует только в этой гармонике;
     *  равна n, если соединение присутствует в гармониках кратностью до n от данной.
     */
    private final int depth;

    /**
     * Конструктор аспекта, т.е. одного из резонансов в дуге.
     * @param numeric      гармоника, т.е. кратность дуги Кругу.
     * @param clearance     эффективный орбис в карте гармоники.
     * @param fromArc   реальная дуга, в которой распознан этот резонанс.
     * @param orb   первичный орб для соединений, используемый при определении резонансов.
     *              При этом если используется сокращение орба для синастрий (по умолчанию),
     *              сюда передаётся уже сокращённое значение.
     */
    public Aspect(int numeric, double clearance, double fromArc, double orb) {
        this.numeric = numeric;
        this.multiplicity = findMultiplier(numeric, fromArc, orb);
        this.clearance = clearance;
        this.strength = CelestialMechanics.calculateStrength(orb, clearance);
        this.depth = (int) floor(orb / clearance);
    }
    /**
     * Выдаёт список простых множителей, в произведении дающих
     * число резонанса данного аспекта.
     * @return  список множителей гармоники, каждый из которых является простым числом.
     */
    public List<Integer> getMultipliers() {
        return Harmonics.multipliersExplicate(numeric);
    }

    /**
     * Сообщает, кратно ли резонансное число этого аспекта данному числу.
     * То есть присутствует ли указанное число среди множителей, дающих
     * резонансное число аспекта.
     * @param baseHarmonic проверяемый на кратность множитель.
     * @return  {@code true}, если резонансное число аспекта кратно данному.
     */
    public boolean hasMultiplier(int baseHarmonic) {
        return getMultipliers().contains(baseHarmonic);
    }

    /**
     * Выводит строковую характеристику, насколько точен резонанс.
     * Та же градация, что для рейтинга в виде звёздочек:
     * <p>{@code приблизительный}  < 50% - присутствует только в данной гармонике</p>
     * <p>{@code уверенный}       50-66% - присутствует в данной и в следующей х2</p>
     * <p>{@code глубокий}        67-83% - сохраняется ещё в гармониках х3, х4 и х5</p>
     * <p>{@code точный}          84-92% - сохраняется до гармоник х12</p>
     * <p>{@code глубоко точный}  93-96% - сохраняется до х24 гармоник</p>
     * <p>{@code крайне точный}   > 96 % - присутствует в нескольких десятках кратных гармоник</p>
     * @return строковое представление ранга точности.
     */
    public String getStrengthLevel() {
        if (depth <= 1) return "- приблизительный ";
        else if (depth == 2) return "- уверенный ";
        else if (depth <= 5) return "- глубокий ";
        else if (depth <= 12) return "- точный ";
        else if (depth <= 24) return "- глубоко точный ";
        else return "- крайне точный ";
    }

    /**
     * Рейтинг силы, выраженный строкой со звёздочками,
     * та же градация, что для строковой характеристики:
     * <p>★        < 50% - присутствует только в данной гармонике</p>
     * <p>★★       50-66% - присутствует в данной и в следующей х2</p>
     * <p>★★★     67-83% - сохраняется ещё в гармониках х3, х4 и х5</p>
     * <p>★★★★    84-92% - сохраняется до гармоник х12</p>
     * <p>★★★★★   93-96% - сохраняется до х24 гармоник</p>
     * <p>✰✰✰✰✰   > 96 % - присутствует в нескольких десятках кратных гармоник</p>
     *
     * @return звездообразный код рейтинга силы согласно соответствию.
     */
    public String strengthRating() {
        if (depth <= 1) return "★";
        if (depth == 2) return "★★";
        if (depth <= 5) return "★★★";
        if (depth <= 12) return "★★★★";
        if (depth <= 24) return "★★★★★";
        return "✰✰✰✰✰";
    }

    /**
     * Сообщает, присутствует ли данный аспект (т.е. связь по этому
     * резонансному числу) в карте указанной гармоники.
     * Например, мы имеем аспект трин (numeric = 3) — он сохраняется в секстиле (6-й гармонике),
     * если глубина аспекта >= 2, а если сила аспекта < 50% (т.е. зазор больше половины орбиса),
     * то не сохраняется. В полусекстиле (12-й гармонике) он же сохраняется при глубине >= 4,
     * а если сила аспекта < 25% (т.е. зазор больше четверти, т.е., для трина при первичном
     * орбисе 12°, больше 1°), то нет.
     * @param harmonic номер гармоники, относительно которой проверяется актуальность аспекта.
     * @return {@code true}, если резонансное число аспекта кратно запрашиваемой гармонике,
     * и он достаточно силён, чтобы проявиться в ней. И обратное в ином случае.
     */
    public boolean hasResonance(int harmonic) {
        return harmonic % numeric == 0 && harmonic / numeric <= depth;
    }

    @Override
    public String toString() {
        return "Резонанс %d %s- %s как %d (%.2f%%, %s)%n".formatted(
                numeric,
                multiplicity > 1 ? "(x%d) ".formatted(multiplicity) : "",
                getStrengthLevel(),
                depth,
                strength,
                secondFormat(clearance, true));
    }
}