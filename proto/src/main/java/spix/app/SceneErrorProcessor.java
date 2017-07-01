package spix.app;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;

import java.util.*;

/**
 * Created by Nehon on 01/07/2017.
 */
public class SceneErrorProcessor implements SceneProcessor {
    private ViewPort vp;
    private RenderManager renderManager;
    private SceneValidatorState validatorState;
    private Map<Geometry, Spatial.CullHint> cullHints = new HashMap<>();

    public SceneErrorProcessor(SceneValidatorState validatorState) {
        this.validatorState = validatorState;
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.vp = vp;
        this.renderManager = rm;
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {

    }

    @Override
    public boolean isInitialized() {
        return this.vp != null;
    }

    @Override
    public void preFrame(float tpf) {
        Set<Geometry> errors = validatorState.getErrors().keySet();
        cullHints.clear();
        if (errors.isEmpty()) {
            return;
        }
        for (Geometry errGeom : errors) {
            cullHints.put(errGeom, errGeom.getCullHint());
            errGeom.setCullHint(Spatial.CullHint.Always);
        }
    }

    @Override
    public void postQueue(RenderQueue rq) {
        Set<Geometry> errors = validatorState.getErrors().keySet();
        if (errors.isEmpty()) {
            return;
        }
        for (Geometry errGeom : errors) {
            renderManager.setForcedMaterial(validatorState.getFallBackMaterial());
            renderManager.renderGeometry(errGeom);
            renderManager.setForcedMaterial(null);
            errGeom.setCullHint(cullHints.get(errGeom));
        }
    }

    @Override
    public void postFrame(FrameBuffer out) {

    }

    @Override
    public void cleanup() {

    }

    @Override
    public void setProfiler(AppProfiler profiler) {

    }
}
