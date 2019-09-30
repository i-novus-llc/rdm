package ru.inovus.ms.rdm.impl.util;

/**
 * Обертка над JSON строкой.
 * Указывает, что ее не нужно экранировать, а вставить как есть.
 */
public class JsonPayload {
    private final String json;
    public JsonPayload(String json) {this.json = json;}
    @Override public String toString() {return json;}
}
