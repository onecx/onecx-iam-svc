package org.tkit.onecx.iam.domain.model;

public class Page {

    private final int number;

    private final int size;

    private Page(int number, int size) {
        this.number = number;
        this.size = size;
    }

    public int number() {
        return number;
    }

    public int size() {
        return size;
    }

    public static Page of(int number, int size) {
        return new Page(number, size);
    }

    @Override
    public String toString() {
        return "Page{" +
                "n=" + number +
                ",s=" + size +
                '}';
    }
}
