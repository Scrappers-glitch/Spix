package spix.swing.materialEditor.sort;

import com.jme3.shader.Shader;

import java.util.*;

/**
 * Created by Nehon on 30/05/2016.
 */
public class Node {

    String key;
    Shader.ShaderType type;
    String name;
    Set<Node> children = new HashSet<>();
    Set<Node> parents = new HashSet<>();
    Set<Node> flattenParents = new HashSet<>();
    boolean highPriority = false;

    public Node(String key, Shader.ShaderType type) {
        this.key = key;
        this.type = type;
        this.name = key.substring(key.lastIndexOf(".") + 1);
    }

    public void addParent(Node node) {
        parents.add(node);
    }

    public void addChild(Node node) {
        children.add(node);
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public void flattenParents() {
        flattenParents.clear();
        visitParents(this);
    }

    private void visitParents(Node node) {
        for (Node parent : node.parents) {
            if (flattenParents.contains(parent)) {
                return;
            }
            flattenParents.add(parent);
            visitParents(parent);
        }
    }

    public void dumpParents() {
        for (Node node : parents) {
            System.err.print(node.getName() + ", ");
        }
        System.err.println("");
        for (Node node : flattenParents) {
            System.err.print(node.getName() + ", ");
        }
        System.err.println("");
    }

    public Shader.ShaderType getType() {
        return type;
    }

    public Set<Node> getChildren() {
        return children;
    }

    public boolean isHighPriority() {
        return highPriority;
    }

    public void setHighPriority(boolean highPriority) {
        this.highPriority = highPriority;
    }

    public boolean hasParent(Node n) {
        return flattenParents.contains(n) || parents.contains(n);

    }

    public String toString2() {
        String res = "Node : \n" +
                "key='" + key + '\'' +
                ", type=" + type +
                ",\n children=";
        for (Node node : children) {
            res += node.getKey() + ", ";
        }
        res += ",\n parents=";
        for (Node node : parents) {
            res += node.getKey() + ", ";
        }
        return res;
    }

    @Override
    public String toString() {
        String res = key;
        res += "\n parents=";
        for (Node node : children) {
            res += node.getName() + ", ";
        }
        return res;
    }
}
