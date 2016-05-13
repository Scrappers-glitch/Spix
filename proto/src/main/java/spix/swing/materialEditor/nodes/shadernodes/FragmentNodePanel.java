package spix.swing.materialEditor.nodes.shadernodes;

import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Nehon on 13/05/2016.
 */
public class FragmentNodePanel extends ShaderNodePanel {

    public FragmentNodePanel() {
        super(new Color(114, 200, 255)); //blue
    }


    @Override
    protected void initHeader(JLabel header) {
        header.setIcon(Icons.frag);
    }


    public void edit() {
//       diagram.showEdit(NodePanel.this);
    }
}
