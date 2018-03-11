package spix.app;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.ViewPort;
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

    private Node renderNode;
    private DirectionalLight dirLight = new DirectionalLight(new Vector3f(-0.2f, -1, -0.3f).normalizeLocal());
    private AmbientLight ambLight = new AmbientLight(new ColorRGBA(0.7f, 0.7f, 0.7f, 1.0f));
    private LightProbe probe;

    private ViewPort viewPort;
    @Override
    protected void initialize(Application app) {
        this.renderManager = app.getRenderManager();
        app.getViewPort().addProcessor(new SceneErrorProcessor(this));
        fallBackMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        fallBackMaterial.setColor("Color", ColorRGBA.Red);
        renderNode = new Node("validatorNode");
        Spatial s = app.getAssetManager().loadModel("Models/probe.j3o");
        probe = (LightProbe) s.getLocalLightList().get(0);
        probe.setPosition(Vector3f.ZERO);
        renderNode.addLight(dirLight);
        renderNode.addLight(ambLight);
        renderNode.addLight(probe);
        viewPort = app.getViewPort();
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
            Node parent = geom.getParent();
            try {
                renderNode.attachChild(geom);
                renderNode.updateGeometricState();
                renderManager.preloadScene(renderNode);
                renderManager.renderScene(renderNode, viewPort);
                renderManager.renderViewPortQueues(viewPort, true);
            } catch (Exception e) {
                errors.put(geom, e.getMessage());
            } finally {
                viewPort.getQueue().clear();
                geom.removeFromParent();
                if(parent != null) {
                    parent.attachChild(geom);
                }
            }
        }
    }
}
