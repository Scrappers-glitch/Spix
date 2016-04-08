package spix.app.light;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.*;
import com.jme3.light.Light;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.control.*;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.*;

import java.nio.*;

/**
 * Created by Nehon on 02/04/2016.
 */
public abstract class LightWrapper<L extends Light> {

    private Node widget;
    private L light;
    protected Material dashed;
    protected Node center;
    private Material dot;
    private Spatial target;
    private Vector3f prevTargetPos = Vector3f.NAN.clone();

    public LightWrapper(Node node, L light, Spatial target, AssetManager assetManager) {
        this.widget = node;
        this.light = light;
        this.target = target;
        initMaterials(assetManager);
        center =  makeCenter();
        widget.attachChild(makeWidget());
    }

    protected abstract void setPositionRelativeToTarget(Spatial target, Spatial widget, L light);
    protected abstract void widgetUpdate(Spatial target, Spatial widget, L light, float tpf);
    protected abstract Spatial makeWidget();

    public void update (float tpf, Camera cam){

        if(!prevTargetPos.equals(target.getWorldTranslation())){
            setPositionRelativeToTarget(target, widget, light);
            prevTargetPos.set(target.getWorldTranslation());
        }
        widgetUpdate(target, widget, light, tpf);

        updateCenter(cam);

    }

    private void updateCenter(Camera cam) {
        Vector3f dir = cam.getDirection();
        float distance = dir.dot(center.getWorldTranslation().subtract(cam.getLocation()));

        // m11 of the projection matrix defines the distance at which 1 pixel
        // is 1 unit.  Kind of.
        float m11 = cam.getProjectionMatrix().m11;
        // Magic scaling... trust the math... don't question the math... magic math...
        float halfHeight = cam.getHeight() * 0.5f;
        float scale = ((distance/halfHeight) * 100)/m11;
        if(center.getParent() != null){
            //ignoring parent scale
            scale /= (center.getParent().getWorldScale().length() / Vector3f.UNIT_XYZ.length());
        }
        center.setLocalScale(scale);
    }

    public L getLight() {
        return light;
    }

    public Node getWidget() {
        return widget;
    }

    protected float getGlobalScale(){
        BoundingVolume v = target.getWorldBound();
        if(v.getType() == BoundingVolume.Type.AABB){
            BoundingBox bb = (BoundingBox)v;
            Vector3f vec = new Vector3f(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
            return vec.length();
        }
        if(v.getType() == BoundingVolume.Type.Sphere){
            BoundingSphere bs = (BoundingSphere)v;
            return bs.getRadius();
        }
        return 0;
    }

    private void initMaterials(AssetManager assetManager) {
        GuiGlobals globals = GuiGlobals.getInstance();

        Texture texture = globals.loadTexture("Interface/small-circle.png", false, false);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        dot = globals.createMaterial(texture, false).getMaterial();
        dot.setColor("Color", ColorRGBA.Black);
        dot.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        dashed = new Material(assetManager, "MatDefs/dashed/dashed.j3md");
        dashed.getAdditionalRenderState().setWireframe(true);
        dashed.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        dashed.getAdditionalRenderState().setDepthWrite(false);
        dashed.setFloat("DashSize", 0.5f);
        dashed.setColor("Color", ColorRGBA.Black);
    }

    protected int makeCircle(int radialSamples, float radius, FloatBuffer posBuf, FloatBuffer texBuf, ShortBuffer idxBuf, int idx) {
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

    protected Geometry makeCircleGeometry(String name,float radius, int radialSamples) {

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
        geom.setMaterial(dashed);
        return geom;
    }

    protected int makeSegmentedLine(int nbSegments, Axis xAxis, float size, float start, Vector3f offset, FloatBuffer posBuf, FloatBuffer texBuf, ShortBuffer idxBuf, int idx) {

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

    private int writeIndex(int radialSamples, ShortBuffer idxBuf, int idx) {
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

    private Node makeCenter(){

        Node center = new Node("Light center");
        // Now the teeny tiny center that never disappears
        Mesh mesh = new Quad(0.08f, 0.08f);
        final Geometry g = new Geometry("centerOrigin", mesh);
        g.setMaterial(dot);
        g.setLocalTranslation(-0.04f,-0.04f,0.0f);
        center.attachChild(g);

        //This is a hidden quad that sole purpose is to increase the area where the user can click and select the light
        final Geometry g2 = new Geometry("hitQuad", new Quad(0.4f,0.4f));
        g2.setMaterial(dot);
        g2.setCullHint(Spatial.CullHint.Always);
        g2.setLocalTranslation(-0.2f,-0.2f,-0.2f);
        center.attachChild(g2);

        center.addControl(new BillboardControl());
        return center;
    }

}
