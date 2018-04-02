package spix.undo.edit;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import spix.core.Spix;

/**
 * Created by Nehon on 23/12/2016.
 */
public class SpatialRemoveEdit extends SpatialAddEdit {

    public SpatialRemoveEdit(Node parent, Spatial child) {
        super(parent, child);
    }

    @Override
    public void undo(Spix spix) {
        addSpatial();
        done = false;
    }

    @Override
    public void redo(Spix spix) {
        removeSpatial();
        done = true;
    }
}
