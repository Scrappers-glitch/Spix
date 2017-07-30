package spix.app.scene;

import com.jme3.light.Light;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import spix.swing.SwingGui;
import spix.undo.UndoManager;
import spix.undo.edit.*;

/**
 * Created by nehon on 29/07/17.
 */
public class SceneService {

    private SwingGui gui;

    public SceneService( SwingGui gui) {
        this.gui = gui;
    }

    private UndoManager getUndoManager(){
        return gui.getSpix().getService(UndoManager.class);
    }

    public void moveSpatial(Spatial spatial, Node newParent){
        gui.runOnRender(new Runnable() {
            @Override
            public void run() {

                SpatialRemoveEdit remEdit = new SpatialRemoveEdit(spatial.getParent(), spatial);
                SpatialAddEdit addEdit = new SpatialAddEdit(newParent, spatial);

                //this will automatically remove the spatial from it current parent.
                newParent.attachChild(spatial);

                SceneEdit edit = new SceneEdit(remEdit, addEdit);
                getUndoManager().addEdit(edit);

            }
        });
    }

    public void moveLight(Light light, Spatial newParent, Spatial oldParent){
        gui.runOnRender(new Runnable() {
            @Override
            public void run() {

                LightRemoveEdit remEdit = new LightRemoveEdit(oldParent, light);
                LightAddEdit addEdit = new LightAddEdit(newParent, light);

                oldParent.removeLight(light);
                newParent.addLight(light);

                SceneEdit edit = new SceneEdit(remEdit, addEdit);
                getUndoManager().addEdit(edit);

            }
        });
    }

    public void moveControl(Control control, Spatial newParent, Spatial oldParent){
        gui.runOnRender(new Runnable() {
            @Override
            public void run() {

                oldParent.removeControl(control);
                newParent.addControl(control);

                ControlRemoveEdit remEdit = new ControlRemoveEdit(oldParent, control);
                ControlRemoveEdit addEdit = new ControlRemoveEdit(newParent, control);

                SceneEdit edit = new SceneEdit(remEdit, addEdit);
                getUndoManager().addEdit(edit);

            }
        });
    }
}
