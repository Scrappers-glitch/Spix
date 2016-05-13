package spix.swing.materialEditor.nodes.inputs;

import com.jme3.shader.ShaderNodeVariable;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Nehon on 13/05/2016.
 */
public class MatParamPanel extends InputPanel {

    public MatParamPanel(ShaderNodeVariable variable) {
        super(variable, new Color(70, 220, 70)); //green
    }

    @Override
    public String getKey() {
        return "MatParam." + outputLabels.get(0).getText();
    }

    @Override
    protected void initHeader(JLabel header) {
        header.setIcon(Icons.mat);
        header.setText("MatParam");
        header.setToolTipText("MatParam");
        setNodeName("MatParam");
    }
}
