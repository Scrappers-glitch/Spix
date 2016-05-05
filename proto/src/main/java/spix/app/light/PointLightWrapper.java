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
import com.jme3.light.*;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.control.BillboardControl;
import com.jme3.util.BufferUtils;
import spix.app.utils.ShapeUtils;
import spix.props.*;

import java.nio.*;


/**
 *
 * @author Rémy Bouquet
 */
public class PointLightWrapper extends LightWrapper<PointLight> {

    //widget
    private Vector3f prevSpatialPos = new Vector3f();
    private Vector3f prevSpatialScale = new Vector3f();

    //Light
    private float prevLightRadius = 0;
    private Vector3f prevLightPos = new Vector3f();

    /**
     * @param light The light to be synced.
     */
    public PointLightWrapper(Node widget, PointLight light, Spatial target, AssetManager assetManager) {
        super(widget, light, target, assetManager);
    }

    @Override
    protected void initWidget(Spatial target, Spatial widget, PointLight light) {
        widget.setLocalTranslation(light.getPosition());
        widget.setLocalScale(light.getRadius());
    }

    @Override
    protected void setPositionRelativeToTarget(Spatial target, Vector3f prevTargetPos, PropertySet lightPropertySet) {
        //nothing to do.
    }

    @Override
    protected void widgetUpdate(Spatial target, Spatial widget, PropertySet set, float tpf) {

        Property position = set.getProperty("worldTranslation");
        Property radius = set.getProperty("radius");

        //Widget to light
        //widget has been moved update light position
        if(!prevSpatialPos.equals(widget.getWorldTranslation())) {
            position.setValue(widget.getWorldTranslation());
            syncPosition(widget, position);
        }
        //widget has been scaled update light radius.
        if(!prevSpatialScale.equals(widget.getWorldScale())) {
            radius.setValue(widget.getWorldScale().length() / Vector3f.UNIT_XYZ.length());
            syncScale(widget, radius);
        }

        //Light to widget
        //Light has been moved update widget position
        if(!prevLightPos.equals(position.getValue())) {
            widget.setLocalTranslation((Vector3f)position.getValue());
            syncPosition(widget, position);
        }
        //light radius has been changed, update widget scale.
        float r =  (float)radius.getValue();
        if(prevLightRadius != r){
            widget.setLocalScale(r);
            syncScale(widget, radius);
        }

    }

    private void syncScale(Spatial widget, Property radius) {
        prevSpatialScale.set(widget.getWorldScale());
        prevLightRadius = (float)radius.getValue();
    }

    private void syncPosition(Spatial widget, Property position) {
        prevSpatialPos.set(widget.getWorldTranslation());
        prevLightPos.set((Vector3f)position.getValue());
    }

    @Override
    protected Spatial makeWidget() {

        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);
        int radialSamples = 16;

        FloatBuffer posBuf = BufferUtils.createVector3Buffer(radialSamples + 1);
        FloatBuffer texBuf = BufferUtils.createVector2Buffer(radialSamples + 1);
        ShortBuffer idxBuf = BufferUtils.createShortBuffer(2 * radialSamples);

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        m.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        ShapeUtils.makeCircle(radialSamples, 0.14f, posBuf, texBuf, idxBuf, 0);

        m.updateBound();
        m.setStatic();

        Geometry geom = new Geometry("LightDebug", m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setMaterial(dashed);

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

        Geometry geom2 = new Geometry("LightRadius", line);
        geom2.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom2.setMaterial(dashed);


        Node widget = new Node();
        center.attachChild(geom);
        widget.attachChild(center);
        widget.attachChild(geom2);
        geom2.addControl(new BillboardControl());
        return widget;
    }

}