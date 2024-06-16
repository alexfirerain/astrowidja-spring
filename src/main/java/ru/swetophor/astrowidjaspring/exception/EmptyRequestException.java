package ru.swetophor.astrowidjaspring.exception;

public class EmptyRequestException extends ChartNotFoundException {
    public EmptyRequestException(String problem) {
        super(problem);
    }
}
