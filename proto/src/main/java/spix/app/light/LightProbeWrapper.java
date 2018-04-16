/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package spix.app.light;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingSphere;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.BufferUtils;
import spix.app.utils.ShapeUtils;
import spix.props.Property;
import spix.props.PropertySet;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


/**
 * @author Rémy Bouquet
 */
public class LightProbeWrapper extends LightWrapper<LightProbe> {

    //widget
    private Vector3f prevSpatialPos = new Vector3f();
    private Vector3f prevSpatialScale = new Vector3f();
    private Quaternion prevSpatialRot = new Quaternion();

    //Light
    private float prevLightRadius = 0;
    private Vector3f prevLightExtent = new Vector3f();
    private Vector3f prevLightPos = new Vector3f();
    private Quaternion prevLightRot = new Quaternion();

    private Material debugMaterial;

    private LightProbe.AreaType areaType;

    /**
     * @param light The light to be synced.
     */
    public LightProbeWrapper(Node widget, LightProbe light, Spatial target, AssetManager assetManager) {
        super(widget, light, target, assetManager);
        this.areaType = light.getAreaType();
    }

    @Override
    protected void initWidget(Spatial target, Spatial widget, LightProbe light) {
        widget.setLocalTranslation(light.getPosition());
        if (light.getAreaType() == LightProbe.AreaType.Spherical) {
            SphereProbeArea area = (SphereProbeArea) light.getArea();
            widget.setLocalScale(area.getRadius());
        } else {
            OrientedBoxProbeArea area = (OrientedBoxProbeArea) light.getArea();
            widget.setLocalScale(area.getExtent());
            widget.setLocalRotation(area.getRotation());
        }
    }

    @Override
    protected void setPositionRelativeToTarget(Spatial target, Vector3f prevTargetPos, PropertySet lightPropertySet) {
        //nothing to do.
    }

    @Override
    protected void widgetUpdate(Spatial target, Spatial widget, PropertySet set, float tpf) {

        Property position = set.getProperty("worldTranslation");

        //Widget to light
        //widget has been moved update light position
        if (!prevSpatialPos.equals(widget.getWorldTranslation())) {
            position.setValue(widget.getWorldTranslation());
            syncPosition(widget, position);
        }
        //Light to widget
        //Light has been moved update widget position
        if (!prevLightPos.equals(position.getValue())) {
            widget.setLocalTranslation((Vector3f) position.getValue());
            syncPosition(widget, position);
        }
        if(getLight().getAreaType() == LightProbe.AreaType.Spherical) {

            //widget has been scaled update light radius.
            Property radius = set.getProperty("radius");
            if (!prevSpatialScale.equals(widget.getWorldScale()) && radius != null) {
                radius.setValue(widget.getWorldScale().length() / Vector3f.UNIT_XYZ.length());
                syncScale(widget, radius);
            }
            //light radius has been changed, update widget scale.
            float r = (float) radius.getValue();
            if (prevLightRadius != r) {
                widget.setLocalScale(r);
                syncScale(widget, radius);
            }
        } else {
            //widget has been scaled update light radius.
            Property extent = set.getProperty("extent");
            if (!prevSpatialScale.equals(widget.getWorldScale()) && extent != null) {
                extent.setValue(widget.getWorldScale());
                syncExtent(widget, extent);
            }
            //light extent has been changed, update widget scale.
            if (extent!= null && !prevLightExtent.equals(extent.getValue())) {
                widget.setLocalScale((Vector3f)extent.getValue());
                syncExtent(widget, extent);
            }


            //widget has been rotated update light direction
            if(!prevSpatialRot.equals(widget.getWorldRotation())) {
                Property rotation = set.getProperty("worldRotation");
                rotation.setValue(widget.getWorldRotation());
                syncRotation(widget, rotation);
            }
        }
    }

