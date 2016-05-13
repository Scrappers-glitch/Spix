package spix.swing.materialEditor.nodes.inputs;

import com.jme3.shader.ShaderNodeVariable;
import spix.swing.materialEditor.nodes.NodePanel;

import java.awt.*;
import java.util.*;

/**
 * Created by Nehon on 13/05/2016.
 */
public abstract class InputPanel extends NodePanel {


    public InputPanel(ShaderNodeVariable variable, Color color){
        super(color);
        java.util.List<ShaderNodeVariable> outputs = new ArrayList<ShaderNodeVariable>();
        outputs.add(variable);
        init(new ArrayList<ShaderNodeVariable>(), outputs);

    }


}
