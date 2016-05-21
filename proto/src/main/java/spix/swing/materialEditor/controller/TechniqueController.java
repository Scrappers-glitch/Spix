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
    private Map<String, NodePanel> nodes = new HashMap<>();
    //A convenience map to easy access to the output nodes;
    private Map<Shader.ShaderType, Map<String, List<InOutPanel>>> outPanels = new HashMap<>();
    protected List<Connection> connections = new ArrayList<>();

    void initDiagram(MaterialDefController controller, TechniqueDef technique, MaterialDef matDef, Diagram diagram ) {
        diagram.removeAll();
        nodes.clear();
        outPanels.clear();
        connections.clear();

        this.currentTechnique = technique;

        if (technique.isUsingShaderNodes()) {
            MaterialDefUtils.computeShaderNodeGenerationInfo(technique);
            List<ShaderNodeVariable> uniforms = new ArrayList<>();
            MaterialDefUtils.getAllUniforms(technique, matDef, uniforms);

            for (ShaderNode sn : technique.getShaderNodes()) {
                NodePanel np = ShaderNodePanel.create(controller, sn);
                np.setTechName(currentTechnique.getName());
                addNode(np, diagram);
            }

            for (ShaderNodeVariable shaderNodeVariable : technique.getShaderGenerationInfo().getAttributes()) {
                NodePanel np = nodes.get(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
                if (np == null) {
                    np = InputPanel.create(controller, InputPanel.ShaderInputType.Attribute, shaderNodeVariable);
                    addNode(np, diagram);
                }
            }

            for (ShaderNodeVariable shaderNodeVariable : uniforms) {
                NodePanel np = nodes.get(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
                if (np == null) {
                    if (shaderNodeVariable.getNameSpace().equals("MatParam")) {
                        np = InputPanel.create(controller, InputPanel.ShaderInputType.MatParam, shaderNodeVariable);
                    } else {
                        np = InputPanel.create(controller, InputPanel.ShaderInputType.WorldParam, shaderNodeVariable);
                    }
                    addNode(np, diagram);
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

        //this will change when we have meta data
        diagram.autoLayout();
    }

    private void makeConnection(MaterialDefController controller, VariableMapping mapping, TechniqueDef technique, String nodeName, Diagram diagram) {
        NodePanel forNode = nodes.get(currentTechnique.getName() + "/" + nodeName);

        Dot leftDot = findConnectPointForInput(controller, mapping, forNode);
        Dot rightDot = findConnectPointForOutput(controller, mapping, forNode);
        Connection conn = connect(controller, leftDot, rightDot, diagram);
        conn.makeKey(mapping, technique.getName());
    }

    Connection connect(MaterialDefController controller, Dot start, Dot end, Diagram diagram) {
        Connection conn = new Connection(controller, start, end);
        start.connect(conn);
        end.connect(conn);
        connections.add(conn);
        diagram.add(conn);
        diagram.repaint();
        return conn;
    }

    private Dot findConnectPointForInput(MaterialDefController controller, VariableMapping mapping, NodePanel forNode) {
        String nameSpace = mapping.getLeftVariable().getNameSpace();
        String name = mapping.getLeftVariable().getName();
        return getNodePanelForConnection(controller, forNode, nameSpace, name, true).getInputConnectPoint(name);
    }

    private Dot findConnectPointForOutput(MaterialDefController controller, VariableMapping mapping, NodePanel forNode) {
        String nameSpace = mapping.getRightVariable().getNameSpace();
        String name = mapping.getRightVariable().getName();
        return getNodePanelForConnection(controller, forNode, nameSpace, name, false).getOutputConnectPoint(name);
    }

    private NodePanel getNodePanelForConnection(MaterialDefController controller, NodePanel forNode, String nameSpace, String name, boolean forInput) {
        NodePanel np = null;
        if (isShaderInput(nameSpace)) {
            np = nodes.get(nameSpace + "." + name);
        } else if (isGlobal(nameSpace)) {
            np = getOutPanel(controller, forNode.getShaderType(), new ShaderNodeVariable("vec4", "Global", name), forNode, forInput);
        } else {
            np = nodes.get(currentTechnique.getName() + "/" + nameSpace);
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

    private InOutPanel getOutPanel(MaterialDefController controller, Shader.ShaderType type, ShaderNodeVariable var, NodePanel node, boolean forInput) {

        List<InOutPanel> panelList = getOutPanelList(type, var);


        for (InOutPanel inOutPanel : panelList) {
            if (forInput) {
                if (inOutPanel.isOutputAvailable() && !inOutPanel.getInputConnectPoint(var.getName()).isConnectedToNode(node)) {
                    return inOutPanel;
                }
            } else {
                if (inOutPanel.isInputAvailable() && !inOutPanel.getOutputConnectPoint(var.getName()).isConnectedToNode(node)) {
                    return inOutPanel;
                }
            }
        }

        InOutPanel panel = InOutPanel.create(controller, type, var);
        panelList.add(panel);
        controller.addNode(panel);
        return panel;

    }

    private List<InOutPanel> getOutPanelList(Shader.ShaderType type, ShaderNodeVariable var) {
        Map<String, List<InOutPanel>> map = outPanels.get(type);
        if (map == null) {
            map = new HashMap<>();
            outPanels.put(type, map);
        }
        List<InOutPanel> panelList = map.get(var.getName());
        if (panelList == null) {
            panelList = new ArrayList<>();
            map.put(var.getName(), panelList);
        }
        return panelList;
    }

    void removeNode(String key, Diagram diagram){
        NodePanel n = nodes.remove(key);
        //just to be sure... but it should never happen.
        assert n != null;

        if (n instanceof InOutPanel) {
            outPanels.get(n.getShaderType()).get(n.getNodeName()).remove(n);
        }
        n.cleanup();

        for (Iterator<Connection> it = connections.iterator(); it.hasNext(); ) {
            Connection conn = it.next();
            if (conn.getStart().getNode() == n || conn.getEnd().getNode() == n) {
                it.remove();
                removeConnection(conn, diagram);
            }
        }

        diagram.remove(n);
        diagram.repaint();
    }
    public void removeConnection(Connection conn, Diagram diagram) {
        connections.remove(conn);
        conn.getEnd().disconnect(conn);
        conn.getStart().disconnect(conn);
        diagram.remove(conn);
        diagram.repaint();
    }


    void addNode(NodePanel node, Diagram diagram){
        node.setTechName(currentTechnique.getName());
        nodes.put(node.getKey(), node);
        diagram.add(node);
        diagram.setComponentZOrder(node, 0);
    }

    // TODO: 21/05/2016 See if we really need this. It was use in the sdk if the user was naming a node with the same name as another.
    private String fixNodeName(String name) {
        return fixNodeName(name, 0);
    }

    private String fixNodeName(String name, int count) {
        for (NodePanel nodePanel : nodes.values()) {
            if ((name + (count == 0 ? "" : count)).equals(nodePanel.getNodeName())) {
                return fixNodeName(name, count + 1);
            }
        }
        return name + (count == 0 ? "" : count);
    }
}
