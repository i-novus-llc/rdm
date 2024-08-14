package ru.i_novus.ms.rdm.service;

import java.util.Comparator;

/**
 * Comparator to order script files by filename only.
 * <p>
 * Based on {@link liquibase.changelog.DatabaseChangeLog#getStandardChangeLogComparator() getStandardChangeLogComparator}
 */
public class LiquibaseChangelogComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {

        // by ignoring WEB-INF/classes in path all changelog Files independent
        // whether they are in a WAR or in a JAR are order following the same rule
        return prepareId(o1).compareTo(prepareId(o2));
    }

    private static String prepareId(String id) {
        return id.replaceFirst("^/", "")
                .replace("WEB-INF/classes/", "");
    }
}
