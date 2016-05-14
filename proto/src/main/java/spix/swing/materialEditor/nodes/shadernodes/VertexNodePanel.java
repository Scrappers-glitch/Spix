package spix.swing.materialEditor.nodes.shadernodes;

import com.jme3.shader.ShaderNode;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Nehon on 13/05/2016.
 */
public class VertexNodePanel extends ShaderNodePanel {

    public VertexNodePanel(ShaderNode shaderNode) {
        super(shaderNode, new Color(220, 220, 70));//yellow
    }

    @Override
    protected void initHeader(JLabel header) {
        super.initHeader(header);
        header.setIcon(Icons.vert);
    }

    public void edit() {
//       diagram.showEdit(NodePanel.this);
    }
}
