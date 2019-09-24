package ru.inovus.ms.rdm.api.util;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    private static final String DOUBLE_QUOTE_CHAR = "\"";
    private static final String SINGLE_QUOTE_CHAR = "\"";

    private StringUtils() {
    }

    public static String addDoubleQuotes(String value) {
        return DOUBLE_QUOTE_CHAR + value + DOUBLE_QUOTE_CHAR;
    }

    public static String addSingleQuotes(String value) {
        return SINGLE_QUOTE_CHAR + value + SINGLE_QUOTE_CHAR;
    }

    /**
     * Разбить строку s вида "[ fgfsdgsdf, fasfa, f    fadsf , fasdfasdf , fasdfasd    ,  ]
     * на токены: ["fgfsdgsdf", "fasfa", "f    fadsf", "fasdfasdf", "fasdfasd", ""].
     * Пустые строки допускаются. Все ведущие пробелы будут удалены.
     * @param s Исходная строка
     * @return Токены, разбитые по запятой
     */
    public static List<String> splitStripSpaces(String s) {
        List<String> l = new ArrayList<>();
        for (int i = 1; i <= s.length() - 1;) {
            while (i < s.length() - 1 && s.charAt(i) == ' ')
                i++;
            if (i >= s.length() - 1) {
                l.add("");
                break;
            }
            int j = s.indexOf(',', i);
            if (j == -1)
                j = s.length() - 1;
            int k = j - 1;
            while (k >= i && s.charAt(k) == ' ')
                k--;
            l.add(s.substring(i, k + 1));
            i = j + 1;
        }
        return l;
    }

}
