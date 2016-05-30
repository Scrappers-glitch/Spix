package spix.swing.materialEditor.sort;

import java.util.*;

import static spix.swing.materialEditor.icons.Icons.node;

/**
 * Created by Nehon on 30/05/2016.
 */
public class TopologicalSort {



    public static Deque<String> sort(List<Node> allNodes) {
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();

        for (Node node : allNodes) {
            if(!visited.contains(node.getKey())){
                sortNodeGraph(node, visited, stack);
            }
        }

        return stack;
    }

    private static void sortNodeGraph(Node root,Set<String> visited, Deque<String> stack){
        visited.add(root.getKey());

        for (Node node : root.getChildren()) {
            if(! visited.contains(node.getKey())){
                sortNodeGraph(node, visited, stack);
            }
        }
        stack.offerFirst(root.getKey());

    }




}
