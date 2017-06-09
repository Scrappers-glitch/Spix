package spix.swing.materialEditor.controller;

import com.jme3.material.*;
import com.jme3.shader.*;
import com.jme3.texture.image.ColorSpace;
import spix.swing.materialEditor.sort.*;
import spix.swing.materialEditor.utils.MaterialDefUtils;

import java.util.*;

import static com.jme3.shader.Shader.ShaderType.Fragment;
import static com.jme3.shader.Shader.ShaderType.Vertex;

/**
 * Created by Nehon on 22/05/2016.
 */
public class DataHandler {

    private TechniqueDef currentTechnique;
    private MaterialDef currentMatDef;
    //convenience map for quick access to the data
    private Map<String, ShaderNode> nodes = new HashMap<>();
    private Map<String, VariableMapping> mappings = new HashMap<>();

    void addMapping(VariableMapping mapping) {
        getMappingList(mapping).add(mapping);
        registerMapping(mapping);
        //   System.err.println("new Mapping: " + mapping.toString());
    }

    public void registerMapping(VariableMapping mapping) {
        String key = MaterialDefUtils.makeConnectionKey(
                mapping.getRightVariable().getNameSpace(),
                mapping.getRightVariable().getName(),
                mapping.getLeftVariable().getNameSpace(),
                mapping.getLeftVariable().getName(),
                currentTechnique.getName());
        mappings.put(key, mapping);
    }

    void removeMappingForKey(String key) {
        VariableMapping mapping = getMappingForKey(key);
        getMappingList(mapping).remove(mapping);
        mappings.remove(key);
        //   System.err.println("removed Mapping: " + mapping.toString());
    }

    private List<VariableMapping> getMappingList(VariableMapping mapping) {
        ShaderNodeVariable lv = mapping.getLeftVariable();
        ShaderNodeVariable rv = mapping.getRightVariable();

        if (lv.getNameSpace().equals("Global")) {
            String key = MaterialDefUtils.makeShaderNodeKey(currentTechnique.getName(), rv.getNameSpace());
            ShaderNode node = nodes.get(key);
            if (node == null) {
                throw new IllegalArgumentException("Can't find node for key " + key);
            }

            return node.getOutputMapping();
        } else {
            String key = MaterialDefUtils.makeShaderNodeKey(currentTechnique.getName(), lv.getNameSpace());
            ShaderNode node = nodes.get(key);
            if (node == null) {
                throw new IllegalArgumentException("Can't find node for key " + key);
            }

            return node.getInputMapping();

        }
    }

    public void addWorldParm(UniformBinding binding) {
        currentTechnique.getWorldBindings().add(binding);
    }

    public void removeWorldParam(String key) {
        key = key.substring(key.lastIndexOf(".") + 1);
        System.err.println(key);
        currentTechnique.getWorldBindings().remove(UniformBinding.valueOf(key));
    }

    public void addMatParam(ShaderNodeVariable var, String strType) {
        if (strType.equals("Color")) {
            strType = "Vector4";
        }
        VarType type = VarType.valueOf(strType);
        if (type.isTextureType()) {
            // TODO: 23/05/2016 We should ask the user for the color space (linear or srgb)
            currentMatDef.addMaterialParamTexture(type, var.getName(), ColorSpace.sRGB);
        } else {
            // TODO: 23/05/2016 We should ask the user for the default value.
            currentMatDef.addMaterialParam(type, var.getName(), null);
        }
    }

    public void removeMatParam(String key) {
        key = key.substring(key.lastIndexOf(".") + 1);
        System.err.println(key);
        MaterialDefUtils.removeParam(currentMatDef, key);
    }

    public void addShaderNode(ShaderNode node) {
        currentTechnique.getShaderNodes().add(node);
        registerShaderNode(node);
    }

    public void registerShaderNode(ShaderNode node) {
        String key = MaterialDefUtils.makeShaderNodeKey(currentTechnique.getName(), node.getName());
        nodes.put(key, node);
    }

    public void removeShaderNodeForKey(String key) {
        ShaderNode node = getShaderNodeForKey(key);
        nodes.remove(key);
        currentTechnique.getShaderNodes().remove(node);
    }

    public VariableMapping getMappingForKey(String key) {
        return mappings.get(key);
    }

    public ShaderNode getShaderNodeForKey(String key) {
        return nodes.get(key);
    }

    public void clear() {
        nodes.clear();
        mappings.clear();
    }

    public void setCurrentTechnique(TechniqueDef currentTechnique) {
        clear();
        this.currentTechnique = currentTechnique;
    }

    public TechniqueDef getCurrentTechnique() {
        return currentTechnique;
    }

    public void setCurrentMatDef(MaterialDef currentMatDef) {
        this.currentMatDef = currentMatDef;
    }

    public Deque<Node> sortNodes(List<Node> nodeList) {
        for (Node node : nodeList) {
            node.flattenParents();
        }

        for (Node node : nodeList) {
            //vertex nodes must be before any fragment node, so we add precedence
            if (node.getType() == Vertex) {
                for (Node node1 : nodeList) {
                    if (node1.getType() != Vertex) {
                        node.addChild(node1);
                        node1.addParent(node);
                    }
                }
            }
            //if the node has high priority it should be as soon as possible in the node list
            //so we set it as parent to any node that is not in his parent line in the same shader.
            if (node.isHighPriority()) {
                for (Node node1 : nodeList) {
                    if (node1.getType() == node.getType() && !node.hasParent(node1)) {
                        node.addChild(node1);
                        node1.addParent(node);
                    }
                }
            }
        }

//        for (Node sortNode : nodeList) {
//            System.err.println(sortNode);
//        }

        Deque<Node> sortedStack = TopologicalSort.sort(nodeList);

        List<ShaderNode> sortedNodes = new ArrayList<>();
        //System.err.println("----------------Node Order------------------");
        for (Node node : sortedStack) {
            node.flattenParents();
            ShaderNode n = nodes.get(node.getKey());
            if (n != null) {
                sortedNodes.add(n);
                //  System.err.println(node.getName());
            }
        }
        currentTechnique.setShaderNodes(sortedNodes);
        return sortedStack;
    }
}
