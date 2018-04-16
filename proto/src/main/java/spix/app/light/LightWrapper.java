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
import com.simsilica.lemur.RangedValueModel;
import spix.app.utils.CameraUtils;
import spix.props.PropertySet;

/**
 * Created by Nehon on 02/04/2016.
 */
public abstract class LightWrapper<L extends Light> {

    private Node widget;
    private L light;
    protected Material dashed;
    protected Material dashedBox;
    protected Node center;
    private PropertySet lightPropertySet;
    private Material dot;
    private Spatial parent;
    private Vector3f prevTargetPos = Vector3f.NAN.clone();
    private boolean initialized = false;
    private boolean selected = false;
    private ColorRGBA widgetColor = ColorRGBA.Black.clone();
    private ColorRGBA color = ColorRGBA.White.clone();
    private float intensity = 1.0f;
    private Vector3f tmpVec3 = new Vector3f();


    public LightWrapper(Node node, L light, Spatial parent, AssetManager assetManager) {
        this.widget = node;
        this.light = light;
        this.parent = parent;
        initMaterials(assetManager);
        center = makeCenter();
        widget.attachChild(makeWidget());

        color.set(light.getColor());
        float max = Math.max(Math.max(color.r, color.g), color.b);
        if (max > 1f) {
            intensity = max;
            color.multLocal(1f / intensity);
        } else {
            intensity = 1f;
        }
    }

    protected abstract void initWidget(Spatial target, Spatial widget, L light);

    protected abstract void setPositionRelativeToTarget(Spatial target, Vector3f prevTargetPos, PropertySet lightPropertySet);

    protected abstract void widgetUpdate(Spatial target, Spatial widget, PropertySet lightPropertySet, float tpf);

    protected abstract Spatial makeWidget();

    public void update(float tpf, Camera cam) {
        if (lightPropertySet != null) {
            if (!prevTargetPos.equals(parent.getWorldTranslation())) {
                setPositionRelativeToTarget(parent, prevTargetPos, lightPropertySet);
            }
            widgetUpdate(parent, widget, lightPropertySet, tpf);
        } else if (!initialized) {
            initWidget(parent, widget, light);
            initialized = true;
        }

        prevTargetPos.set(parent.getWorldTranslation());
        updateCenter(cam);
        updateColor();
    }

    public void updateColor() {
        if (isSelected()) {
            widgetColor.set(ColorRGBA.Orange);
            if(!light.isEnabled()){
                widgetColor.multLocal(0.3f);
            }
        } else {
            widgetColor.set(ColorRGBA.Black);
            if(!light.isEnabled()){
                widgetColor.setAsSrgb(0.3f,0.3f,0.3f,1.0f);
            }
        }
    }

    private void updateCenter(Camera cam) {

        float scale = CameraUtils.getConstantScale(cam, widget.getWorldTranslation(), tmpVec3);

        float scalex = scale;
        float scaley = scale;
        float scalez = scale;
        if (center.getParent() != null) {
            //ignoring parent scale
            scalex /= center.getParent().getWorldScale().x;
            scaley /= center.getParent().getWorldScale().y;
            scalez /= center.getParent().getWorldScale().z;
        }
        center.setLocalScale(scalex, scaley, scalez);

    }

    public L getLight() {
        return light;
    }

    public Node getWidget() {
        return widget;
    }

    public Spatial getParent() {
        return parent;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    protected float getGlobalScale() {
        BoundingVolume v = getBoundingVolume();
        if (v != null && v.getType() == BoundingVolume.Type.AABB) {
            BoundingBox bb = (BoundingBox) v;
            Vector3f vec = new Vector3f(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
            return vec.length();
        }
        if (v != null && v.getType() == BoundingVolume.Type.Sphere) {
            BoundingSphere bs = (BoundingSphere) v;
            return bs.getRadius();
        }
        return 0;
    }

    private BoundingVolume getBoundingVolume() {
        BoundingVolume v = parent.getWorldBound();
        if (Float.isInfinite(v.getVolume())){
            BoundingVolume v2 = new BoundingBox(v.getCenter(), 1,1,1);
            for (Spatial spatial : ((Node) parent).getChildren()) {
                if(Float.isFinite(spatial.getWorldBound().getVolume())){
                    v2.merge(spatial.getWorldBound());
                }
            }
            return v2;
        }
        return v;
    }

    protected void initMaterials(AssetManager assetManager) {
        GuiGlobals globals = GuiGlobals.getInstance();

        Texture texture = globals.loadTexture("Interface/small-circle.png", false, false);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        dot = globals.createMaterial(texture, false).getMaterial();
        dot.setColor("Color", widgetColor);
        dot.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        dashed = new Material(assetManager, "MatDefs/dashed/dashed.j3md");
        dashed.getAdditionalRenderState().setWireframe(true);
        dashed.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        dashed.getAdditionalRenderState().setDepthWrite(false);
        dashed.setFloat("DashSize", 0.5f);
        dashed.setColor("Color", widgetColor);

        dashedBox = new Material(assetManager, "MatDefs/dashed2.j3md");
        dashedBox.setColor("Color", widgetColor);
    }

    private Node makeCenter() {

        Node center = new Node("Light center");
        // Now the teeny tiny center that never disappears
        Mesh mesh = new Quad(0.08f, 0.08f);
        final Geometry g = new Geometry("centerOrigin", mesh);
        g.setMaterial(dot);
        g.setLocalTranslation(-0.04f, -0.04f, 0.0f);
        center.attachChild(g);

        //This is a hidden quad that sole purpose is to increase the area where the user can click and select the light
        final Geometry g2 = new Geometry("hitQuad", new Quad(0.4f, 0.4f));
        g2.setMaterial(dot);
        g2.setCullHint(Spatial.CullHint.Always);
        g2.setLocalTranslation(-0.2f, -0.2f, -0.2f);
        center.attachChild(g2);

        center.addControl(new BillboardControl());
        return center;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = Math.max(intensity, 1f);
        updateLightColor(color);
    }

    public ColorRGBA getColor() {
        return color;
    }

    public void setColor(ColorRGBA color) {
        this.color = color;
        updateLightColor(color);
    }

    public void updateLightColor(ColorRGBA color) {
        light.getColor().set(color);
        light.getColor().multLocal(intensity);
    }

    public PropertySet getLightPropertySet() {
        return lightPropertySet;
    }

    public void setLightPropertySet(PropertySet lightPropertySet) {
        this.lightPropertySet = lightPropertySet;
    }
}
