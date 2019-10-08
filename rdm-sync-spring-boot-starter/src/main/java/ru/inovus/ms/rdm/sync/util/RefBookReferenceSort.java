package ru.inovus.ms.rdm.sync.util;

import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;

import java.util.*;

public class RefBookReferenceSort {

    private RefBookReferenceSort() {}

    public static List<String> getSortedCodes(List<RefBook> refbooks) {
        Map<String, DictionaryNode> m = new HashMap<>();
        for (RefBook refbook : refbooks)
            m.put(refbook.getCode(), new DictionaryNode());
        for (RefBook version : refbooks) {
            DictionaryNode node = m.get(version.getCode());
            Structure s = version.getStructure();
            for (Structure.Reference r : s.getReferences()) {
                String refTo = r.getReferenceCode();
                node.child.add(refTo);
            }
        }
        List<String> topologicalOrder = topologicalSort(m);
        LinkedList<String> inverseOrder = new LinkedList<>();
        for (String s : topologicalOrder) {
            inverseOrder.push(s);
        }
        return inverseOrder;
    }

    private static List<String> topologicalSort(Map<String, DictionaryNode> m) {
        Set<String> visited = new HashSet<>();
        LinkedList<String> stack = new LinkedList<>();
        for (Map.Entry<String, DictionaryNode> e : m.entrySet()) {
            if (!visited.contains(e.getKey())) {
                topologicalSort0(stack, visited, m, e.getKey());
            }
        }
        return stack;
    }

    private static void topologicalSort0(LinkedList<String> stack, Set<String> visited, Map<String, DictionaryNode> m, String code) {
        visited.add(code);
        for (String s : m.get(code).child) {
            if (!visited.contains(s))
                topologicalSort0(stack, visited, m, s);
        }
        stack.push(code);
    }

    private static class DictionaryNode {
        private Collection<String> child = new LinkedList<>();
    }

}
