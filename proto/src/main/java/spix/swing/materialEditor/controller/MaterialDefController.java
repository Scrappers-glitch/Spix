package spix.swing.materialEditor.controller;

import com.jme3.material.*;
import com.jme3.scene.Geometry;
import com.jme3.shader.*;
import groovy.util.ObservableList;
import spix.app.DefaultConstants;
import spix.app.utils.*;
import spix.core.SelectionModel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.*;
import spix.swing.materialEditor.nodes.*;

import java.beans.*;
import java.util.*;

/**
 * Created by Nehon on 18/05/2016.
 */
public class MaterialDefController {

    private MaterialDef matDef;
    private MatDefEditorWindow editor;
    private SwingGui gui;
    private DragController dragController;
    private SelectionModel sceneSelection;
    private SceneSelectionChangeListener sceneSelectionChangeListener = new SceneSelectionChangeListener();
    private List<TechniqueDef> techniques = new ArrayList<>();
    private TechniqueDef currentTechnique;

    public MaterialDefController(SwingGui gui, MatDefEditorWindow editor){
        this.gui = gui;
        this.editor = editor;

        this.dragController = new DragController();
        sceneSelection = gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        sceneSelection.addPropertyChangeListener(sceneSelectionChangeListener);
    }

    public void cleanup(){
        sceneSelection.removePropertyChangeListener(sceneSelectionChangeListener);
        matDef = null;
        techniques.clear();
    }

    public MatDefEditorWindow getEditor() {
        return editor;
    }

    public DragController getDragController() {
        return dragController;
    }

    public TechniqueDef getCurrentTechnique() {
        return currentTechnique;
    }

    private void initDiagram(TechniqueDef technique, MaterialDef matDef) {
        Diagram diagram = editor.getDiagram();
        diagram.clear();
        currentTechnique = technique;

        if (technique.isUsingShaderNodes()) {
            MaterialDefUtils.computeShaderNodeGenerationInfo(technique);
            List<ShaderNodeVariable> uniforms = new ArrayList<>();
            MaterialDefUtils.getAllUniforms(technique, matDef, uniforms);

//            ShaderNodeVariable inPosition = new ShaderNodeVariable("vec4", "Attr", "inPosition");
//            ShaderNodeVariable position = new ShaderNodeVariable("vec4", "Global", "position");
//            diagram.addNode(new AttributePanel(inPosition));
            //makeConnection(new VariableMapping(position,"", inPosition,"",""), technique);

            for (ShaderNode sn : technique.getShaderNodes()) {
                NodePanel np = ShaderNodePanel.create(this, sn);
                diagram.addNode(np);
            }

            for (ShaderNodeVariable shaderNodeVariable : technique.getShaderGenerationInfo().getAttributes()) {
                NodePanel np = diagram.getNodePanel(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
                if (np == null) {
                    np = InputPanel.create(this, InputPanel.ShaderInputType.Attribute, shaderNodeVariable);
                    diagram.addNode(np);
                }
            }

            for (ShaderNodeVariable shaderNodeVariable : uniforms) {
                NodePanel np = diagram.getNodePanel(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
                if (np == null) {
                    if (shaderNodeVariable.getNameSpace().equals("MatParam")) {
                        np = InputPanel.create(this, InputPanel.ShaderInputType.MatParam, shaderNodeVariable);
                    } else {
                        np = InputPanel.create(this, InputPanel.ShaderInputType.WorldParam, shaderNodeVariable);
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
        Diagram diagram = editor.getDiagram();
        NodePanel forNode = diagram.getNodePanel(currentTechnique.getName() + "/" + nodeName);

        Dot leftDot = findConnectPointForInput(mapping, forNode);
        Dot rightDot = findConnectPointForOutput(mapping, forNode);
        Connection conn = connect(leftDot, rightDot);
        //  mapping.addPropertyChangeListener(WeakListeners.propertyChange(conn, mapping));
        //  conn.makeKey(mapping, technique.getName());
    }

    public Connection connect(Dot start, Dot end) {
        Connection conn = new Connection(this, start, end);
        start.connect(conn);
        end.connect(conn);
        editor.getDiagram().addConnection(conn);
        notifyMappingCreation(conn);
        return conn;
    }

    //todo this may not be needed in the future...
    public void notifyMappingCreation(Connection conn) {
        //    parent.makeMapping(conn);
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
        Diagram diagram = editor.getDiagram();
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
                    matDef = CloneUtils.cloneMatDef(g.getMaterial().getMaterialDef(), techniques);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }


                gui.runOnSwing(new Runnable() {
                    @Override
                    public void run() {
                        editor.setTitle(matDef.getName());
                        initDiagram(techniques.get(0), matDef);
                    }
                });

            }
        }
    }

}
