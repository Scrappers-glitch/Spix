package spix.undo.edit;

import com.jme3.light.Light;
import com.jme3.scene.Spatial;
import spix.app.light.LightWidgetState;
import spix.core.Spix;

/**
 * Created by Nehon on 23/12/2016.
 */
public class LightRemoveEdit extends LightAddEdit {

    public LightRemoveEdit(Spatial spatial, Light light, LightWidgetState lightWidgetState) {
        super(spatial, light, lightWidgetState);
    }

    @Override
    public void undo(Spix spix) {
        addLight();
    }

    @Override
    public void redo(Spix spix) {
        removeLight();
    }
}
