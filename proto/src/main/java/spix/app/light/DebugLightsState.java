package spix.app.light;

import com.jme3.app.*;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import spix.app.SpixState;
import spix.core.Spix;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import java.awt.*;

import static spix.app.DefaultConstants.VIEW_DEBUG_LIGHTS;

/**
 * Created by Nehon on 26/12/2016.
 */
public class DebugLightsState extends BaseAppState {

    private Node rootNode;
    private DirectionalLight dirLight = new DirectionalLight(new Vector3f(-0.2f, -1, -0.3f).normalizeLocal());
    private AmbientLight ambLight = new AmbientLight(new ColorRGBA(0.7f, 0.7f, 0.7f, 1.0f));
    public final static String PROBE_INDEX = "dubug.lights.probeindex";
    private LightProbe probe;

    public static String[] probes = {
            "studio",
            "dresden_station_night",
            "City_Night_Lights",
            "corsica_beach",
            "bathroom",
            "glass_passage",
            "flower_road",
            "Parking_Lot",
            "River_Road",
            "road_in_tenerife_mountain",
            "Sky_Cloudy",
            "Stonewall",
    };

    @Override
    protected void initialize(Application app) {
        rootNode = ((SimpleApplication) app).getRootNode();
        Spix spix = getState(SpixState.class).getSpix();
        spix.getBlackboard().bind(VIEW_DEBUG_LIGHTS, this, "enabled");
        spix.getBlackboard().bind(PROBE_INDEX, this, "probeIndex");
        spix.getBlackboard().set(VIEW_DEBUG_LIGHTS, this.isEnabled());
        //loading probe
        Spatial s = app.getAssetManager().loadModel("Probes/" + probes[0] + ".j3o");
        probe = (LightProbe) s.getLocalLightList().get(0);
        probe.setPosition(Vector3f.ZERO);
        s.removeLight(probe);
    }


    public void setProbeIndex(int index){
        rootNode.removeLight(probe);
        Spatial s = getApplication().getAssetManager().loadModel("Probes/" + probes[index] + ".j3o");
        probe = (LightProbe) s.getLocalLightList().get(0);
        probe.setPosition(Vector3f.ZERO);
        s.removeLight(probe);
        rootNode.addLight(probe);
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        rootNode.addLight(dirLight);
        rootNode.addLight(ambLight);
        rootNode.addLight(probe);
    }

    @Override
    protected void onDisable() {
        rootNode.removeLight(dirLight);
        rootNode.removeLight(ambLight);
        rootNode.removeLight(probe);
    }
}
