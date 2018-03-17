package spix.swing.materialEditor.controller;

import com.jme3.material.*;
import com.jme3.shader.*;
import spix.app.metadata.ShaderNodeMetadata;
import spix.swing.SwingGui;
import spix.swing.materialEditor.*;
import spix.swing.materialEditor.panels.ErrorLog;
import spix.swing.materialEditor.nodes.*;
import spix.swing.materialEditor.preview.*;
import spix.swing.materialEditor.sort.Node;
import spix.swing.materialEditor.utils.MaterialDefUtils;
import spix.util.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Created by Nehon on 21/05/2016.
 */
public class DiagramUiHandler {

    private Diagram diagram;
    private String currentTechniqueName;
    private Map<String, NodePanel> nodes = new HashMap<>();
    //A convenience map to easy access to the output nodes;
    private Map<Shader.ShaderType, Map<String, List<OutPanel>>> outPanels = new HashMap<>();
    protected List<Connection> connections = new ArrayList<>();
    private MaterialPreviewRenderer previewRenderer = new MaterialPreviewRenderer();
    private Map<String, Object> matDefMetadata;
    private int outCursor = 0;
    private List<VariableMapping> tmpOutMappings = new ArrayList<>();
    private Map<String, Group> groups = new HashMap<>();

    public DiagramUiHandler(Diagram diagram) {
        this.diagram = diagram;
    }

    void clear() {
        nodes.clear();
        outPanels.clear();
        connections.clear();
        diagram.removeAll();
        outCursor = 0;
    }

    void refreshDiagram() {
        diagram.revalidate();
        diagram.repaint();
    }


    private void attachNodePanel(NodePanel node) {
        if (node == null) {
            return;
        }

        diagram.add(node);
        diagram.setComponentZOrder(node, 0);
    }

    public void dispatchEventToDiagram(MouseEvent e, Component source) {
        MouseEvent me = SwingUtils.convertEvent(source, e, diagram);
        diagram.dispatchEvent(me);
    }

    void setCurrentTechniqueName(String currentTechniqueName) {
        clear();
        this.currentTechniqueName = currentTechniqueName;
    }

    public void setMatDefMetadata(Map<String, Object> matDefMetadata) {
        this.matDefMetadata = matDefMetadata;
    }

    public Map<String, Object> getMatDefMetadata() {
        return matDefMetadata;
    }

    void makeConnection(MatDefEditorController controller, VariableMapping mapping, TechniqueDef technique, String nodeName) {
        NodePanel forNode = nodes.get(MaterialDefUtils.makeShaderNodeKey(technique.getName(), nodeName));

        Dot rightDot = findConnectPointForInput(controller, mapping, forNode);
        Dot leftDot = findConnectPointForOutput(controller, mapping, forNode);
        connect(controller, leftDot, rightDot);
    }

    public void createGroup(MatDefEditorController controller, String groupName, List<NodePanel> panels) {
        Group g = new Group(controller, groupName, panels);
        groups.put(groupName, g);
        initGroup(g);
        refreshDiagram();
    }

    public void initGroup(Group group) {

        ShaderNodeGroup groupPanel = group.getComponent();
        int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE;
        Point loc = new Point();
        for (NodePanel node : group.nodes) {
            ShaderNodeMetadata nodeMd = getNodeMetadata(node);
            nodeMd.setGroup(group.name);
            node.getLocation(loc);
            if (loc.x < x) {
                x = loc.x;
            }
            if (loc.y < y) {
                y = loc.y;
            }
            node.hideToolBar();
            diagram.remove(node);

        }

        groupPanel.setLocation(x, y);
        attachNodePanel(groupPanel);
    }

    public void ungroup(String groupName) {
        Group g = groups.remove(groupName);
        if (g == null) {
            return;
        }
        g.getComponent().cleanup(g.nodes, connections);
        diagram.remove(g.getComponent());
        for (NodePanel node : g.nodes) {
            diagram.add(node);
            diagram.setComponentZOrder(node, 0);
        }
        refreshDiagram();
    }

