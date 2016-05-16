package spix.swing.materialEditor.nodes;

import com.jme3.shader.*;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import static com.jme3.shader.Shader.ShaderType.Vertex;

/**
 * Created by Nehon on 13/05/2016.
 */
public abstract class InputPanel extends NodePanel {

    protected Shader.ShaderType shaderType = null;

    public enum ShaderInputType{
        Attribute,
        MatParam,
        WorldParam
    }

    private InputPanel(ShaderNodeVariable variable, Color color, Icon icon, Shader.ShaderType shaderType ){
        super(color, icon);
        this.shaderType = shaderType;
        java.util.List<ShaderNodeVariable> outputs = new ArrayList<ShaderNodeVariable>();
        outputs.add(variable);
        init(new ArrayList<ShaderNodeVariable>(), outputs);

    }

    public static InputPanel create(ShaderInputType type, ShaderNodeVariable var){
        Color color = new Color(0,0,0);
        Shader.ShaderType sType = null;
        Icon icon = Icons.node;
        switch (type){
            case Attribute:
                color = new Color(200, 200, 200);
                sType = Vertex;
                icon = Icons.attrib;
                break;
            case MatParam:
                color = new Color(70, 220, 70);
                icon = Icons.mat;
                break;
            case WorldParam:
                color = new Color(220, 70, 70);
                icon = Icons.world;
                break;
        }

        return new InputPanel(var, color, icon, sType) {
            @Override
            public Shader.ShaderType getShaderType() {
                return shaderType;
            }

            @Override
            public String getKey() {
                return var.getNameSpace() + "." + var.getName();
            }

            @Override
            protected void initHeader(JLabel header) {
                header.setText(type.name());
                header.setToolTipText(type.name());
                setNodeName(var.getNameSpace());
            }
        };
    }
}
