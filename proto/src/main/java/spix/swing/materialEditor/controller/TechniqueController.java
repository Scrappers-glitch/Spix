package spix.swing.materialEditor.controller;

import com.jme3.material.*;
import com.jme3.shader.*;
import spix.app.utils.MaterialDefUtils;
import spix.swing.materialEditor.*;
import spix.swing.materialEditor.nodes.*;

import java.util.*;

/**
 * Created by Nehon on 21/05/2016.
 */
public class TechniqueController {

    private TechniqueDef currentTechnique;


    public TechniqueDef getCurrentTechnique() {
        return currentTechnique;
    }

    public void setCurrentTechnique(TechniqueDef currentTechnique) {
        this.currentTechnique = currentTechnique;
    }

    protected void initDiagram(MaterialDefController controller, TechniqueDef technique, MaterialDef matDef, Diagram diagram ) {
        diagram.clear();
        setCurrentTechnique(technique);

        if (technique.isUsingShaderNodes()) {
            MaterialDefUtils.computeShaderNodeGenerationInfo(technique);
            List<ShaderNodeVariable> uniforms = new ArrayList<>();
            MaterialDefUtils.getAllUniforms(technique, matDef, uniforms);

//            ShaderNodeVariable inPosition = new ShaderNodeVariable("vec4", "Attr", "inPosition");
//            ShaderNodeVariable position = new ShaderNodeVariable("vec4", "Global", "position");
//            diagram.addNode(new AttributePanel(inPosition));
            //makeConnection(new VariableMapping(position,"", inPosition,"",""), technique);

            for (ShaderNode sn : technique.getShaderNodes()) {
                NodePanel np = ShaderNodePanel.create(controller, sn);
                np.setTechName(currentTechnique.getName());
                diagram.addNode(np);
            }

            for (ShaderNodeVariable shaderNodeVariable : technique.getShaderGenerationInfo().getAttributes()) {
                NodePanel np = diagram.getNodePanel(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
                if (np == null) {
                    np = InputPanel.create(controller, InputPanel.ShaderInputType.Attribute, shaderNodeVariable);
                    np.setTechName(currentTechnique.getName());
                    diagram.addNode(np);
                }
            }

            for (ShaderNodeVariable shaderNodeVariable : uniforms) {
                NodePanel np = diagram.getNodePanel(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
                if (np == null) {
                    if (shaderNodeVariable.getNameSpace().equals("MatParam")) {
                        np = InputPanel.create(controller, InputPanel.ShaderInputType.MatParam, shaderNodeVariable);
                    } else {
                        np = InputPanel.create(controller, InputPanel.ShaderInputType.WorldParam, shaderNodeVariable);
                    }
                    np.setTechName(currentTechnique.getName());
                    diagram.addNode(np);
                }
            }

            for (ShaderNode sn : technique.getShaderNodes()) {
                List<VariableMapping> ins = sn.getInputMapping();
                if (ins != null) {
                    for (VariableMapping mapping : ins) {
                        makeConnection(controller, mapping, technique, sn.getName(), diagram);
                    }
                }
                List<VariableMapping> outs = sn.getOutputMapping();
                if (outs != null) {
                    for (VariableMapping mapping : outs) {
                        makeConnection(controller, mapping, technique, sn.getName(), diagram);
                    }
                }

            }
        }

        diagram.autoLayout();
    }

    private void makeConnection(MaterialDefController controller, VariableMapping mapping, TechniqueDef technique, String nodeName, Diagram diagram) {
        NodePanel forNode = diagram.getNodePanel(currentTechnique.getName() + "/" + nodeName);

        Dot leftDot = findConnectPointForInput(mapping, forNode, diagram);
        Dot rightDot = findConnectPointForOutput(mapping, forNode, diagram);
        Connection conn = connect(controller, leftDot, rightDot, diagram);
        //  mapping.addPropertyChangeListener(WeakListeners.propertyChange(conn, mapping));
        //  conn.makeKey(mapping, technique.getName());
    }

    public Connection connect(MaterialDefController controller, Dot start, Dot end, Diagram diagram) {
        Connection conn = new Connection(controller, start, end);
        start.connect(conn);
        end.connect(conn);
        diagram.addConnection(conn);
        return conn;
    }

    private Dot findConnectPointForInput(VariableMapping mapping, NodePanel forNode, Diagram diagram) {
        String nameSpace = mapping.getLeftVariable().getNameSpace();
        String name = mapping.getLeftVariable().getName();
        return getNodePanelForConnection(forNode, nameSpace, name, true, diagram).getInputConnectPoint(name);
    }

    private Dot findConnectPointForOutput(VariableMapping mapping, NodePanel forNode, Diagram diagram) {
        String nameSpace = mapping.getRightVariable().getNameSpace();
        String name = mapping.getRightVariable().getName();
        return getNodePanelForConnection(forNode, nameSpace, name, false, diagram).getOutputConnectPoint(name);
    }

    private NodePanel getNodePanelForConnection(NodePanel forNode, String nameSpace, String name, boolean forInput, Diagram diagram) {
        NodePanel np = null;
        if (isShaderInput(nameSpace)) {
            np = diagram.getNodePanel(nameSpace + "." + name);
        } else if (isGlobal(nameSpace)) {
            np = diagram.getOutPanel(forNode.getShaderType(), new ShaderNodeVariable("vec4", "Global", name), forNode, forInput);
        } else {
            np = diagram.getNodePanel(currentTechnique.getName() + "/" + nameSpace);
        }
        return np;
    }

    private boolean isShaderInput(String nameSpace) {
        return nameSpace.equals("MatParam")
                || nameSpace.equals("WorldParam")
                || nameSpace.equals("Attr");
    }

    private boolean isGlobal(String nameSpace) {
        return nameSpace.equals("Global");
    }


}
