package ru.swetophor.astrowidjaspring.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Утилита для создания рамок вокруг текста.
 *
 * @author swetophor
 */
public class Decorator {

    /**
     * Константный массив звёздочек для создания звёздной рамки.
     */
    public static final char[] ASTERISK_FRAME = {'*'};
    /**
     * Константный массив символов одинарной рамки.
     */
    public static final char[] SINGULAR_FRAME = {'┌', '─', '┐', '│', '└', '┘'};
    /**
     * Константный массив символов двойной рамки.
     */
    public static final char[] DOUBLE_FRAME = {'╔', '═', '╗', '║', '╚', '╝'};
    /**
     * Константный массив символов полудвойной рамки.
     */
    public static final char[] HALF_DOUBLE_FRAME = {'┌', '─', '╖', '│', '║', '╘', '═', '╝'};

    /**
     * Создаёт рамку вокруг заданного текста с указанными параметрами.
     *
     * @param text          обрамляемый текст.
     * @param minWidth      минимальная ширина рамки.
     * @param maxWidth      максимальная ширина рамки.
     * @param leftTop        символ левого верхнего угла рамки.
     * @param horizontal      символ горизонтальной линии рамки.
     * @param rightTop       символ правого верхнего угла рамки.
     * @param vertical        символ вертикальной линии рамки.
     * @param leftBottom     символ левого нижнего угла рамки.
     * @param rightBottom    символ правого нижнего угла рамки.
     * @return строковое представление обрамлённого текста.
     */
    public static String frameText(String text,
                                   int minWidth,
                                   int maxWidth,
                            char leftTop,
                            char horizontal,
                            char rightTop,
                            char vertical,
                            char leftBottom,
                            char rightBottom) {
        String[] lines = text.lines().toArray(String[]::new);
        StringBuilder output = new StringBuilder();

        int width = Math.min(maxWidth,
                Math.max(minWidth,
                        Arrays.stream(lines)
                                .mapToInt(String::length)
                                .max()
                                .orElse(0)));

        int framedWidth = width + 4;
        output.append(buildBorderString(leftTop, horizontal, rightTop, framedWidth));

        for (String line : lines) {
            while (line.length() > width) {
                output.append(buildMidleString(vertical, line.substring(0, width), framedWidth));
                line = line.substring(width);
            }
            output.append(buildMidleString(vertical, line, framedWidth));
        }

        output.append(buildBorderString(leftBottom, horizontal, rightBottom, framedWidth));

        return output.toString();
    }

    public static String frameText(String text, int minWidth, char symbol) {
        return frameText(text, minWidth, 80, symbol, symbol, symbol, symbol, symbol, symbol);
    }

    public static String frameText(String text, int minWidth, int maxWidth, char... pattern) {
        return switch (pattern.length) {
            case 1 -> frameText(text, minWidth, pattern[0]);
            case 6 -> frameText(text, minWidth, maxWidth,
                    pattern[0], pattern[1], pattern[2],
                    pattern[3], pattern[4], pattern[5]);
            case 8 -> frameText(text, minWidth, maxWidth,
                    pattern[0], pattern[1], pattern[2],
                    pattern[3], pattern[4], pattern[5],
                    pattern[6], pattern[7]);
            default -> throw new IllegalArgumentException("Некорректная длинна паттерна рамки.");
        };
    }

    private static String complementString(String string, int length) {
        int lack = length - string.length();
        return lack <= 0 ?
                string :
                string + " ".repeat(lack);
    }

    /**
     * Выдаёт строку указанной длины, составленную как верхняя или нижняя
     * граница обрамлённого текста. Особо указываются первый и последний
     * символы и символы заполнения.
     *
     * @param beginning первый символ строки.
     * @param body      символ, из повторов которого составлена основная часть строки.
     * @param ending    заключительный символ строки.
     * @param length    желаемая полная длина строки.
     * @return сформированную оговорённым способом строку.
     */
    private static String buildBorderString(char beginning,
                                            char body,
                                            char ending,
                                            int length) {
        return "%s%s%s%n"
                .formatted(beginning,
                        String.valueOf(body).repeat(length - 2),
                        ending);
    }

