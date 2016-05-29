package spix.swing.materialEditor.nodes;

import com.jme3.shader.*;
import spix.app.material.MaterialAppState;
import spix.swing.materialEditor.*;
import spix.swing.materialEditor.controller.MatDefEditorController;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.preview.PreviewRequest;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by bouquet on 15/05/16.
 */
public abstract class OutPanel extends NodePanel {

    private String varName = "";
    private MaterialAppState.DisplayType displayType = MaterialAppState.DisplayType.Box;
    private OutToolBar outToolBar;

    private OutPanel(MatDefEditorController controller, String key, ShaderNodeVariable var, Color color, Icon icon) {
        super(controller, key, color, icon);
        var.setShaderOutput(true);
        displayPreview = true;
        varName = var.getName();
        setNodeName(var.getNameSpace());
        List<ShaderNodeVariable> outputs = new ArrayList<ShaderNodeVariable>();
        outputs.add(new ShaderNodeVariable(var.getType(), var.getName()));
        List<ShaderNodeVariable> inputs = new ArrayList<ShaderNodeVariable>();
        inputs.add(new ShaderNodeVariable(var.getType(), var.getName()));

        outToolBar = new OutToolBar(this);
        init(inputs, outputs);
    }

    public String getVarName() {
        return varName;
    }

    public boolean isInputAvailable() {
        return !inputDots.values().iterator().next().isConnected();
    }

    public boolean isOutputAvailable() {
        return !outputDots.values().iterator().next().isConnected();
    }

    public Dot getInputConnectPoint() {
        return inputDots.values().iterator().next();
    }

    public Dot getOutputConnectPoint() {
        return inputDots.values().iterator().next();
    }

    public void setDisplayType(MaterialAppState.DisplayType displayType) {
        this.displayType = displayType;
        controller.refreshPreviews();
    }

    public PreviewRequest makePreviewRequest() {

        String nodeName = getForNodeName();
        return new PreviewRequest(getShaderType(), nodeName, previewLabel, displayType);
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        if(!selected) {
            if (outToolBar.isVisible()) {
                outToolBar.setVisible(false);
            }
        }
    }

    @Override
    protected void showToolBar() {
        super.showToolBar();
        outToolBar.display();
    }

    @Override
    public void cleanup() {
        super.cleanup();
        outToolBar.cleanup();
    }

    public String getForNodeName() {
        String nodeName = null;
        for (Dot dot : getInputConnectPoint().getConnectedDots()) {
            nodeName = dot.getNode().getName();
        }
        return nodeName;
    }

    public static OutPanel create(MatDefEditorController controller, String key, Shader.ShaderType type, ShaderNodeVariable var) {
        Color color = new Color(0, 0, 0);
        Icon icon = Icons.output;
        switch (type) {
            case Vertex:
                color = new Color(220, 220, 70);
                icon = Icons.outputV;
                break;
            case Fragment:
                color = new Color(114, 200, 255);
                break;
            case Geometry:
                color = new Color(250, 150, 0);
                break;
            case TessellationControl:
                color = new Color(150, 100, 255);
                break;
            case TessellationEvaluation:
                color = new Color(250, 150, 255);
                break;
        }

        return new OutPanel(controller, key, var, color, icon) {
            @Override
            public Shader.ShaderType getShaderType() {
                return type;
            }

            @Override
            protected void initHeader(JLabel header) {
                //header.setText(getVarName() + " (" +type.name().substring(0,4)+")");
                header.setText(type.name() + ": " + getVarName());
                header.setToolTipText(type.name() + " shader output");
            }
        };
    }


}
