package spix.swing.materialEditor.sort;

import java.util.*;

import static spix.swing.materialEditor.icons.Icons.node;

/**
 * Created by Nehon on 30/05/2016.
 */
public class TopologicalSort {


    public static Deque<Node> sort(List<Node> allNodes) {
        Set<Node> visited = new HashSet<>();
        Deque<Node> stack = new ArrayDeque<>();

        for (Node node : allNodes) {
            if (!visited.contains(node)) {
                sortNodeGraph(node, visited, stack);
            }
        }

        return stack;
    }

    private static void sortNodeGraph(Node root, Set<Node> visited, Deque<Node> stack) {
        visited.add(root);

        for (Node node : root.getChildren()) {
            if (!visited.contains(node)) {
                sortNodeGraph(node, visited, stack);
            }
        }
        stack.offerFirst(root);

    }




}
