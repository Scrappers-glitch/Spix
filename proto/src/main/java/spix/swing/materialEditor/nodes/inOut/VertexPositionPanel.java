package spix.swing.materialEditor.nodes.inOut;

import com.jme3.shader.ShaderNodeVariable;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.nodes.NodePanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Nehon on 13/05/2016.
 */
public class VertexPositionPanel extends NodePanel {

    public VertexPositionPanel() {
        super(new Color(50,50,50));// dark gray
        setNodeName("Position");
        java.util.List<ShaderNodeVariable> outputs = new ArrayList<ShaderNodeVariable>();
        outputs.add(new ShaderNodeVariable("vec4","out"));
        java.util.List<ShaderNodeVariable> inputs = new ArrayList<ShaderNodeVariable>();
        inputs.add(new ShaderNodeVariable("vec4","in"));

        init(inputs, outputs);
    }

    @Override
    public String getKey() {
        return getNodeName();
    }

    @Override
    protected void initHeader(JLabel header) {
        header.setIcon(Icons.output);
        header.setText("Position");
        header.setToolTipText("Position");
        //setNodeName("Attr");
    }
}
