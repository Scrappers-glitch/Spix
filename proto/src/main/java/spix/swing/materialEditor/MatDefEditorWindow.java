package spix.swing.materialEditor;

import com.jme3.material.*;
import com.jme3.scene.Geometry;
import com.jme3.shader.*;
import groovy.util.ObservableList;
import spix.app.DefaultConstants;
import spix.app.utils.*;
import spix.core.SelectionModel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.nodes.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Created by Nehon on 11/05/2016.
 */
public class MatDefEditorWindow extends JFrame {

    public static final String MAT_DEF_EDITOR_WIDTH = "MatDefEditor.width";
    public static final String MAT_DEF_EDITOR_HEIGHT = "MatDefEditor.height";
    public static final String MAT_DEF_EDITOR_X = "MatDefEditor.x";
    public static final String MAT_DEF_EDITOR_Y = "MatDefEditor.y";
    private Preferences prefs = Preferences.userNodeForPackage(MatDefEditorWindow.class);
    private SwingGui gui;
    private SelectionModel sceneSelection;
    private SceneSelectionChangeListener sceneSelectionChangeListener = new SceneSelectionChangeListener();
    private MaterialDef currentMatdef;
    private List<TechniqueDef> techniques = new ArrayList<>();
    private Diagram diagram;

    public MatDefEditorWindow(SwingGui gui) {
        super("Material definition editor");
        this.gui = gui;
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                prefs.putInt(MAT_DEF_EDITOR_WIDTH, e.getComponent().getWidth());
                prefs.putInt(MAT_DEF_EDITOR_HEIGHT, e.getComponent().getHeight());
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                prefs.putInt(MAT_DEF_EDITOR_X, e.getComponent().getX());
                prefs.putInt(MAT_DEF_EDITOR_Y, e.getComponent().getY());
            }
        });

        diagram = new Diagram(gui);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(diagram);
        getContentPane().add(scrollPane);
        scrollPane.addComponentListener(diagram);

        sceneSelection = gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        sceneSelection.addPropertyChangeListener(sceneSelectionChangeListener);

    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            int width = prefs.getInt(MAT_DEF_EDITOR_WIDTH, 300);
            int height = prefs.getInt(MAT_DEF_EDITOR_HEIGHT, 150);

            int x = prefs.getInt(MAT_DEF_EDITOR_X, 300);
            int y = prefs.getInt(MAT_DEF_EDITOR_Y, 150);

            setSize(new Dimension(width, height));
            setLocation(x, y);
        }
        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        sceneSelection.removePropertyChangeListener(sceneSelectionChangeListener);
        super.dispose();
    }

    public SwingGui getGui() {
        return gui;
    }

    private class SceneSelectionChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {

            //I receive this event before the property change.
            //Ask Paul about this as it seems it's intentional.
            if (evt instanceof ObservableList.ElementUpdatedEvent) {
                return;
            }

            if (evt.getNewValue() instanceof Geometry) {
                Geometry g = (Geometry) evt.getNewValue();
                try {
                    currentMatdef = CloneUtils.cloneMatDef(g.getMaterial().getMaterialDef(), techniques);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }


                gui.runOnSwing(new Runnable() {
                    @Override
                    public void run() {
                        setTitle(currentMatdef.getName());
                        initDiagram(techniques.get(0), currentMatdef);
                    }
                });

            }
        }
    }

    private void initDiagram(TechniqueDef technique, MaterialDef matDef) {

        diagram.clear();
        diagram.setCurrentTechniqueName(technique.getName());

        if (technique.isUsingShaderNodes()) {
            MaterialDefUtils.computeShaderNodeGenerationInfo(technique);
            List<ShaderNodeVariable> uniforms = new ArrayList<>();
            MaterialDefUtils.getAllUniforms(technique, matDef, uniforms);

//            ShaderNodeVariable inPosition = new ShaderNodeVariable("vec4", "Attr", "inPosition");
//            ShaderNodeVariable position = new ShaderNodeVariable("vec4", "Global", "position");
//            diagram.addNode(new AttributePanel(inPosition));
            //makeConnection(new VariableMapping(position,"", inPosition,"",""), technique);

            for (ShaderNode sn : technique.getShaderNodes()) {
                NodePanel np = ShaderNodePanel.create(sn);
                diagram.addNode(np);
            }

            for (ShaderNodeVariable shaderNodeVariable : technique.getShaderGenerationInfo().getAttributes()) {
                NodePanel np = diagram.getNodePanel(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
                if (np == null) {
                    np = InputPanel.create(InputPanel.ShaderInputType.Attribute, shaderNodeVariable);
                    diagram.addNode(np);
                }
            }

            for (ShaderNodeVariable shaderNodeVariable : uniforms) {
                NodePanel np = diagram.getNodePanel(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
                if (np == null) {
                    if (shaderNodeVariable.getNameSpace().equals("MatParam")) {
                        np = InputPanel.create(InputPanel.ShaderInputType.MatParam, shaderNodeVariable);
                    } else {
                        np = InputPanel.create(InputPanel.ShaderInputType.WorldParam, shaderNodeVariable);
                    }
                    diagram.addNode(np);
                }
            }

            for (ShaderNode sn : technique.getShaderNodes()) {
                List<VariableMapping> ins = sn.getInputMapping();
                if (ins != null) {
                    for (VariableMapping mapping : ins) {
                        makeConnection(mapping, technique, sn.getName());
                    }
                }
                List<VariableMapping> outs = sn.getOutputMapping();
                if (outs != null) {
                    for (VariableMapping mapping : outs) {
                        makeConnection(mapping, technique, sn.getName());
                    }
                }

            }
        }

        diagram.autoLayout();
    }

    private void makeConnection(VariableMapping mapping, TechniqueDef technique, String nodeName) {
        NodePanel forNode = diagram.getNodePanel(diagram.getCurrentTechniqueName() + "/" + nodeName);

        Dot leftDot = findConnectPointForInput(mapping, forNode);
        Dot rightDot = findConnectPointForOutput(mapping, forNode);
        Connection conn = diagram.connect(leftDot, rightDot);
        //  mapping.addPropertyChangeListener(WeakListeners.propertyChange(conn, mapping));
        //  conn.makeKey(mapping, technique.getName());
    }


    private Dot findConnectPointForInput(VariableMapping mapping, NodePanel forNode) {
        String nameSpace = mapping.getLeftVariable().getNameSpace();
        String name = mapping.getLeftVariable().getName();
        return getNodePanelForConnection(forNode, nameSpace, name, true).getInputConnectPoint(name);
    }

    private Dot findConnectPointForOutput(VariableMapping mapping, NodePanel forNode) {
        String nameSpace = mapping.getRightVariable().getNameSpace();
        String name = mapping.getRightVariable().getName();
        return getNodePanelForConnection(forNode, nameSpace, name, false).getOutputConnectPoint(name);
    }

    private NodePanel getNodePanelForConnection(NodePanel forNode, String nameSpace, String name, boolean forInput) {
        NodePanel np = null;
        if (isShaderInput(nameSpace)) {
            np = diagram.getNodePanel(nameSpace + "." + name);
        } else if (isGlobal(nameSpace)) {
            np = diagram.getOutPanel(forNode.getShaderType(), new ShaderNodeVariable("vec4", "Global", name), forNode, forInput);
        } else {
            np = diagram.getNodePanel(diagram.getCurrentTechniqueName() + "/" + nameSpace);
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
