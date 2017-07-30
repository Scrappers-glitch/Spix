package spix.undo.edit;

import com.jme3.light.Light;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import spix.core.Spix;

/**
 * Created by Nehon on 23/12/2016.
 */
public class ControlRemoveEdit extends ControlAddEdit {

    public ControlRemoveEdit(Spatial spatial, Control control) {
        super(spatial, control);
    }

    @Override
    public void undo(Spix spix) {
        addControl();
        done = false;
    }

    @Override
    public void redo(Spix spix) {
        removeControl();
        done = true;
    }
}
