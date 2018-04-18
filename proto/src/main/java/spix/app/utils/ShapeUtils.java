package spix.app.utils;

import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.scene.shape.*;
import com.jme3.scene.shape.Line;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.Axis;

import java.nio.*;

/**
 * Created by Nehon on 30/04/2016.
 */
public class ShapeUtils {


    public static final float NODE_ARMS_LENGTH = 1f;

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
        m.setBuffer(VertexBuffer.Type.Index, 1, idxBuf);

        makeCircle(radialSamples, radius, posBuf, texBuf, idxBuf, 0);

        m.updateBound();
        m.setStatic();

        Geometry geom = new Geometry(name, m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);

        return geom;
    }


    public static Geometry makeNodeHintShape(String name, ColorRGBA color, float handleSize) {


        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);

        ColorRGBA colorTip = color.clone();
        colorTip.a = 0;

        FloatBuffer posBuf = BufferUtils.createFloatBuffer(
                new Vector3f(-handleSize,0,0),new Vector3f(0,-handleSize,0),new Vector3f(0,0,-handleSize), Vector3f.UNIT_X.mult(-1), Vector3f.UNIT_Y.mult(-1), Vector3f.UNIT_Z.mult(-1)
        );

        short[] indices = {0,3,1,4,2,5};
        ShortBuffer idxBuf = BufferUtils.createShortBuffer(indices);

        FloatBuffer colBuf = BufferUtils.createFloatBuffer(color,color,color, colorTip,  colorTip,  colorTip);

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.Color,4, colBuf);
        m.setBuffer(VertexBuffer.Type.Index, 1, idxBuf);

        m.updateBound();
        m.setStatic();

        Geometry geom = new Geometry(name, m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);

        return geom;
    }

    public static Geometry makeNodeHintShape2(String name, ColorRGBA color, float handleSize) {


        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);

        ColorRGBA colorTip = color.clone();
        colorTip.a = 0;

        FloatBuffer posBuf = BufferUtils.createFloatBuffer(
                new Vector3f(-handleSize,0,0),new Vector3f(0,-handleSize,0),new Vector3f(0,0,-handleSize), new Vector3f(handleSize,0,0),new Vector3f(0,handleSize,0),new Vector3f(0,0,handleSize),
                Vector3f.UNIT_X.mult(-NODE_ARMS_LENGTH), Vector3f.UNIT_Y.mult(-NODE_ARMS_LENGTH), Vector3f.UNIT_Z.mult(-NODE_ARMS_LENGTH),Vector3f.UNIT_X.mult(NODE_ARMS_LENGTH), Vector3f.UNIT_Y.mult(NODE_ARMS_LENGTH), Vector3f.UNIT_Z.mult(NODE_ARMS_LENGTH)
        );

        short[] indices = {0,6,1,7,2,8,3,9,4,10,5,11};
        ShortBuffer idxBuf = BufferUtils.createShortBuffer(indices);

        FloatBuffer colBuf = BufferUtils.createFloatBuffer(color,color,color,color,color,color, colorTip,  colorTip,  colorTip, colorTip,  colorTip,  colorTip);

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.Color,4, colBuf);
        m.setBuffer(VertexBuffer.Type.Index, 1, idxBuf);

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

    public static Mesh makeWireBox(){
        Vector3f[] points = new Vector3f[8];

        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }

        points[0].set(-1, -1, 1);
        points[1].set(-1, 1, 1);
        points[2].set(1, 1, 1);
        points[3].set(1, -1, 1);

        points[4].set(-1, -1, -1);
        points[5].set(-1, 1, -1);
        points[6].set(1, 1, -1);
        points[7].set(1, -1, -1);

        return new WireFrustum(points);
    }
}
