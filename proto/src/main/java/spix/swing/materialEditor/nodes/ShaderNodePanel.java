package spix.swing.materialEditor.nodes;

import com.jme3.shader.*;
import spix.swing.materialEditor.controller.MatDefEditorController;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by Nehon on 13/05/2016.
 */
public abstract class ShaderNodePanel extends NodePanel implements Editable {

    private boolean editionAllowed = true;
    private List<String> filePaths = new ArrayList<>();

    private ShaderNodePanel(MatDefEditorController controller, String key, ShaderNode shaderNode, Color color, Icon icon){
        super(controller, key, color, icon);
        ShaderNodeDefinition def = shaderNode.getDefinition();

//        node.addPropertyChangeListener(WeakListeners.propertyChange(this, node));
//        this.addPropertyChangeListener(WeakListeners.propertyChange(node, this));
       // refresh(node);
        setNodeName(shaderNode.getName());
        this.filePaths.addAll(def.getShadersPath());
        String defPath = def.getPath();
        this.filePaths.add(defPath);
        init(def.getInputs(), def.getOutputs());
    }

    @Override
    protected void initHeader(JLabel header) {
        header.setText(getNodeName());
        header.setToolTipText(getNodeName());
    }

    public void edit() {
        if(editionAllowed) {
            //TODO pop an edition window
        }
    }

    @Override
    public void setEditionAllowed(boolean editionAllowed) {
        this.editionAllowed = editionAllowed;
    }

    @Override
    public boolean isEditionAllowed() {
        return editionAllowed;
    }

    //    public final void refresh(ShaderNode node) {
//        nodeName = node.getName();
//        header.setText(node.getName());
//        header.setToolTipText(node.getName());
//
//    }

    public static ShaderNodePanel create(MatDefEditorController controller, String key,  ShaderNode shaderNode){
        Color color = new Color(0,0,0);
        Icon icon = Icons.node;
        Shader.ShaderType type = shaderNode.getDefinition().getType();
        switch (type){
            case Vertex:
                color = new Color(220, 220, 70);
                icon = Icons.vert;
                break;
            case Fragment:
                color = new Color(114, 200, 255);
                icon = Icons.frag;
                break;
            case Geometry:
                color = new Color(250, 150, 0);
                break;
            case TessellationControl:
                color = new Color(150, 100, 255);
                break;
            case TessellationEvaluation:
                color = new Color(250, 150, 255);
                break;
        }

        return new ShaderNodePanel(controller, key, shaderNode, color, icon) {
            @Override
            public Shader.ShaderType getShaderType() {
                return type;
            }
        };
    }
}
