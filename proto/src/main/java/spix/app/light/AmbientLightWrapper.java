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
import com.jme3.bounding.*;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.Axis;
import spix.app.utils.ShapeUtils;
import spix.props.PropertySet;

import java.nio.*;

import static spix.app.utils.ShapeUtils.makeSegmentedLine;


/**
 *
 * @author Rémy Bouquet
 */
public class AmbientLightWrapper extends LightWrapper<AmbientLight> {

    /**
     * @param light The light to be synced.
     */
    public AmbientLightWrapper(Node widget, AmbientLight light, Spatial target, AssetManager assetManager) {
        super(widget, light, target, assetManager);
    }

    @Override
    protected void setPositionRelativeToTarget(Spatial target, Spatial widget, AmbientLight light) {
        Vector3f pos = Vector3f.UNIT_Y.clone();
        float scale = getGlobalScale();
        pos.multLocal(scale).addLocal(0,4,0);//get an arbitrary position for the light depending on the target bounding box
        pos.addLocal(target.getWorldTranslation());
        widget.setLocalTranslation(pos);
    }

    @Override
    protected void widgetUpdate(Spatial target, Spatial widget, PropertySet set, float tpf) {
        //nothing to do
    }

    @Override
    protected Spatial makeWidget() {

        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);
        int lineSegments = 10;

        FloatBuffer posBuf = BufferUtils.createVector3Buffer(( lineSegments + 1) * 3);
        FloatBuffer texBuf = BufferUtils.createVector2Buffer(( lineSegments + 1) * 3);
        ShortBuffer idxBuf = BufferUtils.createShortBuffer(2 * (lineSegments * 3));

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        m.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        int idx = 0;

        idx = ShapeUtils.makeSegmentedLine(lineSegments, Axis.X , 0.3f, -0.15f,  new Vector3f(0, 0.1f, 0), posBuf, texBuf, idxBuf, idx);
        idx = ShapeUtils.makeSegmentedLine(lineSegments, Axis.X , 0.4f, -0.2f,  Vector3f.ZERO, posBuf, texBuf, idxBuf, idx);
        idx = ShapeUtils.makeSegmentedLine(lineSegments, Axis.X , 0.3f, -0.15f,  new Vector3f(0, -0.1f, 0), posBuf, texBuf, idxBuf, idx);

        m.updateBound();
        m.setStatic();

        Geometry geom = new Geometry("LightDebug", m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setMaterial(dashed);

        Node widget = new Node();
        center.attachChild(geom);
        widget.attachChild(center);

        return widget;

    }

}