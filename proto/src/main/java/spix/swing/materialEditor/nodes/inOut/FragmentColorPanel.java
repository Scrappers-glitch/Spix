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
public class FragmentColorPanel extends InOutPanel {

    public FragmentColorPanel(ShaderNodeVariable var) {
        super(var, new Color(220, 150, 0));//orange
    }


}
