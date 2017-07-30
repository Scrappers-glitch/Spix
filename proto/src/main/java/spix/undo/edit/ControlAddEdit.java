package spix.undo.edit;

import com.jme3.light.Light;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import spix.core.Spix;
import spix.undo.SceneGraphStructureEdit;

/**
 * Created by Nehon on 23/12/2016.
 */
public class ControlAddEdit implements SceneGraphStructureEdit {

    protected Spatial spatial;
    protected Control control;
    protected boolean done = true;

    public ControlAddEdit(Spatial spatial, Control control) {
        this.spatial = spatial;
        this.control = control;
    }

    @Override
    public void undo(Spix spix) {
        removeControl();
        done = false;
    }

    @Override
    public void redo(Spix spix) {
        addControl();
        done = true;
    }

    protected void addControl() {
        spatial.addControl(control);
    }

    protected void removeControl() {
        spatial.removeControl(control);
    }

    public Spatial getSpatial() {
        return spatial;
    }

    public Control getControl() {
        return control;
    }

    public boolean isDone() {
        return done;
    }
}