    Connection connect(MatDefEditorController controller, Dot start, Dot end) {
        String key = MaterialDefUtils.makeConnectionKey(start.getNodeName(), start.getVariableName(), end.getNodeName(), end.getVariableName(), currentTechniqueName);
        Connection conn = new Connection(controller, key, start, end);
        start.connect(conn);
        end.connect(conn);
        connections.add(conn);
        diagram.add(conn);
        refreshDiagram();

        refreshOutPanelKey(start, end);

        return conn;
    }

    private void refreshOutPanelKey(Dot dot1, Dot dot2, String stamp) {
        if (dot1.getNode() instanceof OutPanel) {
            NodePanel p = dot1.getNode();
            nodes.remove(p.getKey());
            String k = p.getKey();
            String[] s = k.split("-");
            k = "";
            for (String s1 : s) {
                if (!s1.startsWith(stamp)) {
                    k += s1 + "-";
                }
            }
            k += stamp + dot2.getNode().getKey();
            p.setKey(k);
            nodes.put(k, p);
            System.err.println(k);
        }
    }

    private Dot findConnectPointForInput(MatDefEditorController controller, VariableMapping mapping, NodePanel forNode) {
        String nameSpace = mapping.getLeftVariable().getNameSpace();
        String name = mapping.getLeftVariable().getName();
        return getNodePanelForConnection(controller, forNode, nameSpace, name, true).getInputConnectPoint(nameSpace, name);
    }

    private Dot findConnectPointForOutput(MatDefEditorController controller, VariableMapping mapping, NodePanel forNode) {
        String nameSpace = mapping.getRightVariable().getNameSpace();
        String name = mapping.getRightVariable().getName();
        return getNodePanelForConnection(controller, forNode, nameSpace, name, false).getOutputConnectPoint(nameSpace, name);
    }

