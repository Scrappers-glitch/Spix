package spix.app.light;

import com.jme3.light.Light;
import com.jme3.scene.Node;

/**
 * Created by Nehon on 02/04/2016.
 */
public class LightWrapper {

    private Node node;
    private Light light;

    public LightWrapper(Node node, Light light) {
        this.node = node;
        this.light = light;
    }

    public Light getLight() {
        return light;
    }

    public Node getNode() {
        return node;
    }
}
