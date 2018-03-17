package spix.swing.materialEditor.nodes;

import com.jme3.shader.*;
import spix.swing.materialEditor.Connection;
import spix.swing.materialEditor.Dot;
import spix.swing.materialEditor.controller.MatDefEditorController;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Created by Nehon on 13/05/2016.
 */
public abstract class ShaderNodeGroup extends NodePanel {

    private boolean editionAllowed = false;

    private ShaderNodeGroup(MatDefEditorController controller, String groupName, List<NodePanel> nodes, List<Connection> connections) {
        super(controller, "group." + groupName, new Color(180, 180, 225, 200), Icons.group);
        backgroundColor = new Color(100, 100, 130, 200);
        setNodeName(groupName);

        for (NodePanel node : nodes) {

            for (String key : node.inputDots.keySet()) {
                Dot d = node.inputDots.get(key);
                Dot nd = createDot(d.getType(), d.getParamType(), d.getVariableName(), d.getNodeName());

                for (Connection connection : connections) {
                    if (connection.getEnd() == d) {
                        NodePanel otherNode = connection.getStart().getNode();
                        if (nodes.contains(otherNode)) {
                            connection.setHidden(true);
                            continue;
                        }
                        connection.updateConnectPoints(connection.getStart(), nd);
                        d.disconnect(connection);
                        nd.connect(connection);
                        if (!inputDots.values().contains(nd)) {
                            inputDots.put(key, nd);
                            String name = connection.getStart().getVariableName();
                            JLabel l = createLabel(d.getType(), name, d.getParamType());
                            inputLabels.add(l);
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
                            continue;
                        }
                        connection.updateConnectPoints(nd, connection.getEnd());
                        d.disconnect(connection);
                        nd.connect(connection);
                        if (!outputDots.values().contains(nd)) {
                            outputDots.put(key, nd);
                            String name = connection.getEnd().getVariableName();
                            JLabel l = createLabel(d.getType(), name, d.getParamType());
                            outputLabels.add(l);
                        }
                    }
                }
            }

        }
        init();
    }

    public void ungroup() {
        controller.ungroup(this.getNodeName());
    }


    public void cleanup(List<NodePanel> nodes, List<Connection> connections) {
        cleanup();
        for (NodePanel node : nodes) {

            for (String key : node.inputDots.keySet()) {
                Dot d = node.inputDots.get(key);
                Dot gd = inputDots.get(key);
                for (Connection connection : connections) {
                    if (connection.getEnd() == d) {
                        connection.setHidden(false);
                    } else if (gd != null && connection.getEnd() == gd) {
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
                    if (connection.getStart() == d) {
                        connection.setHidden(false);
                    } else if (gd != null && connection.getStart() == gd) {
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


    public static ShaderNodeGroup create(MatDefEditorController controller, String groupName, List<NodePanel> shaderNodes, List<Connection> connections) {

        return new ShaderNodeGroup(controller, groupName, shaderNodes, connections) {
            @Override
            public Shader.ShaderType getShaderType() {
                return null;
            }
        };
    }


}
