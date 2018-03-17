package spix.swing.materialEditor.nodes;

import com.jme3.shader.*;
import spix.swing.materialEditor.Dot;
import spix.swing.materialEditor.controller.MatDefEditorController;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Created by Nehon on 13/05/2016.
 */
public abstract class ShaderNodeGroup extends NodePanel implements Editable {

    private boolean editionAllowed = false;

    private ShaderNodeGroup(MatDefEditorController controller, String groupName, List<ShaderNodePanel> shaderNodes) {
        super(controller, "group." + groupName, Color.BLUE, Icons.edit);

        setNodeName(groupName);

        for (ShaderNodePanel shaderNode : shaderNodes) {

            for (String key : shaderNode.inputDots.keySet()) {
                Dot d = shaderNode.inputDots.get(key);
                Dot nd = createDot(d.getType(), d.getParamType(), d.getVariableName(), d.getNodeName());
                String newKey = key + " (" + shaderNode.getName() + ")";
                JLabel l = createLabel(d.getType(), newKey, d.getParamType());
                inputDots.put(newKey, nd);
                inputLabels.add(l);
            }

            for (String key : shaderNode.outputDots.keySet()) {
                Dot d = shaderNode.outputDots.get(key);
                Dot nd = createDot(d.getType(), d.getParamType(), d.getVariableName(), d.getNodeName());

                String newKey = "(" + shaderNode.getName() + ") " + key;
                JLabel l = createLabel(d.getType(), newKey, d.getParamType());
                outputDots.put(newKey, nd);
                outputLabels.add(l);
            }

        }
       // init();
    }

    @Override
    protected void initHeader(JLabel header) {
        header.setText(getNodeName());
        header.setToolTipText(getNodeName());
    }

    public void edit() {
        if (editionAllowed) {
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

    public static ShaderNodeGroup create(MatDefEditorController controller, String groupName, List<ShaderNodePanel> shaderNodes) {

        return new ShaderNodeGroup(controller, groupName, shaderNodes) {
            @Override
            public Shader.ShaderType getShaderType() {
                return null;
            }
        };
    }


}
