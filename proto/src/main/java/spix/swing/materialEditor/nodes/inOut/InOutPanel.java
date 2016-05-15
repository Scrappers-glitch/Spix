package spix.swing.materialEditor.nodes.inOut;

import com.jme3.shader.ShaderNodeVariable;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.nodes.NodePanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Created by bouquet on 15/05/16.
 */
public abstract class InOutPanel extends NodePanel {

    public InOutPanel(ShaderNodeVariable var, Color color){
        super(color);
        setNodeName(var.getName());
        java.util.List<ShaderNodeVariable> outputs = new ArrayList<ShaderNodeVariable>();
        outputs.add(new ShaderNodeVariable(var.getType(),var.getName()));
        java.util.List<ShaderNodeVariable> inputs = new ArrayList<ShaderNodeVariable>();
        inputs.add(new ShaderNodeVariable(var.getType(),var.getName()));

        init(inputs, outputs);
    }

    public boolean isInputAvailable(){
        return !inputDots.get(0).isConnected();
    }

    public boolean isOutputAvailable(){
        return !outputDots.get(0).isConnected();
    }

    @Override
    public String getKey() {
        return "Global." + getNodeName();
    }

    @Override
    protected void initHeader(JLabel header) {
        header.setIcon(Icons.output);
        header.setText(getNodeName());
        header.setToolTipText(getNodeName());
    }
}
