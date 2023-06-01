package ru.i_novus.ms.rdm.service;

import java.util.Comparator;

public class CustomComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        //by ignoring WEB-INF/classes in path all changelog Files independent
        //whehther they are in a WAR or in a JAR are order following the same rule
        return o1.replaceFirst("^\\/", "").replace("WEB-INF/classes/", "").compareTo(o2.replaceFirst("^\\/", "").replace("WEB-INF/classes/", ""));
    }
}
