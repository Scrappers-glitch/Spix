package spix.app.light;

import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.light.LightProbe;
import com.jme3.scene.Spatial;
import spix.app.DefaultConstants;
import spix.core.SelectionModel;
import spix.core.Spix;
import spix.ui.MessageRequester;

public class LightProbeService {

    private Spix spix;
    private EnvironmentCamera envCam;

    public LightProbeService(Spix spix, EnvironmentCamera envCam) {
        this.spix = spix;
        this.envCam = envCam;
    }

    public void render(EnvMapUtils.GenerationType type) {

        Object selection = spix.getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class).getSingleSelection();
        if (!(selection instanceof LightProbeWrapper)) {
            return;
        }
        Spatial scene = spix.getBlackboard().get(DefaultConstants.SCENE_ROOT, Spatial.class);
        LightProbeWrapper wrapper = (LightProbeWrapper) selection;
        LightProbe probe =wrapper.getLight();

        String id = spix.getService(MessageRequester.class).displayLoading("Rendering probe...");
        LightProbeFactory.updateProbe(probe, envCam, scene, type, new JobProgressAdapter<LightProbe>() {
            @Override
            public void done(LightProbe result) {
                spix.getService(MessageRequester.class).hideLoading(id);
                wrapper.updateMap();
            }
        });

    }
}
