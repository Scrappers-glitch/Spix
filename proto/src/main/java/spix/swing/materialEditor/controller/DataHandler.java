package spix.swing.materialEditor.controller;

import com.jme3.material.TechniqueDef;
import com.jme3.shader.*;
import spix.swing.materialEditor.utils.MaterialDefUtils;

import java.util.*;

import static com.jme3.shader.Shader.ShaderType.Fragment;

/**
 * Created by Nehon on 22/05/2016.
 */
public class DataHandler {

    private TechniqueDef currentTechnique;
    //convenience map for quick access to the data
    private Map<String, ShaderNode> nodes = new HashMap<>();
    private Map<String, VariableMapping> mappings = new HashMap<>();

    void addMapping(VariableMapping mapping){
        getMappingList(mapping).add(mapping);
        registerMapping(mapping);
     //   System.err.println("new Mapping: " + mapping.toString());
    }

    public void registerMapping(VariableMapping mapping){
        String key = MaterialDefUtils.makeConnectionKey(
                mapping.getRightVariable().getNameSpace(),
                mapping.getRightVariable().getName(),
                mapping.getLeftVariable().getNameSpace(),
                mapping.getLeftVariable().getName(),
                currentTechnique.getName());
        mappings.put(key, mapping);
    }

    void removeMappingForKey(String key){
        VariableMapping mapping = getMappingForKey(key);
        getMappingList(mapping).remove(mapping);
        mappings.remove(key);
     //   System.err.println("removed Mapping: " + mapping.toString());
    }

    private List<VariableMapping> getMappingList(VariableMapping mapping) {
        ShaderNodeVariable lv = mapping.getLeftVariable();
        ShaderNodeVariable rv = mapping.getRightVariable();

        if(lv.getNameSpace().equals("Global")){
            String key = MaterialDefUtils.makeShaderNodeKey(currentTechnique.getName(), rv.getNameSpace());
            ShaderNode node = nodes.get(key);
            if(node == null){
                throw new IllegalArgumentException("Can't find node for key " + key);
            }

            return node.getOutputMapping();
        } else {
            String key = MaterialDefUtils.makeShaderNodeKey(currentTechnique.getName(), lv.getNameSpace());
            ShaderNode node = nodes.get(key);
            if(node == null){
                throw new IllegalArgumentException("Can't find node for key " + key);
            }

            return node.getInputMapping();

        }
    }

    public void registerShaderNode(ShaderNode node){
        String key = MaterialDefUtils.makeShaderNodeKey(currentTechnique.getName(), node.getName());
        nodes.put(key, node);
    }

    public void removeShaderNodeForKey(String key){
        ShaderNode node = getShaderNodeForKey(key);
        nodes.remove(key);
        currentTechnique.getShaderNodes().remove(node);
    }

    public VariableMapping getMappingForKey(String key){
        return mappings.get(key);
    }

    public ShaderNode getShaderNodeForKey(String key){
        return nodes.get(key);
    }


    public TechniqueDef getCurrentTechnique() {
        return currentTechnique;
    }

    public void setCurrentTechnique(TechniqueDef currentTechnique) {
        this.currentTechnique = currentTechnique;
    }
}
