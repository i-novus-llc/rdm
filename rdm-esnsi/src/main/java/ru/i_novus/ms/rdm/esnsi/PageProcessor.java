package ru.i_novus.ms.rdm.esnsi;

public class PageProcessor {

    private final String id;
    private final int seed;

    PageProcessor(String id, int seed) {
        this.id = id;
        this.seed = seed;
    }

    public String fullId() {
        return id;
    }

    public int id() {
        return Integer.parseInt(id.substring(id.lastIndexOf('-') + 1));
    }

    public int seed() {
        return seed;
    }

}
