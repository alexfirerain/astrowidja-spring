package ru.swetophor.astrowidjaspring.model;

import lombok.Getter;
import lombok.Setter;
import ru.swetophor.astrowidjaspring.model.astro.Astra;
import ru.swetophor.astrowidjaspring.utils.Decorator;

@Getter
@Setter
public abstract class ChartObject {
    protected String name;

    public ChartObject(String name) {
        this.name = name;
    }

    public abstract Chart[] getData();

    public abstract int getDimension();

    /**
     * Выдаёт имя карты. Если оно длиннее указанного предела,
     * выдаёт его первые буквы и символ "…" в конце, так чтобы
     * общая длина строки была равна указанному пределу.
     *
     * @param limit максимальная длина возвращаемой строки.
     * @return имя карты, сокращённое до указанной длины.
     */
    public String getShortenedName(int limit) {
        return name.length() <= limit ?
                name :
                name.substring(0, limit - 1) + "…";
    }

    /**
     * Возвращает строку того формата, который принят для хранения
     * данных карты астры при сохранении.
     * В первой строке содержит "#" + имя карты.
     * В каждой последующей строке – положение очередной астры, как оно
     * предоставляется функцией {@link Astra#getString()}.
     * В конце также добавляется пустая строка для разделения.
     *
     * @return многостроку с названием карты и положением астр.
     */
    public abstract String getString();

    /**
     * Выдаёт заголовок.
     *
     * @return имя объекта карты в рамке.
     */
    public String getCaption() {
        return Decorator.frameText(name, 30, '*');
    }

    /**
     * Выдаёт список астр в этой карте или многокарте.
     * @return строку с готовым для отображения списком астр.
     */
    public abstract String getAstrasList();
}
