package spix.undo.edit;

import com.jme3.light.Light;
import com.jme3.scene.Spatial;
import spix.core.Spix;

/**
 * Created by Nehon on 23/12/2016.
 */
public class LightRemoveEdit extends LightAddEdit {

    public LightRemoveEdit(Spatial spatial, Light light) {
        super(spatial, light);
    }

    @Override
    public void undo(Spix spix) {
        addLight();
        done = false;
    }

    @Override
    public void redo(Spix spix) {
        removeLight();
        done = true;
    }
}