    private NodePanel getNodePanelForConnection(MatDefEditorController controller, NodePanel forNode, String nameSpace, String name, boolean forInput) {
        NodePanel np = null;
        if (isShaderInput(nameSpace)) {
            np = nodes.get(MaterialDefUtils.makeInputKey(currentTechniqueName, nameSpace, name));
        } else if (isGlobal(nameSpace)) {
            np = getOutPanel(controller, forNode.getShaderType(), new ShaderNodeVariable("vec4", "Global", name), forNode, forInput);
            nodes.remove(np.getKey());
            np.setKey(np.getKey() + "-" + (forInput ? "in." : "out.") + forNode.getKey());
            nodes.put(np.getKey(), np);
        } else {
            np = nodes.get(MaterialDefUtils.makeShaderNodeKey(currentTechniqueName, nameSpace));
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

    private NodePanel getOutPanel(MatDefEditorController controller, Shader.ShaderType type, ShaderNodeVariable var, NodePanel node, boolean forInput) {

        List<OutPanel> panelList = getOutPanelList(type, var);


        if (!forInput) {
            for (OutPanel outPanel : panelList) {
                if (outPanel.isOutputAvailable() && !outPanel.getInputConnectPoint(var.getNameSpace(), var.getName()).isConnectedToNode(node)) {
                    return outPanel;
                }
            }
        } else {
            for (OutPanel outPanel : panelList) {
                if (outPanel.isInputAvailable() && !outPanel.getOutputConnectPoint(var.getNameSpace(), var.getName()).isConnectedToNode(node)) {
                    return outPanel;
                }
            }
        }

        return controller.addOutPanel(type, var);

    }

    private List<OutPanel> getOutPanelList(Shader.ShaderType type, ShaderNodeVariable var) {
        Map<String, List<OutPanel>> map = outPanels.get(type);
        if (map == null) {
            map = new HashMap<>();
            outPanels.put(type, map);
        }
        List<OutPanel> panelList = map.get(var.getName());
        if (panelList == null) {
            panelList = new ArrayList<>();
            map.put(var.getName(), panelList);
        }
        return panelList;
    }

    void removeNode(MatDefEditorController controller, String key) {
        NodePanel n = nodes.remove(key);
        if (n == null) {
            //todo Somtimes, for some unknown reason the node is null.
            //It's not a threading issue, seems to always occur on the awt event thread.
            //Seems more that this code is sometimes called twice in a row...
            System.err.println("Is event dispatch thread: " + SwingUtilities.isEventDispatchThread());
            for (String k : nodes.keySet()) {
                System.err.println("Key: " + k + " => " + nodes.get(k));
            }
            return;
            //throw new IllegalArgumentException("Cannot delete node for key: " + key);
        }

        if (n instanceof OutPanel) {
            OutPanel p = (OutPanel) n;
            outPanels.get(p.getShaderType()).get(p.getVarName()).remove(p);
        }
        n.cleanup();

        for (Iterator<Connection> it = connections.iterator(); it.hasNext(); ) {
            Connection conn = it.next();
            if (conn.getStart().getNode() == n || conn.getEnd().getNode() == n) {
                it.remove();
                //it's important to call this from the controller so the connections are not just removed from the UI
                controller.removeConnectionNoRefresh(conn);
            }
        }

        diagram.remove(n);
        refreshDiagram();
    }

    public void removeConnection(Connection conn) {
        connections.remove(conn);
        cleanUpConnection(conn);
        //   refreshDiagram();
    }

    private void cleanUpConnection(Connection conn) {
        refreshOutPanelKey(conn.getStart(), conn.getEnd());
        conn.getEnd().disconnect(conn);
        conn.getStart().disconnect(conn);
        diagram.remove(conn);
    }

    private void refreshOutPanelKey(Dot start, Dot end) {
        OutPanel p = null;
        if (start.getNode() instanceof OutPanel) {
            p = (OutPanel) start.getNode();
        }
        if (end.getNode() instanceof OutPanel) {
            p = (OutPanel) end.getNode();
        }
        if (p != null) {
            String k = p.refreshKey(currentTechniqueName);
            nodes.remove(p.getKey());
            ShaderNodeMetadata nodeMd = (ShaderNodeMetadata) matDefMetadata.get(p.getKey());
            p.setKey(k);
            nodes.put(k, p);
            matDefMetadata.put(k, nodeMd);
        }
    }


    NodePanel makeOutPanel(MatDefEditorController controller, Shader.ShaderType type, ShaderNodeVariable var) {
        List<OutPanel> panelList = getOutPanelList(type, var);
        String key = MaterialDefUtils.makeGlobalOutKey(currentTechniqueName, var.getName(), "");
        OutPanel node = OutPanel.create(controller, key, type, var);
        panelList.add(node);
        nodes.put(key, node);
        attachNodePanel(node);
        return node;
    }

    NodePanel addShaderNodePanel(MatDefEditorController controller, ShaderNode sn) {
        NodePanel node = ShaderNodePanel.create(controller, MaterialDefUtils.makeShaderNodeKey(currentTechniqueName, sn.getName()), sn);
        nodes.put(node.getKey(), node);
        attachNodePanel(node);
        return node;
    }

    private boolean hasVariableWithName(String name, List<ShaderNodeVariable> variables) {
        for (ShaderNodeVariable variable : variables) {
            if (variable.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void refreshShaderNodePanel(MatDefEditorController controller, ShaderNode sn, TechniqueDef techniqueDef, boolean reconnect) {
        NodePanel panel = nodes.get(MaterialDefUtils.makeShaderNodeKey(currentTechniqueName, sn.getName()));
        if (panel == null) {
            return;
        }

        tmpOutMappings.clear();
        panel.cleanup();
        for (Iterator<Connection> it = connections.iterator(); it.hasNext(); ) {
            Connection conn = it.next();
            if (conn.getStart().getNode() == panel || conn.getEnd().getNode() == panel) {
                if (reconnect) {
                    if (conn.getStart().getNode() == panel) {
                        if (hasVariableWithName(conn.getStart().getText(), sn.getDefinition().getOutputs())) {
                            ShaderNodeVariable left = new ShaderNodeVariable(conn.getEnd().getType(), conn.getEnd().getNodeName(), conn.getEnd().getVariableName());
                            ShaderNodeVariable right = new ShaderNodeVariable(conn.getStart().getType(), conn.getStart().getNodeName(), conn.getStart().getVariableName());
                            tmpOutMappings.add(new VariableMapping(left, "", right, "", null));
                        }
                    }
                    it.remove();
                    cleanUpConnection(conn);
                } else {
                    it.remove();
                    cleanUpConnection(conn);
                    controller.removeMappingForConnection(conn);
                }

            }
        }
        diagram.remove(panel);

        NodePanel newPanel = addShaderNodePanel(controller, sn);
        newPanel.setLocation(panel.getLocation());
        if (panel.isSelected()) {
            controller.select(newPanel, false);
        }
        if (!reconnect) {
            return;
        }

        for (VariableMapping mapping : sn.getInputMapping()) {
            makeConnection(controller, mapping, techniqueDef, sn.getName());
        }

        for (VariableMapping mapping : tmpOutMappings) {
            makeConnection(controller, mapping, techniqueDef, sn.getName());
        }
    }


    void renameShaderNode(String oldName, String newName) {
        String oldKey = MaterialDefUtils.makeShaderNodeKey(currentTechniqueName, oldName);
        String newKey = MaterialDefUtils.makeShaderNodeKey(currentTechniqueName, newName);
        ShaderNodePanel node = (ShaderNodePanel) nodes.remove(oldKey);
        Map<String, ShaderNodeMetadata> techMD = getTechMetadata();
        ShaderNodeMetadata nodeMD = techMD.remove(oldKey);
        node.refresh(newName);
        node.setKey(newKey);
        techMD.put(newKey, nodeMD);
        nodes.put(newKey, node);

        //refresh connection keys
        for (Connection connection : connections) {
            String key = MaterialDefUtils.makeConnectionKey(connection.getStart().getNodeName(), connection.getStart().getVariableName(),
                    connection.getEnd().getNodeName(), connection.getEnd().getVariableName(), currentTechniqueName);
            connection.setKey(key);
        }
    }


    NodePanel addInputPanel(MatDefEditorController controller, ShaderNodeVariable shaderNodeVariable) {
        String key = MaterialDefUtils.makeInputKey(currentTechniqueName, shaderNodeVariable.getNameSpace(), shaderNodeVariable.getName());
        NodePanel node = nodes.get(key);
        if (node == null) {

            switch (shaderNodeVariable.getNameSpace()) {
                case "MatParam":
                    node = InputPanel.create(controller, key, InputPanel.ShaderInputType.MatParam, shaderNodeVariable);
                    break;
                case "Attr":
                    node = InputPanel.create(controller, key, InputPanel.ShaderInputType.Attribute, shaderNodeVariable);
                    break;
                case "WorldParam":
                    node = InputPanel.create(controller, key, InputPanel.ShaderInputType.WorldParam, shaderNodeVariable);
                    break;
            }
            nodes.put(node.getKey(), node);
            attachNodePanel(node);
        }
        return node;
    }

    String fixNodeName(String name, int count) {
        for (NodePanel nodePanel : nodes.values()) {
            if ((name + (count == 0 ? "" : count)).equals(nodePanel.getNodeName())) {
                return fixNodeName(name, count + 1);
            }
        }
        return name + (count == 0 ? "" : count);
    }

    String fixGroupName(String name, int count) {
        for (Group group : groups.values()) {
            if ((name + (count == 0 ? "" : count)).equals(group.name)) {
                return fixGroupName(name, count + 1);
            }
        }
        return name + (count == 0 ? "" : count);
    }

    Connection pickForConnection(MouseEvent e) {
        for (Connection connection : connections) {
            MouseEvent me = SwingUtilities.convertMouseEvent(diagram, e, connection);
            if (connection.pick(me)) {
                return connection;
            }
        }
        return null;
    }

    public void autoLayout(MatDefEditorController controller, Deque<Node> sortedNodes) {
        if (sortedNodes == null) {
            return;
        }
        //first layout from metadata
        boolean needsAutoLayout = false;
        for (NodePanel p : nodes.values()) {
            ShaderNodeMetadata nodeMD = getNodeMetadata(p);
            checkGroup(controller, p, nodeMD);
            p.setLocation(nodeMD.getX(), nodeMD.getY());
            if (nodeMD.getX() < 0) {
                needsAutoLayout = true;
            }
        }
        for (Map<String, List<OutPanel>> map : outPanels.values()) {
            for (List<OutPanel> list : map.values()) {
                for (OutPanel p : list) {
                    ShaderNodeMetadata nodeMD = getNodeMetadata(p);
                    checkGroup(controller, p, nodeMD);
                    p.setLocation(nodeMD.getX(), nodeMD.getY());
                    if (nodeMD.getX() < 0) {
                        needsAutoLayout = true;
                    }
                }
            }
        }

        if (!groups.isEmpty()) {
            for (Group group : groups.values()) {
                initGroup(group);
                ShaderNodeMetadata md = getGroupMetadata(group);
                group.getComponent().setLocation(md.getX(), md.getY());
            }
        }

        if (!needsAutoLayout) {
            return;
        }

        //diagram autoLayout
        int offset = 200;
        final int wMargin = 25;
        final int hMargin = 10;
        for (Node node : sortedNodes) {
            NodePanel p = nodes.get(node.getKey());
            ShaderNodeMetadata nodeMD = getNodeMetadata(p);
            if (nodeMD.getX() > 0) {
                p.setLocation(nodeMD.getX(), nodeMD.getY());
                continue;
            }
            if (p instanceof ShaderNodePanel) {
                int heightOffset = 0;
                p.setLocation(offset, getNodeTop(p));
                nodeMD.setX(offset);
                nodeMD.setY(getNodeTop(p));
                for (Dot dot : p.getInputConnectPoints().values()) {
                    for (Dot pair : dot.getConnectedDots()) {
                        NodePanel p2 = pair.getNode();
                        ShaderNodeMetadata nodeMD2 = getNodeMetadata(p2);
                        if (!(p2 instanceof ShaderNodePanel)) {
                            int x = p.getX() - p2.getWidth() - wMargin;
                            int y = p.getY() + heightOffset;
                            p2.setLocation(x, y);
                            nodeMD2.setX(x);
                            nodeMD2.setY(y);
                            heightOffset += p2.getHeight() + hMargin;
                        }
                    }
                }
                heightOffset = 0;
                for (Dot dot : p.getOutputConnectPoints().values()) {
                    for (Dot pair : dot.getConnectedDots()) {
                        NodePanel p2 = pair.getNode();
                        ShaderNodeMetadata nodeMD2 = getNodeMetadata(p2);
                        if (p2 instanceof OutPanel) {
                            int x = p.getX() + p.getWidth() + wMargin;
                            int y = p.getY() + heightOffset;
                            p2.setLocation(x, y);
                            nodeMD2.setX(x);
                            nodeMD2.setY(y);
                            heightOffset += p2.getHeight() + hMargin;
                        }
                    }
                }
                offset += 350;
            }

        }
        refreshDiagram();
        diagram.fitContent();

    }

    public void checkGroup(MatDefEditorController controller, NodePanel p, ShaderNodeMetadata nodeMD) {
        String groupName = nodeMD.getGroup();
        if (groupName != null) {
            Group g = groups.get(groupName);
            if (g == null) {
                g = new Group(controller, groupName, new ArrayList<>());
                groups.put(groupName, g);
            }
            if (!g.nodes.contains(p)) {
                g.nodes.add(p);
            }
        }
    }

    public void onNodeMoved(NodePanel node) {
        ShaderNodeMetadata nodeMD = getNodeMetadata(node);
        Point p = node.getLocation();
        nodeMD.setX((int) p.getX());
        nodeMD.setY((int) p.getY());
        fitContent();
    }

    private ShaderNodeMetadata getNodeMetadata(NodePanel node) {
        Map<String, ShaderNodeMetadata> techLayout = getTechMetadata();
        ShaderNodeMetadata nodeMD = techLayout.get(node.getKey());
        if (nodeMD == null) {
            nodeMD = new ShaderNodeMetadata();
            techLayout.put(node.getKey(), nodeMD);
        }

        return nodeMD;
    }

    private ShaderNodeMetadata getGroupMetadata(Group g) {
        Map<String, ShaderNodeMetadata> techLayout = getTechMetadata();
        ShaderNodeMetadata nodeMD = techLayout.get("group." + g.name);
        if (nodeMD == null) {
            nodeMD = new ShaderNodeMetadata();
            techLayout.put("group." + g.name, nodeMD);
        }

        return nodeMD;
    }

    private Map<String, ShaderNodeMetadata> getTechMetadata() {
        Map<String, Map<String, ShaderNodeMetadata>> nodeLayout = (Map<String, Map<String, ShaderNodeMetadata>>) matDefMetadata.get("NodesLayout");
        if (nodeLayout == null) {
            nodeLayout = new LinkedHashMap<>();
            matDefMetadata.put("NodesLayout", nodeLayout);
        }
        Map<String, ShaderNodeMetadata> techLayout = nodeLayout.get(currentTechniqueName);
        if (techLayout == null) {
            techLayout = new LinkedHashMap<>();
            nodeLayout.put(currentTechniqueName, techLayout);
        }

        return techLayout;
    }

    private int getNodeTop(NodePanel node) {

        if (node.getShaderType() == Shader.ShaderType.Vertex) {
            return 50;
        }
        if (node.getShaderType() == Shader.ShaderType.Fragment) {
            return 300;
        }

        return 0;

    }


    public void fitContent() {
        diagram.fitContent();
    }

    private List<OutPanel> getOutPanelsForPreviews() {

        List<OutPanel> panels = new ArrayList<>();
        for (NodePanel nodePanel : nodes.values()) {
            if (nodePanel instanceof OutPanel) {
                panels.add((OutPanel) nodePanel);
            }
        }
        return panels;
    }

    public void refreshPreviews(SwingGui gui, ErrorLog errorLog, MaterialDef matDef, Deque<Node> sortedNodes, Map<String, MatParam> params) {
        previewRenderer.batchRequests(gui, errorLog, getOutPanelsForPreviews(), matDef, currentTechniqueName, sortedNodes, params);
    }

    public List<Node> getNodesForSort() {
        List<Node> sortNodes = new ArrayList<>();
        Map<String, Node> nodeMap = new HashMap<>();
        for (String key : nodes.keySet()) {
            NodePanel p = nodes.get(key);
            if (isNodeForSort(p)) {
                Node n = getNode(nodeMap, key, p.getShaderType());
                if (p.getOutputConnectPoints().values().isEmpty()) {
                    //node has no output
                    if (!(p instanceof OutPanel)) {
                        //the node is not an output but has no output, it has to be placed as soon as possible in the node graph
                        //for example a discard pixel node is best done as soon as possible.
                        n.setHighPriority(true);
                    }
                } else {
                    for (Dot dot : p.getOutputConnectPoints().values()) {
                        for (Dot pair : dot.getConnectedDots()) {
                            if (isNodeForSort(pair.getNode())) {
                                Node n2 = getNode(nodeMap, pair.getNode().getKey(), pair.getNode().getShaderType());
                                n.addChild(n2);
                                n2.addParent(n);
                            }
                        }
                    }
                }
                sortNodes.add(n);
            }
        }


        return sortNodes;
    }

    private boolean isNodeForSort(NodePanel p) {
        return p instanceof ShaderNodePanel || p instanceof OutPanel;
    }

    private Node getNode(Map<String, Node> nodeMap, String key, Shader.ShaderType type) {
        Node n = nodeMap.get(key);
        if (n == null) {
            n = new Node(key, type);
            nodeMap.put(key, n);
        }
        return n;
    }

    private class Group {
        List<NodePanel> nodes;
        MatDefEditorController controller;
        ShaderNodeGroup panel;
        String name;

        public Group(MatDefEditorController controller, String groupName, List<NodePanel> nodes) {
            this.nodes = nodes;
            this.name = groupName;
            this.controller = controller;
        }

        ShaderNodeGroup getComponent() {
            if (panel != null) {
                return panel;
            }
            createComponent();
            return panel;
        }

        private void createComponent() {
            panel = ShaderNodeGroup.create(controller, name, nodes, connections);
        }
    }
}
