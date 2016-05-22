package spix.swing.materialEditor.nodes;

import com.jme3.shader.*;
import spix.swing.materialEditor.*;
import spix.swing.materialEditor.controller.MatDefEditorController;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.jme3.shader.Shader.ShaderType.Vertex;

/**
 * Created by bouquet on 15/05/16.
 */
public abstract class InOutPanel extends NodePanel {

    private InOutPanel(MatDefEditorController controller, String key, ShaderNodeVariable var, Color color, Icon icon){
        super(controller, key, color, icon);
        setNodeName(var.getName());
        List<ShaderNodeVariable> outputs = new ArrayList<ShaderNodeVariable>();
        outputs.add(new ShaderNodeVariable(var.getType(),var.getName()));
        List<ShaderNodeVariable> inputs = new ArrayList<ShaderNodeVariable>();
        inputs.add(new ShaderNodeVariable(var.getType(),var.getName()));

        init(inputs, outputs);
    }

    public boolean isInputAvailable(){
        return !inputDots.values().iterator().next().isConnected();
    }

    public boolean isOutputAvailable(){
        return !outputDots.values().iterator().next().isConnected();
    }

    public Dot getInputConnectPoint() {
        return inputDots.values().iterator().next();
    }

    public Dot getOutputConnectPoint() {
        return inputDots.values().iterator().next();
    }

    public static InOutPanel create(MatDefEditorController controller, String key, Shader.ShaderType type, ShaderNodeVariable var){
        Color color = new Color(0,0,0);
        switch (type){
            case Vertex:
                color = new Color(220, 220, 70);
                break;
            case Fragment:
                color = new Color(114, 200, 255);
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

        return new InOutPanel(controller, key, var, color, Icons.output) {
            @Override
            public Shader.ShaderType getShaderType() {
                return type;
            }

            @Override
            protected void initHeader(JLabel header) {
                header.setText(type.name() + " out");
                header.setToolTipText(type.name() + " shader output");
            }
        };
    }


}
