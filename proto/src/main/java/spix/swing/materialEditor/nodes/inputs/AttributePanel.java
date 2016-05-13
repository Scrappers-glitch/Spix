package spix.swing.materialEditor.nodes.inputs;

import com.jme3.shader.ShaderNodeVariable;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Nehon on 13/05/2016.
 */
public class AttributePanel extends InputPanel {

    public AttributePanel(ShaderNodeVariable variable) {
        super(variable, new Color(200, 200, 200)); //whitish
    }


    @Override
    public String getKey() {
        return "Attr." + outputLabels.get(0).getText();
    }

    @Override
    protected void initHeader(JLabel header) {
        header.setIcon(Icons.attrib);
        header.setText("Attribute");
        header.setToolTipText("Attribute");
        setNodeName("Attr");
    }
}
