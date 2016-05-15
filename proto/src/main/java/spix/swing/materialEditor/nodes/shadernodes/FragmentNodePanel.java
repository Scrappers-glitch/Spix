package spix.swing.materialEditor.nodes.shadernodes;

import com.jme3.shader.ShaderNode;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.nodes.Editable;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Nehon on 13/05/2016.
 */
public class FragmentNodePanel extends ShaderNodePanel {

    public FragmentNodePanel(ShaderNode shaderNode) {
        super(shaderNode, new Color(114, 200, 255)); //blue
    }


    @Override
    protected void initHeader(JLabel header) {
        super.initHeader(header);
        header.setIcon(Icons.frag);
    }

}
