package spix.swing.materialEditor.nodes;

import com.jme3.shader.Shader;
import com.jme3.shader.ShaderNode;
import spix.swing.materialEditor.nodes.shadernodes.FragmentNodePanel;
import spix.swing.materialEditor.nodes.shadernodes.ShaderNodePanel;
import spix.swing.materialEditor.nodes.shadernodes.VertexNodePanel;

import java.awt.*;

/**
 * Created by bouquet on 15/05/16.
 */
public class NodePanelFactory {


    public static ShaderNodePanel createShaderNodePanel(ShaderNode shaderNode){
        if(shaderNode.getDefinition().getType() == Shader.ShaderType.Vertex){
            return new VertexNodePanel(shaderNode);
        }
        if(shaderNode.getDefinition().getType() == Shader.ShaderType.Fragment){
            return new FragmentNodePanel(shaderNode);
        }

        return null;
    }

}
