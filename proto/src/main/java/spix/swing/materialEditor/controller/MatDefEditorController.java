package spix.swing.materialEditor.controller;

import com.jme3.material.*;
import com.jme3.scene.Geometry;
import com.jme3.shader.*;
import groovy.util.ObservableList;
import spix.app.DefaultConstants;
import spix.app.utils.CloneUtils;
import spix.core.SelectionModel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.*;
import spix.swing.materialEditor.dialog.*;
import spix.swing.materialEditor.nodes.*;
import spix.swing.materialEditor.utils.MaterialDefUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.*;
import java.util.*;
import java.util.List;

/**
 * Created by Nehon on 18/05/2016.
 */
public class MatDefEditorController {

    private MaterialDef matDef;
    private MatDefEditorWindow editor;
    private SwingGui gui;
    private DragHandler dragHandler = new DragHandler();
    private SelectionModel sceneSelection;
    private SceneSelectionChangeListener sceneSelectionChangeListener = new SceneSelectionChangeListener();
    private List<TechniqueDef> techniques = new ArrayList<>();
    private DiagramUiHandler diagramUiHandler;
    private SelectionHandler selectionHandler = new SelectionHandler();
    private DataHandler dataHandler = new DataHandler();


    public MatDefEditorController(SwingGui gui, MatDefEditorWindow editor) {
        this.gui = gui;
        this.editor = editor;
        sceneSelection = gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        sceneSelection.addPropertyChangeListener(sceneSelectionChangeListener);
        diagramUiHandler = new DiagramUiHandler(this);
    }

    public void cleanup() {
        sceneSelection.removePropertyChangeListener(sceneSelectionChangeListener);
        matDef = null;
        techniques.clear();
    }

    public MatDefEditorWindow getEditor() {
        return editor;
    }

    public DragHandler getDragHandler() {
        return dragHandler;
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
                        initTechnique(techniques.get(0), matDef);
                    }
                });

            }
        }
    }

    void initTechnique(TechniqueDef technique, MaterialDef matDef) {
        diagramUiHandler.setCurrentTechniqueName(technique.getName());
        dataHandler.setCurrentTechnique(technique);

        if (technique.isUsingShaderNodes()) {
            MaterialDefUtils.computeShaderNodeGenerationInfo(technique);
            List<ShaderNodeVariable> uniforms = new ArrayList<>();
            MaterialDefUtils.getAllUniforms(technique, matDef, uniforms);

            for (ShaderNode sn : technique.getShaderNodes()) {
                diagramUiHandler.addShaderNodePanel(this, sn);
                dataHandler.registerShaderNode(sn);
            }

            for (ShaderNodeVariable shaderNodeVariable : technique.getShaderGenerationInfo().getAttributes()) {
                diagramUiHandler.addInputPanel(this, shaderNodeVariable);
            }

            for (ShaderNodeVariable shaderNodeVariable : uniforms) {
                diagramUiHandler.addInputPanel(this, shaderNodeVariable);
            }

            for (ShaderNode sn : technique.getShaderNodes()) {
                List<VariableMapping> ins = sn.getInputMapping();
                if (ins != null) {
                    for (VariableMapping mapping : ins) {
                        diagramUiHandler.makeConnection(this, mapping, technique, sn.getName());
                        dataHandler.registerMapping(mapping);
                    }
                }
                List<VariableMapping> outs = sn.getOutputMapping();
                if (outs != null) {
                    for (VariableMapping mapping : outs) {
                        diagramUiHandler.makeConnection(this, mapping, technique, sn.getName());
                        dataHandler.registerMapping(mapping);
                    }
                }
            }
        }

        //this will change when we have meta data
        diagramUiHandler.autoLayout();
    }

    public Connection connect(Dot start, Dot end) {
        Connection conn = diagramUiHandler.connect(this, start, end);
        dataHandler.addMapping(MaterialDefUtils.createVariableMapping(start, end));
        return conn;
    }

    public void removeConnection(Connection conn) {

        diagramUiHandler.removeConnection(conn);
        dataHandler.removeMappingForKey(conn.getKey());
    }

    public void removeNode(String key) {

        /*the order in those calls is very important.
            1. the node deletion in the ui
                we also iterate over the connections and delete them from the UI AND from the data model
            2. the node deletion in the data model.

            This allow to avoid iterating in all the shader in the data model to find inputs of other nodes that were
            coming from this node
        */
        diagramUiHandler.removeNode(this, key);
        dataHandler.removeShaderNodeForKey(key);
    }


    public void addShaderNode(ShaderNode sn) {
        diagramUiHandler.addShaderNodePanel(this, sn);
        // TODO: 22/05/2016 actually add the node to the techniqueDef
    }

    public NodePanel addOutPanel(Shader.ShaderType type, ShaderNodeVariable var) {
        NodePanel node = diagramUiHandler.makeOutPanel(this, type, var);
        return node;
    }

    public void multiMove(DraggablePanel movedPanel, int xOffset, int yOffset) {
        selectionHandler.multiMove(movedPanel, xOffset, yOffset);
    }

    public void multiStartDrag(DraggablePanel movedPanel) {
        selectionHandler.multiStartDrag(movedPanel);
    }


    public void select(Selectable selectable, boolean multi) {
        selectionHandler.select(selectable, multi);
        diagramUiHandler.refreshDiagram();
    }

    public void findSelection(MouseEvent me, boolean multi) {
        //Click on the diagram, we are trying to find if we clicked in a connection area and select it
        Connection conn = diagramUiHandler.pickForConnection(me);

        if (conn != null) {
            select(conn, multi);
            me.consume();
            return;
        }

        //we didn't find anything, let's unselect
        selectionHandler.clearSelection();
        diagramUiHandler.refreshDiagram();
    }

    public void removeSelected() {

        int result = JOptionPane.showConfirmDialog(editor, "Delete all selected items, nodes and mappings?", "Delete Selected", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            selectionHandler.removeSelected(this);
        }
    }

    public void dispatchEventToDiagram(MouseEvent e, Component source) {
        diagramUiHandler.dispatchEventToDiagram(e, source);
    }

    public void onNodeMoved(NodePanel node) {
        diagramUiHandler.fitContent();
        // TODO: 22/05/2016 save the location of the node in the metadata
    }

    public void displayAddAttibuteDialog(Point clickPosition) {
        AddAttributeDialog d = new AddAttributeDialog(editor, true, this, clickPosition);
        d.setVisible(true);
    }
    public void addAttribute(String name, String type, Point point) {
        ShaderNodeVariable param = new ShaderNodeVariable(type, "Attr", name);
        NodePanel node = diagramUiHandler.addInputPanel(this, param);
        node.setLocation(point);
        diagramUiHandler.refreshDiagram();
    }

    public void displayAddMatParamDialog(Point clickPosition) {
        AddMaterialParameterDialog d = new AddMaterialParameterDialog(editor, true, this, clickPosition);
        d.setVisible(true);
    }
    public void addMatParam(String name, String type, Point point) {
        if (type.equals("Color")) {
            type = "Vector4";
        }
        type = VarType.valueOf(type).getGlslType();
        ShaderNodeVariable param = new ShaderNodeVariable(type, "MatParam", name);
        NodePanel node = diagramUiHandler.addInputPanel(this, param);
        node.setLocation(point);
        diagramUiHandler.refreshDiagram();
    }
}
