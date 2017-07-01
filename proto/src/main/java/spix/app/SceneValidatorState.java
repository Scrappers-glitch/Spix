package spix.app;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.RendererException;
import com.jme3.scene.*;
import spix.ui.MessageRequester;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nehon on 01/07/2017.
 */
public class SceneValidatorState extends BaseAppState {

    private RenderManager renderManager;
    private SceneValidator validator = new SceneValidator();
    private Map<Geometry, String> errors = new HashMap<>();
    private Material fallBackMaterial;

    @Override
    protected void initialize(Application app) {
        this.renderManager = app.getRenderManager();
        app.getViewPort().addProcessor(new SceneErrorProcessor(this));
        fallBackMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        fallBackMaterial.setColor("Color", ColorRGBA.Red);
    }

    public boolean validate(Spatial scene) {
        errors.clear();
        scene.depthFirstTraversal(validator);

        if (!errors.isEmpty()) {
            String error = "";
            for (Geometry geometry : errors.keySet()) {
                error += "Error on geometry " + geometry.getName() + ": " + errors.get(geometry) + "\n";
            }
            getState(SpixState.class).getSpix().getService(MessageRequester.class).showMessage("Errors in scene", error, MessageRequester.Type.Error);
            return false;
        }

        return true;
    }

    public Material getFallBackMaterial() {
        return fallBackMaterial;
    }

    public Map<Geometry, String> getErrors() {
        return errors;
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }

    class SceneValidator extends SceneGraphVisitorAdapter {

        @Override
        public void visit(Geometry geom) {
            try {
                renderManager.preloadScene(geom);
            } catch (Exception e) {
                errors.put(geom, e.getMessage());
            }
        }
    }
}
