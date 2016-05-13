package spix.swing.materialEditor.nodes.inputs;

import com.jme3.shader.ShaderNodeVariable;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Nehon on 13/05/2016.
 */
public class WorldParamPanel extends InputPanel {

    public WorldParamPanel(ShaderNodeVariable variable) {
        super(variable,new Color(220, 70, 70));//red
    }

    @Override
    public String getKey() {
        return "WorldParam." + outputLabels.get(0).getText();
    }

    @Override
    protected void initHeader(JLabel header) {
        header.setIcon(Icons.world);
        header.setText("WorldParam");
        header.setToolTipText("WorldParam");
        setNodeName("WorldParam");
    }
}
