package spix.swing.materialEditor;

import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.scene.Geometry;
import com.jme3.shader.*;
import groovy.util.ObservableList;
import spix.app.DefaultConstants;
import spix.app.utils.CloneUtils;
import spix.app.utils.MaterialDefUtils;
import spix.core.SelectionModel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.nodes.NodePanel;
import spix.swing.materialEditor.nodes.NodePanelFactory;
import spix.swing.materialEditor.nodes.inOut.FragmentColorPanel;
import spix.swing.materialEditor.nodes.inOut.VertexPositionPanel;
import spix.swing.materialEditor.nodes.inputs.AttributePanel;
import spix.swing.materialEditor.nodes.inputs.MatParamPanel;
import spix.swing.materialEditor.nodes.inputs.WorldParamPanel;
import spix.swing.materialEditor.nodes.shadernodes.FragmentNodePanel;
import spix.swing.materialEditor.nodes.shadernodes.VertexNodePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
//        diagram.setPreferredSize(new Dimension(scrollPane.getWidth() - 2, scrollPane.getHeight() - 2));
//        diagram.revalidate();
        scrollPane.addComponentListener(diagram);
        //diagram.addOutBus(new OutBusPanel("Test", Shader.ShaderType.Fragment));

//        diagram.addNode(new AttributePanel(new ShaderNodeVariable("vec2", "inTexCoord")));
//        diagram.addNode(new WorldParamPanel(new ShaderNodeVariable("mat4", "WorldProjectionMatrix")));
//        diagram.addNode(new MatParamPanel(new ShaderNodeVariable("vec4", "Color")));
//
//        diagram.addNode(new VertexPositionPanel());
//        diagram.addNode(new FragmentColorPanel());
//
//        diagram.autoLayout();
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

    private void initDiagram(TechniqueDef technique, MaterialDef matDef ) {

        diagram.clear();

        if (technique.isUsingShaderNodes()) {
            MaterialDefUtils.computeShaderNodeGenerationInfo(technique);
            List<ShaderNodeVariable> uniforms = new ArrayList<>();
            MaterialDefUtils.getAllUniforms(technique, matDef, uniforms);

            for (ShaderNode sn : technique.getShaderNodes()) {
                NodePanel np = NodePanelFactory.createShaderNodePanel(sn);
                diagram.addNode(np);
            }

            if (technique.getShaderGenerationInfo().getVertexGlobal() != null) {
               // NodePanel np = new VertexPositionPanel();
                OutBusPanel out = new OutBusPanel(technique.getShaderGenerationInfo().getVertexGlobal().getName(), Shader.ShaderType.Vertex);
                diagram.addOutBus(out);
               // diagram.addNode(np);
            }


            for (ShaderNodeVariable var : technique.getShaderGenerationInfo().getFragmentGlobals()) {
                OutBusPanel out2 = new OutBusPanel(var.getName(), Shader.ShaderType.Fragment);
                diagram.addOutBus(out2);
//                NodePanel np = new FragmentColorPanel();
//                diagram.addNode(np);
            }
            for (ShaderNodeVariable shaderNodeVariable : technique.getShaderGenerationInfo().getAttributes()) {
                NodePanel np = diagram.getNodePanel(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
                if (np == null) {
                    np = new AttributePanel(shaderNodeVariable);
                    diagram.addNode(np);
                }
            }

            for (ShaderNodeVariable shaderNodeVariable : uniforms) {
                NodePanel np = diagram.getNodePanel(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
                if (np == null) {
                    if(shaderNodeVariable.getNameSpace().equals("MatParam") ){
                        np = new MatParamPanel(shaderNodeVariable);
                    } else {
                        np = new WorldParamPanel(shaderNodeVariable);
                    }
                    diagram.addNode(np);
                }
            }

            for (ShaderNode sn : technique.getShaderNodes()) {
                //NodePanel np = diagram1.getNodePanel(sn.getName());
                List<VariableMapping> ins = sn.getInputMapping();
                if (ins != null) {
                    for (VariableMapping mapping : ins) {
                        makeConnection(mapping, technique);
//                        if (!mapping.getRightNameSpace().equals("Global")
//                                && !mapping.getRightNameSpace().equals("MatParam")
//                                && !mapping.getRightNameSpace().equals("Attribute")
//                                && !mapping.getRightNameSpace().equals("WorldParam")) {
//                            sn.addInputNode(mapping.getRightNameSpace());
//                        } else if (mapping.getRightNameSpace().equals("Global")) {
//                            sn.setGlobalInput(true);
//                        }
                    }
                }
                List<VariableMapping> outs = sn.getOutputMapping();
                if (outs != null) {
                    for (VariableMapping mapping : outs) {
                        makeConnection(mapping, technique);
//                        if (mapping.getLeftNameSpace().equals("Global")) {
//                            sn.setGlobalOutput(true);
//                        }
                    }
                }

            }
        }

        diagram.autoLayout();
    }

    private void makeConnection(VariableMapping mapping, TechniqueDef technique) {

        Dot leftDot = findConnectPoint(mapping.getLeftVariable().getNameSpace(), mapping.getLeftVariable().getName(), true);
        Dot rightDot = findConnectPoint(mapping.getRightVariable().getNameSpace(), mapping.getRightVariable().getName(), false);
        Connection conn = diagram.connect(leftDot, rightDot);
      //  mapping.addPropertyChangeListener(WeakListeners.propertyChange(conn, mapping));
      //  conn.makeKey(mapping, technique.getName());
    }

    private Dot findConnectPoint(String nameSpace, String name, boolean isInput) {

        if (nameSpace.equals("MatParam")
                || nameSpace.equals("WorldParam")
                || nameSpace.equals("Attr")) {
            NodePanel np = diagram.getNodePanel(nameSpace + "." + name);
            return isInput ? np.getInputConnectPoint(name) : np.getOutputConnectPoint(name);
        } else if (nameSpace.equals("Global")) {
            OutBusPanel outBus = diagram.getOutBusPanel(name);
            return outBus.getConnectPoint();
        } else {
            NodePanel np = diagram.getNodePanel(diagram.getCurrentTechniqueName() + "/" + nameSpace);
            return isInput ? np.getInputConnectPoint(name) : np.getOutputConnectPoint(name);
        }
    }


}
