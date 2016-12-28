package spix.undo.edit;

import com.jme3.light.Light;
import com.jme3.scene.Spatial;
import spix.app.light.LightWidgetState;
import spix.core.Spix;
import spix.undo.SceneGraphStructureEdit;

/**
 * Created by Nehon on 23/12/2016.
 */
public class LightAddEdit implements SceneGraphStructureEdit {

    protected Spatial spatial;
    protected Light light;
    protected LightWidgetState lightWidgetState;

    public LightAddEdit(Spatial spatial, Light light, LightWidgetState lightWidgetState) {
        this.spatial = spatial;
        this.light = light;
        this.lightWidgetState = lightWidgetState;
    }

    @Override
    public void undo(Spix spix) {
        removeLight();
    }

    @Override
    public void redo(Spix spix) {
        addLight();
    }

    protected void addLight() {
        spatial.addLight(light);
        lightWidgetState.addLight(spatial, light);
    }

    protected void removeLight() {
        spatial.removeLight(light);
        lightWidgetState.removeLight(light);
    }

}
