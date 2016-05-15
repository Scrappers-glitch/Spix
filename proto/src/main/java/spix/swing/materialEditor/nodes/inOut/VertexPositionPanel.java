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
public class VertexPositionPanel extends InOutPanel {

    public VertexPositionPanel(ShaderNodeVariable var) {
        super(var, new Color(50,50,50));// dark gray
    }
}
