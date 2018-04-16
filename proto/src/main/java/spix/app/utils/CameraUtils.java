package spix.app.utils;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class CameraUtils {

    public static float getConstantScale(Camera cam, Vector3f worldTranslation, Vector3f tmpVec) {
        if (cam.isParallelProjection()) {
            float height = cam.getFrustumTop() - cam.getFrustumBottom();
            return height * 100f / cam.getHeight();
        }

        //Perspective mode

        // Need to figure out how much to scale the widget so that it stays
        // the same size on screen.  In our case, we want 1 unit to be
        // 100 pixels.
        Vector3f dir = cam.getDirection();
        float distance = dir.dot(tmpVec.set(worldTranslation).subtractLocal(cam.getLocation()));

        // m11 of the projection matrix defines the distance at which 1 pixel
        // is 1 unit.  Kind of.
        float m11 = cam.getProjectionMatrix().m11;
        // Magic scaling... trust the math... don't question the math... magic math...
        float halfHeight = cam.getHeight() * 0.5f;
        float scale = ((distance / halfHeight) * 100f) / m11;
        return scale;
    }

}
