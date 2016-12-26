package spix.app.light;

import com.jme3.app.*;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.Node;

/**
 * Created by Nehon on 26/12/2016.
 */
public class DebugLightsState extends BaseAppState {

    private Node rootNode;
    private DirectionalLight dirLight = new DirectionalLight(new Vector3f(-0.2f, -1, -0.3f).normalizeLocal());
    private AmbientLight ambLight = new AmbientLight(new ColorRGBA(0.7f, 0.7f, 0.7f, 1.0f));

    @Override
    protected void initialize(Application app) {
        rootNode = ((SimpleApplication) app).getRootNode();
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        rootNode.addLight(dirLight);
        rootNode.addLight(ambLight);
    }

    @Override
    protected void onDisable() {
        rootNode.removeLight(dirLight);
        rootNode.removeLight(ambLight);
    }
}
