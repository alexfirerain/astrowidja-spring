package ru.swetophor.astrowidjaspring.exception;

/**
 * Вызывается, чтобы указать, что при чтении .daw обнаружено
 * нарушение конвенционального формата.
 */
public class FileFormatException extends IllegalArgumentException {
    public FileFormatException(String message) {
        super(message);
    }
}
