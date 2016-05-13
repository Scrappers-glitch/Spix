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
public class FragmentColorPanel extends NodePanel {

    public FragmentColorPanel() {
        super(new Color(220, 150, 0));//orange
        setNodeName("OutColor");
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
        header.setText("OutColor");
        header.setToolTipText("outColor");
        //setNodeName("Attr");
    }
}
