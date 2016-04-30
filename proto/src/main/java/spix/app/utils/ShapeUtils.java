package spix.app.utils;

import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.Axis;

import java.nio.*;

/**
 * Created by Nehon on 30/04/2016.
 */
public class ShapeUtils {


    public static int makeCircle(int radialSamples, float radius, FloatBuffer posBuf, FloatBuffer texBuf, ShortBuffer idxBuf, int idx) {
        // generate geometry
        float fInvRS = 1.0f / radialSamples;

        // Generate points on the unit circle to be used in computing the mesh
        // points on a sphere slice.
        float[] afSin = new float[(radialSamples + 1)];
        float[] afCos = new float[(radialSamples + 1)];
        for (int iR = 0; iR < radialSamples; iR++) {
            float fAngle = FastMath.TWO_PI * fInvRS * iR;
            afCos[iR] = FastMath.cos(fAngle) * radius;
            afSin[iR] = FastMath.sin(fAngle) * radius;
        }
        afSin[radialSamples] = afSin[0];
        afCos[radialSamples] = afCos[0];

        for (int iR = 0; iR <= radialSamples; iR++) {
            posBuf.put(afCos[iR])
                    .put(afSin[iR])
                    .put(0);
            texBuf.put(iR % 2f)
                    .put(iR % 2f);

        }
        return writeIndex(radialSamples, idxBuf, idx);
    }

    public static Geometry makeCircleGeometry(String name, float radius, int radialSamples) {

        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);


        FloatBuffer posBuf = BufferUtils.createVector3Buffer((radialSamples + 1));
        FloatBuffer texBuf = BufferUtils.createVector2Buffer((radialSamples + 1));
        ShortBuffer idxBuf = BufferUtils.createShortBuffer((2 * radialSamples));

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        m.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        makeCircle(radialSamples, radius, posBuf, texBuf, idxBuf, 0);

        m.updateBound();
        m.setStatic();

        Geometry geom = new Geometry(name, m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);

        return geom;
    }


    public static int makeSegmentedLine(int nbSegments, Axis xAxis, float size, float start, Vector3f offset, FloatBuffer posBuf, FloatBuffer texBuf, ShortBuffer idxBuf, int idx) {

        for (int i = 0; i <= nbSegments; i++) {
            float value = start + (size / nbSegments) * i;
            float x = xAxis == Axis.X?value:offset.x;
            float y = xAxis == Axis.Y?value:offset.y;
            float z = xAxis == Axis.Z?value:offset.z;
            posBuf.put(x).put(y).put(z);
            texBuf.put(i % 2f).put(i % 2f);
        }
        return writeIndex(nbSegments, idxBuf, idx);
    }


    private static int writeIndex(int radialSamples, ShortBuffer idxBuf, int idx) {
        int segDone = 0;
        while (segDone < radialSamples) {
            idxBuf.put((short) idx);
            idxBuf.put((short) (idx + 1));
            idx++;
            segDone++;
        }
        idx++;
        return idx;
    }
}
