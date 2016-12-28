package spix.undo.edit;

import com.jme3.light.Light;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import spix.app.light.LightWidgetState;
import spix.core.Spix;
import spix.undo.SceneGraphStructureEdit;

/**
 * Created by Nehon on 23/12/2016.
 */
public class SpatialAddEdit implements SceneGraphStructureEdit {

    protected Node parent;
    protected Spatial child;
    protected boolean done = true;

    public SpatialAddEdit(Node parent, Spatial child) {
        this.parent = parent;
        this.child = child;
    }

    @Override
    public void undo(Spix spix) {
        removeSpatial();
        done = false;
    }

    @Override
    public void redo(Spix spix) {
        addSpatial();
        done = true;
    }

    protected void addSpatial() {
        parent.attachChild(child);
    }

    protected void removeSpatial() {
        parent.detachChild(child);
    }

    public Spatial getChild() {
        return child;
    }

    public boolean isDone(){
        return done;
    }
}
