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
import com.jme3.light.DirectionalLight;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.Axis;
import spix.app.utils.ShapeUtils;
import spix.props.*;

import java.nio.*;


/**
 *
 * @author Rémy Bouquet
 */
public class DirectionalLightWrapper extends LightWrapper<DirectionalLight> {

    //Widget
    private Vector3f prevSpatialPos = new Vector3f();

    //Light
    private Vector3f prevLightDir = new Vector3f();

    public DirectionalLightWrapper(Node widget, DirectionalLight light, Spatial target, AssetManager assetManager) {
        super(widget, light, target, assetManager);
    }

    @Override
    protected void setPositionRelativeToTarget(Spatial target, Spatial widget, DirectionalLight light) {
        updateWidgetFromDirection(target, widget, light.getDirection());
    }

    private void updateWidgetFromDirection(Spatial target, Spatial widget, Vector3f direction) {
        Vector3f pos = direction.clone();
        float scale = getGlobalScale();
        pos.multLocal(-scale).multLocal(3,3,3);//get an arbitrary position for the light depending on the target bounding box
        pos.addLocal(target.getWorldTranslation());
        widget.setLocalTranslation(pos);
        widget.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    @Override
    protected void widgetUpdate(Spatial target, Spatial widget, PropertySet set, float tpf) {
        Property direction = set.getProperty("direction");
        if(!prevSpatialPos.equals(widget.getWorldTranslation())) {
            direction.setValue(widget.getWorldTranslation().mult(-1.0f).normalizeLocal());
            widget.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
            syncOrientation(widget, direction);
        }

        if(!prevLightDir.equals(direction.getValue())){
            updateWidgetFromDirection(target, widget, (Vector3f) direction.getValue() );
            syncOrientation(widget, direction);
        }
    }

    private void syncOrientation(Spatial widget, Property direction) {
        prevSpatialPos.set(widget.getWorldTranslation());
        prevLightDir.set((Vector3f)direction.getValue());
    }

    @Override
    protected Spatial makeWidget() {

        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);
        int radialSamples = 16;
        int lineSegments = 10;

        FloatBuffer posBuf = BufferUtils.createVector3Buffer((radialSamples + 1 + lineSegments + 1) * 2);
        FloatBuffer texBuf = BufferUtils.createVector2Buffer((radialSamples + 1 + lineSegments + 1) * 2);
        ShortBuffer idxBuf = BufferUtils.createShortBuffer((2 * (radialSamples + lineSegments)) * 2);

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        m.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        int idx = 0;

        idx = ShapeUtils.makeCircle(radialSamples, 0.14f, posBuf, texBuf, idxBuf, idx);
        idx = ShapeUtils.makeCircle(radialSamples, 0.11f, posBuf, texBuf, idxBuf, idx);
        idx = ShapeUtils.makeSegmentedLine(lineSegments, Axis.X , 0.4f, -0.2f, Vector3f.ZERO, posBuf, texBuf, idxBuf, idx);
        idx = ShapeUtils.makeSegmentedLine(lineSegments, Axis.Y , 0.4f, -0.2f, Vector3f.ZERO, posBuf, texBuf, idxBuf, idx);

        m.updateBound();
        m.setStatic();

        Geometry geom = new Geometry("LightDebug", m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setMaterial(dashed);


        Mesh line = new Mesh();
        line.setMode(Mesh.Mode.Lines);
        lineSegments = 30;

        posBuf = BufferUtils.createVector3Buffer(lineSegments + 1);
        texBuf = BufferUtils.createVector2Buffer(lineSegments + 1);
        idxBuf = BufferUtils.createShortBuffer(2 * lineSegments);

        line.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        line.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        line.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        ShapeUtils.makeSegmentedLine(lineSegments, Axis.Z , 4f, 0.2f, Vector3f.ZERO, posBuf, texBuf, idxBuf, 0);

        line.updateBound();
        line.setStatic();

        Geometry geom2 = new Geometry("lightDirection", line);
        geom2.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom2.setMaterial(dashed);


        Node widget = new Node();
        center.attachChild(geom);
        widget.attachChild(center);
        widget.attachChild(geom2);
        return widget;
    }

}