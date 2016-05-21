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

import java.beans.*;
import java.util.*;

/**
 * Created by Nehon on 18/05/2016.
 */
public class MaterialDefController {

    private MaterialDef matDef;
    private MatDefEditorWindow editor;
    private SwingGui gui;
    private DragController dragController = new DragController();
    private SelectionModel sceneSelection;
    private SceneSelectionChangeListener sceneSelectionChangeListener = new SceneSelectionChangeListener();
    private List<TechniqueDef> techniques = new ArrayList<>();
    private TechniqueController techniqueController = new TechniqueController();


    public MaterialDefController(SwingGui gui, MatDefEditorWindow editor) {
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

    public DragController getDragController() {
        return dragController;
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
                        techniqueController.initDiagram(MaterialDefController.this, techniques.get(0), matDef, editor.getDiagram());
                    }
                });

            }
        }
    }

    public Connection connect(Dot start, Dot end) {
        return techniqueController.connect(this, start, end, editor.getDiagram());
    }

    public void removeConnection(Connection conn) {
        techniqueController.removeConnection(conn, editor.getDiagram());
    }

    public void removeNode(String key){
        techniqueController.removeNode(key, editor.getDiagram());
    }

    public void addNode(NodePanel node){
        techniqueController.addNode(node, editor.getDiagram());
    }


}
