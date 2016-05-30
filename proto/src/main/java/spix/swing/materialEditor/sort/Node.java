package spix.swing.materialEditor.sort;

import com.jme3.shader.Shader;

import java.util.*;

/**
 * Created by Nehon on 30/05/2016.
 */
public class Node {

    String key;
    Shader.ShaderType type;
    Set<Node> children = new HashSet<>();
    Set<Node> parents = new HashSet<>();

    public Node(String key, Shader.ShaderType type) {
        this.key = key;
        this.type = type;
    }

    public void addParent(Node node){
        parents.add(node);
    }

    public void addChild(Node node){
        children.add(node);
    }

    public String getKey() {
        return key;
    }

    public Shader.ShaderType getType() {
        return type;
    }

    public Set<Node> getChildren() {
        return children;
    }

    public boolean hasParent(Node n){
        for (Node parent : parents) {
            if(parent == n){
                return true;
            } else {
                return parent.hasParent(n);
            }
        }
        return false;
    }

    @Override
    public String toString() {
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
}
