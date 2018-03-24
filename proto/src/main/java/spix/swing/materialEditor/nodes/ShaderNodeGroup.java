package spix.swing.materialEditor.nodes;

import com.jme3.shader.*;
import spix.swing.materialEditor.Connection;
import spix.swing.materialEditor.Dot;
import spix.swing.materialEditor.controller.Group;
import spix.swing.materialEditor.controller.MatDefEditorController;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.utils.MaterialDefUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by Nehon on 13/05/2016.
 */
public class ShaderNodeGroup extends NodePanel {

    private Group group;

    public ShaderNodeGroup(MatDefEditorController controller, Group group, List<NodePanel> nodes, List<Connection> connections, Map<String, Connection> internalConnections) {
        super(controller, "group." + group.getName(), new Color(180, 180, 225, 200), Icons.group);
        backgroundColor = new Color(100, 100, 130, 200);
        setNodeName(group.getName());
        canRenameFields = true;
        this.group = group;

        List<Dot> inDots = new ArrayList<>();
        List<Dot> outDots = new ArrayList<>();

        for (NodePanel node : nodes) {

            for (String key : node.inputDots.keySet()) {
                Dot d = node.inputDots.get(key);
                Dot nd = createDot(d.getType(), d.getParamType(), d.getVariableName(), d.getNodeName());

                for (Connection connection : connections) {
                    if (connection.getEnd() == d) {
                        NodePanel otherNode = connection.getStart().getNode();
                        if (nodes.contains(otherNode)) {
                            connection.setHidden(true);
                            connection.setGroup(group.getName());
                            continue;
                        }
                        connection.updateConnectPoints(connection.getStart(), nd);
                        d.disconnect(connection);
                        String k = "Group." + group.getName() + d.getNodeName() + "." + d.getVariableName();
                        internalConnections.put(k, new Connection(controller, k, null, d));
                        nd.connect(connection);
                        if (!inDots.contains(nd)) {
                            nd.setIndex(otherNode.getY());
                            inDots.add(nd);
                        }
                    }
                }
            }

            for (String key : node.outputDots.keySet()) {
                Dot d = node.outputDots.get(key);
                Dot nd = createDot(d.getType(), d.getParamType(), d.getVariableName(), d.getNodeName());

                for (Connection connection : connections) {
                    if (connection.getStart() == d) {
                        NodePanel otherNode = connection.getEnd().getNode();
                        if (nodes.contains(otherNode)) {
                            connection.setHidden(true);
                            connection.setGroup(group.getName());
                            continue;
                        }
                        connection.updateConnectPoints(nd, connection.getEnd());
                        d.disconnect(connection);
                        String k = "Group." + group.getName() + d.getNodeName() + "." + d.getVariableName();
                        internalConnections.put(k, new Connection(controller, k, null, d));
                        nd.connect(connection);
                        if (!outDots.contains(nd)) {
                            nd.setIndex(otherNode.getY());
                            outDots.add(nd);
                        }
                    }
                }
            }
        }

        layoutDots(inDots, inputDots, inputLabels);
        layoutDots(outDots, outputDots, outputLabels);

        init();
    }

    public void layoutDots(List<Dot> dots, Map<String, Dot> inputs, List<JLabel> labels) {
        Collections.sort(dots, new Comparator<Dot>() {
            @Override
            public int compare(Dot o1, Dot o2) {
                if (o1.getIndex() == o2.getIndex()) return 0;
                return o1.getIndex() < o2.getIndex() ? -1 : 1;
            }
        });

        int index = 0;
        for (Dot dot : dots) {
            dot.setIndex(index++);
            inputs.put(dot.getNodeName() + "." + dot.getVariableName(), dot);
            String name = dot.getNodeName().substring(0, 1).toLowerCase() + dot.getNodeName().substring(1) +
                    dot.getVariableName().substring(0, 1).toUpperCase() + dot.getVariableName().substring(1);
            JLabel l = createLabel(dot.getType(), name, dot.getParamType());
            labels.add(l);
        }
    }

    @Override
    public Shader.ShaderType getShaderType() {
        return null;
    }

    public void ungroup() {
        controller.ungroup(this.getNodeName());
    }

    public void expand() {
        controller.displayGroup(this.getNodeName());
    }

    @Override
    public void renameField(JLabel source, int index, boolean isInput) {
        super.renameField(source, index, isInput);
        JLabel gpLabel;
        NodePanel panel;
        if (isInput) {
            panel = group.getInputsPanel();
            gpLabel = panel.outputLabels.get(index);
        } else {
            panel = group.getOutputsPanel();
            gpLabel = panel.inputLabels.get(index);
        }
        controller.setFieldName(source.getText(), panel, gpLabel, index, isInput);

    }

    public void cleanup(List<NodePanel> nodes, List<Connection> connections) {
        cleanup();
        for (NodePanel node : nodes) {

            for (String key : node.inputDots.keySet()) {
                Dot d = node.inputDots.get(key);
                Dot gd = inputDots.get(key);
                for (Connection connection : connections) {
                    if (this.getNodeName().equals(connection.getGroup())) {
                        connection.setHidden(false);
                        connection.setGroup(null);
                    }
                    if (gd != null && connection.getEnd() == gd) {
                        connection.updateConnectPoints(connection.getStart(), d);
                        gd.disconnect(connection);
                        d.connect(connection);
                        connection.resize(connection.getStart(), connection.getEnd());
                    }
                }
            }

            for (String key : node.outputDots.keySet()) {
                Dot d = node.outputDots.get(key);
                Dot gd = outputDots.get(key);
                for (Connection connection : connections) {
                    if (gd != null && connection.getStart() == gd) {
                        connection.updateConnectPoints(d, connection.getEnd());
                        gd.disconnect(connection);
                        d.connect(connection);
                        connection.resize(connection.getStart(), connection.getEnd());
                    }
                }
            }

        }
    }

    @Override
    protected void initHeader(JLabel header) {
        header.setText(getNodeName());
        header.setToolTipText(getNodeName());
    }

}