    private static String frameText(String text, int minWidth, int maxWidth,
                                    char leftTop, char upperHorizontal, char rightTop,
                                    char leftVertical, char rightVertical,
                                    char leftBottom, char lowerHorizontal, char rightBottom) {
        String[] lines = text.lines().toArray(String[]::new);
        StringBuilder output = new StringBuilder();

        int width = Math.min(maxWidth,
                Math.max(minWidth,
                        Arrays.stream(lines)
                                .mapToInt(String::length)
                                .max()
                                .orElse(0)));

        output.append(buildBorderString(leftTop, upperHorizontal, rightTop, width + 4));

        for (String line : lines) {
            while (line.length() > width) {
                output.append(buildMidleString(leftVertical, rightVertical, line.substring(0, width), width + 4));
                line = line.substring(width);
            }
            output.append(buildMidleString(leftVertical, rightVertical, line, width + 4));
        }

        output.append(buildBorderString(leftBottom, lowerHorizontal, rightBottom, width + 4));

        return output.toString();
    }

    /**
     * Выдаёт строку указанной длины, составленную как строка обрамлённого текста
     * с одинаковыми символами для левой и правой декоративной границы.
     *
     * @param border рамочный символ, помещаемый в начало и конец строки.
     * @param text   текст обрамляемой строки.
     * @param length желаемая полная длина строки.
     * @return сформированную оговорённым способом строку.
     */
    private static String buildMidleString(char border, String text, int length) {
        return buildMidleString(border, border, text, length);
    }

    /**
     * Выдаёт строку указанной длины, составленную как строка обрамлённого текста
     * с различным символом для левой и правой декоративной границы.
     *
     * @param leftBorder  рамочный символ, помещаемый в начало строки.
     * @param rightBorder рамочный символ, помещаемый в конец строки.
     * @param text        текст обрамляемой строки.
     * @param length      желаемая полная длина строки.
     * @return сформированную оговорённым способом строку.
     */
    private static String buildMidleString(char leftBorder, char rightBorder, String text, int length) {
        return "%s %s %s%n"
                .formatted(leftBorder,
                        complementString(text, length - 4),
                        rightBorder);
    }

    public static String concatenateTables(String... tables) {
        if (tables == null || tables.length == 0)
            throw new IllegalArgumentException();
        if (tables.length == 1)
            return tables[0];

        StringBuilder output = new StringBuilder();

        List<List<String>> stuff = new ArrayList<>(Arrays.stream(tables)
                .map(table -> new ArrayList<>(table.lines().toList()))
                .toList());

        int ceilingTableLength = stuff.stream()
                .mapToInt(List::size)
                .max().orElse(0);

        for (int i = 0; i < stuff.size(); i++) {
            var list = stuff.get(i);
            if (list.size() < ceilingTableLength)
                stuff.set(i, stretchTable(list, ceilingTableLength));
        }

        for (int i = 0; i < ceilingTableLength; i++) {
            for (int j = 0; j < stuff.size(); j++) {
                String str = stuff.get(j).get(i);
                output.append(j == 0 ? str : str.substring(1));
            }
            output.append("\n");
        }

        return output.toString();
    }

    private static List<String> stretchTable(List<String> table, int ceilingTableLength) {
        if (table.size() < 3 || ceilingTableLength < 3) throw new IllegalArgumentException("Минимальная высота таблицы = 3");
        int fillerCount = ceilingTableLength - table.size();
        if (fillerCount < 0) throw new IllegalArgumentException("Таблица уже длиннее, чем " + ceilingTableLength);

        if (fillerCount > 0) {
            String middleLine = table.get(1);
            if (middleLine.length() < 5) throw new IllegalArgumentException("Минимальная ширина таблицы = 5");
            int length = middleLine.length();
            String filler = middleLine.charAt(0) + " ".repeat(length - 2) + middleLine.charAt(length - 1);
            IntStream.range(0, fillerCount)
                    .forEach(_ -> table.add(table.size() - 1, filler));
        }
        return table;
    }

    /*
        Фасадные функции.
     */
    public static String singularFrame(String text) {
        return frameText(text, 30, 90, SINGULAR_FRAME);
    }

    public static String halfDoubleFrame(String text) {
        return frameText(text, 30, 90, HALF_DOUBLE_FRAME);
    }

    public static String doubleFrame(String text) {
        return frameText(text, 30, 90, DOUBLE_FRAME);
    }

    public static String asteriskFrame(String text) {
        return frameText(text, 30, 90, ASTERISK_FRAME);
    }


    public static void print(String text) {
        System.out.println(text);
    }

    public static void console(String text) {
        System.out.print(text);
    }

    public static void print() {
        System.out.println();
    }

    public static void printInAsterisk(String text) {
        System.out.println(asteriskFrame(text));
    }

    public static void printInFrame(String text) {
        System.out.println(singularFrame(text));
    }

    public static void printInDoubleFrame(String text) {
        System.out.println(doubleFrame(text));
    }

    public static void printInSemiDouble(String text) {
        System.out.println(halfDoubleFrame(text));
    }
}
