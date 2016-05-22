package spix.swing.materialEditor.controller;

import com.jme3.material.*;
import com.jme3.scene.Geometry;
import groovy.util.ObservableList;
import spix.app.DefaultConstants;
import spix.app.utils.CloneUtils;
import spix.core.SelectionModel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.*;
import spix.swing.materialEditor.nodes.NodePanel;
import spix.util.SwingUtils;

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
    private DataHandler dataHandler = new DataHandler();
    private SelectionHandler selectionHandler = new SelectionHandler();


    public MatDefEditorController(SwingGui gui, MatDefEditorWindow editor) {
        this.gui = gui;
        this.editor = editor;
        sceneSelection = gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        sceneSelection.addPropertyChangeListener(sceneSelectionChangeListener);
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
                        dataHandler.initDiagram(MatDefEditorController.this, techniques.get(0), matDef, editor.getDiagram());
                    }
                });

            }
        }
    }

    public Connection connect(Dot start, Dot end) {
        Connection conn = dataHandler.connect(this, start, end, editor.getDiagram());
        repaintDiagram();

        return conn;
    }

    public void removeConnection(Connection conn) {
        dataHandler.removeConnection(conn, editor.getDiagram());
        repaintDiagram();
    }

    public void removeNode(String key){
        dataHandler.removeNode(key, editor.getDiagram());
        repaintDiagram();
    }

    public void addNode(NodePanel node){
        dataHandler.addNode(node, editor.getDiagram());
    }

    public void multiMove(DraggablePanel movedPanel, int xOffset, int yOffset) {
        selectionHandler.multiMove(movedPanel, xOffset, yOffset);
    }

    public void multiStartDrag(DraggablePanel movedPanel) {
        selectionHandler.multiStartDrag(movedPanel);
    }


    public void select(Selectable selectable, boolean multi ){
        selectionHandler.select(selectable, multi);
        repaintDiagram();
    }

    public void findSelection(MouseEvent me, boolean multi){
        //Click on the diagram, we are trying to find if we clicked in a connection area and select it
        Connection conn = dataHandler.pickForConnection(me, editor.getDiagram());

        if( conn != null) {
            select(conn, multi);
            me.consume();
            return;
        }

        //we didn't find anything, let's unselect
        selectionHandler.clearSelection();
        repaintDiagram();
    }

    public void removeSelected(){

        int result = JOptionPane.showConfirmDialog(editor, "Delete all selected items, nodes and mappings?", "Delete Selected", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            selectionHandler.removeSelected(this);
        }
    }

    public void dispatchEventToDiagram(MouseEvent e, Component source) {
        MouseEvent me = SwingUtils.convertEvent(source, e, editor.getDiagram());
        editor.getDiagram().dispatchEvent(me);
    }

    void repaintDiagram() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                editor.getDiagram().repaint();
            }
        });

    }

}
