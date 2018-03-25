package spix.swing.materialEditor.nodes;

import com.jme3.shader.Shader;
import spix.swing.materialEditor.Connection;
import spix.swing.materialEditor.Dot;
import spix.swing.materialEditor.controller.Group;
import spix.swing.materialEditor.controller.MatDefEditorController;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;


/**
 * Created by Nehon on 13/05/2016.
 */
public abstract class GroupInOutPanel extends NodePanel {

    public enum Type {
        Inputs,
        Outputs
    }

    private ShaderNodeGroup groupPanel;
    private Type type;

    private GroupInOutPanel(MatDefEditorController controller, ShaderNodeGroup groupPanel, Type type, Map<String, Connection> internalConnections) {
        super(controller, "group." + groupPanel.getNodeName() + "." + type.name(), new Color(255, 255, 0, 255), Icons.group);
        canRenameFields = true;
        this.groupPanel = groupPanel;
        this.type = type;

        if (type == Type.Inputs) {
            createDots(groupPanel, internalConnections, Dot.ParamType.Output, groupPanel.inputLabels, outputLabels, groupPanel.inputDots, outputDots);
        } else {
            createDots(groupPanel, internalConnections, Dot.ParamType.Input, groupPanel.outputLabels, inputLabels, groupPanel.outputDots, inputDots);
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if(controller.getDragHandler().getDraggedFrom() != null) {
                    controller.getDragHandler().setDraggedTo(GroupInOutPanel.this);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (controller.getDragHandler().getDraggedTo() == GroupInOutPanel.this && e.getButton() == 1) {
                     controller.getDragHandler().setDraggedTo(null);
                }
            }
        });

        init();
    }

    public void draggedOver() {

        Dot dot = this.controller.getDragHandler().getDraggedFrom();
        if(dot!=null) {
            String name = dot.getNodeName().substring(0, 1).toLowerCase() + dot.getNodeName().substring(1) +
                    dot.getVariableName().substring(0, 1).toUpperCase() + dot.getVariableName().substring(1);
            if(type == Type.Inputs) {
                Dot d = createDot(dot.getType(), Dot.ParamType.Output, dot.getVariableName(), dot.getNodeName());
                insertOutput(d, createLabel(dot.getType(), name, Dot.ParamType.Output));
                controller.connect(d, dot);
                groupPanel.insertInput(groupPanel.createDot(dot.getType(), Dot.ParamType.Input, dot.getVariableName(), dot.getNodeName()), groupPanel.createLabel(dot.getType(), name, Dot.ParamType.Input));
            } else {
                Dot d = createDot(dot.getType(), Dot.ParamType.Input, dot.getVariableName(), dot.getNodeName());
                insertInput(d, createLabel(dot.getType(), name, Dot.ParamType.Input));
                controller.connect(dot, d);
                groupPanel.insertOutput(groupPanel.createDot(dot.getType(), Dot.ParamType.Output, dot.getVariableName(), dot.getNodeName()), groupPanel.createLabel(dot.getType(), name, Dot.ParamType.Output));
            }
        }
    }

    private void createDots(ShaderNodeGroup groupPanel, Map<String, Connection> internalConnections, Dot.ParamType dotType, List<JLabel> labels, List<JLabel> newLabels, Map<String, Dot> groupInputDots, Map<String, Dot> newOutputDots) {
        for (String key : groupInputDots.keySet()) {
            Dot d = groupInputDots.get(key);
            Dot nd = createDot(d.getType(), dotType, d.getVariableName(), d.getNodeName());
            String k = "Group." + groupPanel.getNodeName() + d.getNodeName() + "." + d.getVariableName();
            Connection c = internalConnections.get(k);
            c.setGroup(groupPanel.getNodeName());
            c.updateConnectPoints(nd, c.getEnd());
            nd.connect(c);
            c.getEnd().connect(c);
            newOutputDots.put(key, nd);
        }
        for (JLabel label : labels) {
            newLabels.add(createLabel("", label.getText(), dotType));
        }
    }

    @Override
    public void renameField(JLabel source, int index, boolean isInput) {
        super.renameField(source, index, isInput);
        JLabel gpLabel;
        if(type == Type.Inputs){
            gpLabel = groupPanel.inputLabels.get(index);
        } else {
            gpLabel = groupPanel.outputLabels.get(index);
        }
        controller.setFieldName(source.getText(), groupPanel, gpLabel, index, isInput);

    }

    public static GroupInOutPanel create(MatDefEditorController controller, ShaderNodeGroup groupPanel, Type type, Map<String, Connection> internalConnections) {

        return new GroupInOutPanel(controller, groupPanel, type, internalConnections) {

            @Override
            public Shader.ShaderType getShaderType() {
                return null;
            }

            @Override
            protected void initHeader(JLabel header) {
                header.setText(groupPanel.getNodeName() + (type == Type.Inputs ? " Inputs" : " Outputs"));
            }
        };
    }
}
