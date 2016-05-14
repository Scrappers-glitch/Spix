package spix.swing.materialEditor.nodes.shadernodes;

import com.jme3.shader.ShaderNode;
import com.jme3.shader.ShaderNodeDefinition;
import spix.swing.materialEditor.nodes.NodePanel;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Nehon on 13/05/2016.
 */
public abstract class ShaderNodePanel extends NodePanel{

    public ShaderNodePanel(ShaderNode shaderNode, Color color){
        super(color);
        ShaderNodeDefinition def = shaderNode.getDefinition();
        shaderType = def.getType();
        init(def.getInputs(), def.getOutputs());

//        node.addPropertyChangeListener(WeakListeners.propertyChange(this, node));
//        this.addPropertyChangeListener(WeakListeners.propertyChange(node, this));
       // refresh(node);
        setNodeName(shaderNode.getName());
        this.filePaths.addAll(def.getShadersPath());
        String defPath = def.getPath();
        this.filePaths.add(defPath);
    }

    @Override
    protected void initHeader(JLabel header) {
        header.setText(getNodeName());
        header.setToolTipText(getNodeName());
    }

    //    public final void refresh(ShaderNode node) {
//        nodeName = node.getName();
//        header.setText(node.getName());
//        header.setToolTipText(node.getName());
//
//    }
}
