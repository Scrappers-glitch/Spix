package spix.app.light;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.*;
import com.jme3.light.Light;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.simsilica.lemur.GuiGlobals;
import spix.props.PropertySet;

/**
 * Created by Nehon on 02/04/2016.
 */
public abstract class LightWrapper<L extends Light> {

    private Node widget;
    private L light;
    protected Material dashed;
    protected Node center;
    private PropertySet lightPropertySet;
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

    protected abstract void initWidget(Spatial target, Spatial widget, L light);
    protected abstract void setPositionRelativeToTarget(Spatial target, Vector3f prevTargetPos, PropertySet lightPropertySet);
    protected abstract void widgetUpdate(Spatial target, Spatial widget, PropertySet lightPropertySet, float tpf);
    protected abstract Spatial makeWidget();

    public void update (float tpf, Camera cam){
        if(lightPropertySet != null) {
            if(!prevTargetPos.equals(target.getWorldTranslation())){
                setPositionRelativeToTarget(target, prevTargetPos, lightPropertySet);
            }
            widgetUpdate(target, widget, lightPropertySet, tpf);
        } else {
            initWidget(target, widget, light);
        }

        prevTargetPos.set(target.getWorldTranslation());
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
        if (v != null && v.getType() == BoundingVolume.Type.AABB) {
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

    public PropertySet getLightPropertySet() {
        return lightPropertySet;
    }

    public void setLightPropertySet(PropertySet lightPropertySet) {
        this.lightPropertySet = lightPropertySet;
    }
}