    public void setAreaType(LightProbe.AreaType areaType) {
        this.areaType = areaType;
        ProbeArea area = getLight().getArea();

        getLight().setAreaType(areaType);
        ProbeArea newArea = getLight().getArea();
        float ext;
        if(areaType == LightProbe.AreaType.Spherical){
            ext = area.getRadius() / FastMath.cos(FastMath.PI * 0.25f);
            ((SphereProbeArea)newArea).setRadius(ext);
        } else {
            ext = area.getRadius() * FastMath.cos(FastMath.PI * 0.25f);
            ((OrientedBoxProbeArea)newArea).setExtent(new Vector3f(ext, ext ,ext));
        }
        getLight().setPosition(getLight().getPosition());

        getWidget().detachAllChildren();
        getWidget().attachChild(makeWidget());
        getWidget().setLocalScale(ext);
    }

    public LightProbe.AreaType getAreaType() {
        return areaType;
    }

    public void updateMap() {
        LightProbe probe = getLight();
        if (probe.isReady()) {
            debugMaterial.setTexture("CubeMap", probe.getPrefilteredEnvMap());
        }
    }

    private void syncRotation(Spatial widget, Property rotation) {
        prevSpatialRot.set(widget.getWorldRotation());
        prevLightRot.set((Quaternion) rotation.getValue());
    }

    private void syncScale(Spatial widget, Property radius) {
        prevSpatialScale.set(widget.getWorldScale());
        prevLightRadius = (float) radius.getValue();
    }

    private void syncExtent(Spatial widget, Property extent) {
        prevSpatialScale.set(widget.getWorldScale());
        prevLightExtent.set((Vector3f) extent.getValue());
    }

    private void syncPosition(Spatial widget, Property position) {
        prevSpatialPos.set(widget.getWorldTranslation());
        prevLightPos.set((Vector3f) position.getValue());
    }

    @Override
    protected void initMaterials(AssetManager assetManager) {
        super.initMaterials(assetManager);
        debugMaterial = new Material(assetManager, "Common/MatDefs/Misc/reflect.j3md");
    }

    @Override
    protected Spatial makeWidget() {

        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);
        int radialSamples = 16;
        FloatBuffer posBuf;
        FloatBuffer texBuf;
        ShortBuffer idxBuf;

        posBuf = BufferUtils.createVector3Buffer(radialSamples + 1);
        texBuf = BufferUtils.createVector2Buffer(radialSamples + 1);
        idxBuf = BufferUtils.createShortBuffer(2 * radialSamples);

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        m.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        ShapeUtils.makeCircle(radialSamples, 0.14f, posBuf, texBuf, idxBuf, 0);

        m.updateBound();
        m.setStatic();
        Geometry geom = new Geometry("LightDebug", m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setMaterial(dashed);


        Geometry geom2;

        if (getLight().getAreaType() == LightProbe.AreaType.Spherical) {
            Mesh line = new Mesh();
            line.setMode(Mesh.Mode.Lines);
            radialSamples = 128;

            posBuf = BufferUtils.createVector3Buffer(radialSamples + 1);
            texBuf = BufferUtils.createVector2Buffer(radialSamples + 1);
            idxBuf = BufferUtils.createShortBuffer(2 * radialSamples);

            line.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
            line.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
            line.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

            ShapeUtils.makeCircle(radialSamples, 1f, posBuf, texBuf, idxBuf, 0);

            line.updateBound();
            line.setStatic();
            geom2 = new Geometry("LightRadius", line);
            geom2.setQueueBucket(RenderQueue.Bucket.Transparent);
            geom2.setMaterial(dashed);
            geom2.addControl(new BillboardControl());

        } else {
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

            Mesh box = new WireFrustum(points);
            geom2 = new Geometry("LightArea", box);
            geom2.setMaterial(dashedBox);
        }

        Sphere s = new Sphere(16, 16, 0.15f);
        Geometry debugGeom = new Geometry("debugEnvProbe", s);
        debugGeom.setMaterial(debugMaterial);

        Node widget = new Node();
        center.attachChild(geom);
        center.attachChild(debugGeom);
        widget.attachChild(center);
        widget.attachChild(geom2);

        updateMap();

        return widget;
    }

}