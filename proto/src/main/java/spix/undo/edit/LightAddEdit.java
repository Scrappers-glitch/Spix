package spix.undo.edit;

import com.jme3.light.Light;
import com.jme3.scene.Spatial;
import spix.core.Spix;
import spix.undo.SceneGraphStructureEdit;

/**
 * Created by Nehon on 23/12/2016.
 */
public class LightAddEdit implements SceneGraphStructureEdit {

    protected Spatial spatial;
    protected Light light;
    protected boolean done = true;

    public LightAddEdit(Spatial spatial, Light light) {
        this.spatial = spatial;
        this.light = light;
        //this.lightWidgetState = lightWidgetState;
    }

    @Override
    public void undo(Spix spix) {
        removeLight();
        done = false;
    }

    @Override
    public void redo(Spix spix) {
        addLight();
        done = true;
    }

    protected void addLight() {
        spatial.addLight(light);
    }

    protected void removeLight() {
        spatial.removeLight(light);
    }

    public Spatial getSpatial() {
        return spatial;
    }

    public Light getLight() {
        return light;
    }

    public boolean isDone() {
        return done;
    }
}
