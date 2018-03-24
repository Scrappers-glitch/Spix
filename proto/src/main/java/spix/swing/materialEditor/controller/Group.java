package spix.swing.materialEditor.controller;

import spix.swing.materialEditor.Connection;
import spix.swing.materialEditor.Dot;
import spix.swing.materialEditor.nodes.*;

import java.util.*;

public class Group {
    private List<NodePanel> nodes;
    private MatDefEditorController controller;
    private List<Connection> connections;
    private ShaderNodeGroup panel;
    private String name;
    private Map<String, Connection> internalConnections = new HashMap<>();
    private GroupInOutPanel inputsPanel;
    private GroupInOutPanel outputsPanel;

    public Group(MatDefEditorController controller, String groupName, List<NodePanel> nodes, List<Connection> connections) {
        this.nodes = nodes;
        this.name = groupName;
        this.controller = controller;
        this.connections = connections;
    }

    public ShaderNodeGroup getComponent() {
        if (panel == null) {
            createComponent();
        }
        return panel;
    }

    public Map<String, Connection> getInternalConnections() {
        return internalConnections;
    }

    private void createComponent() {
        panel = new ShaderNodeGroup(controller, this, nodes, connections, internalConnections);
        inputsPanel = GroupInOutPanel.create(controller, panel, GroupInOutPanel.Type.Inputs, internalConnections);
        outputsPanel = GroupInOutPanel.create(controller, panel, GroupInOutPanel.Type.Outputs, internalConnections);
    }

    public GroupInOutPanel getInputsPanel() {
        if (inputsPanel == null) {
            createComponent();
        }
        return inputsPanel;
    }

    public void addInternalConnection(Connection c, Dot d){
        c.setGroup(name);
        String k = "Group." + name + d.getNodeName() + "." + d.getVariableName();
        internalConnections.put(k, c);
    }

    public GroupInOutPanel getOutputsPanel() {
        if (outputsPanel == null) {
            createComponent();
        }
        return outputsPanel;
    }

    public List<Connection> cleanUpConnection(Connection conn) {
        List<Connection> toRemove = new ArrayList<>();
        // 1. remove the connection from the group's internalConnections.
        internalConnections.remove(conn.getKey());
        // 2. remove the actual connection(s) if there was one(some).
        for (Connection connection : connections) {
            if (equals(connection.getEnd(), conn.getEnd()) ||
                    equals(connection.getStart(), conn.getStart())) {
                toRemove.add(connection);
            }
        }
        if (conn.getEnd().getNode() == outputsPanel) {
            outputsPanel.removeDot(conn.getEnd());
            if(!toRemove.isEmpty()) {
                panel.removeDot(toRemove.get(0).getStart());
            }
        }
        if (conn.getStart().getNode() == inputsPanel) {
            inputsPanel.removeDot(conn.getStart());
            if(!toRemove.isEmpty()) {
                panel.removeDot(toRemove.get(0).getEnd());
            }
        }
        // 3. remove the input / output from the in out panels. and possibly meta data
        return toRemove;
    }

    private boolean equals(Dot d1, Dot d2) {
        return d1.getNodeName() == d2.getNodeName() &&
                d1.getVariableName() == d2.getVariableName() &&
                d1.getType() == d2.getType();
    }

    public List<NodePanel> getNodes() {
        return nodes;
    }

    public String getName() {
        return name;
    }
}
