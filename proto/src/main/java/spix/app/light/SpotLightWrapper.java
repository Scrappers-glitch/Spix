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
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.Axis;
import spix.props.*;

import java.nio.*;


/**
 *
 * @author Rémy Bouquet
 */
public class SpotLightWrapper extends LightWrapper<SpotLight> {

    //Spatial
    private Vector3f prevSpatialPos = new Vector3f();
    private Vector3f prevSpatialScale = Vector3f.UNIT_XYZ.normalize();
    private Quaternion prevSpatialRot = new Quaternion();

    //Light
    private float prevLightSpotRange = 0;
    private float prevInnerAngle = 0;
    private float prevOuterAngle = 0;
    private Vector3f prevLightPos = new Vector3f();
    private Vector3f prevLightDir = new Vector3f();

    private Quaternion tmpRot = new Quaternion();

    /**
     * @param light The light to be synced.
     */
    public SpotLightWrapper(Node widget, SpotLight light, Spatial target, AssetManager assetManager) {
        super(widget, light, target, assetManager);
    }

    @Override
    protected void setPositionRelativeToTarget(Spatial target, Spatial widget, SpotLight light) {
        //Not relative, we have a position for that light
        widget.setLocalTranslation(light.getPosition());
        updateWidgetRotationFromDirection(widget, light.getDirection());
        widget.setLocalScale(light.getSpotRange());

        updateCircleFromAngle((Node) widget, "innerCircle", light.getSpotInnerAngle());
        updateCircleFromAngle((Node) widget, "outerCircle", light.getSpotOuterAngle());

    }

    private void updateWidgetRotationFromDirection(Spatial widget, Vector3f direction) {
        tmpRot.lookAt(direction, Vector3f.UNIT_Y);
        widget.setLocalRotation(tmpRot);
    }

    private void updateCircleFromAngle(Node widget, String geomName, float angle) {
        Spatial circle = widget.getChild(geomName);
        float scale = FastMath.tan(angle);
        circle.setLocalScale(scale);
    }

    @Override
    protected void widgetUpdate(Spatial target, Spatial widget, PropertySet set, float tpf) {

        Property position = set.getProperty("worldTranslation");
        Property direction = set.getProperty("direction");
        Property spotRange = set.getProperty("spotRange");
        Property spotInnerAngle = set.getProperty("spotInnerAngle");
        Property spotOuterAngle = set.getProperty("spotOuterAngle");

        //Widget to light
        //widget has been moved update light position
        if(!prevSpatialPos.equals(widget.getWorldTranslation())) {
            position.setValue(widget.getWorldTranslation());
            syncPosition(widget, position);
        }
        //widget has been scaled update light spot range
        if(!prevSpatialScale.equals(widget.getWorldScale())) {
            spotRange.setValue(widget.getWorldScale().length() / Vector3f.UNIT_XYZ.length());
            syncScale(widget, spotRange);
        }
        //widget has been rotated update light direction
        if(!prevSpatialRot.equals(widget.getWorldRotation())) {
            direction.setValue(widget.getWorldRotation().getRotationColumn(2));
            syncDirection(widget, direction);
        }

        //Light to widget
        //Light has been moved update widget position
        if(!prevLightPos.equals(position.getValue())) {
            widget.setLocalTranslation((Vector3f)position.getValue());
            syncPosition(widget, position);
        }
        //light radius has been changed, update widget scale.
        float range = (float) spotRange.getValue();
        if(prevLightSpotRange != range){
            widget.setLocalScale(range);
            syncScale(widget, spotRange);
        }
        //Light changed direction, update widget rotation
        if(!prevLightDir.equals(direction.getValue())){
            updateWidgetRotationFromDirection(widget,(Vector3f) direction.getValue());
            syncDirection(widget, direction);
        }
        //Light inner angle has been changed, update widget's inner circle.
        float innerAngle = (float) spotInnerAngle.getValue();
        if(prevInnerAngle != innerAngle){
            updateCircleFromAngle((Node) widget, "innerCircle", innerAngle);
            prevInnerAngle = innerAngle;
        }
        //Light outer angle has been changed, update widget's outer circle.
        float outerAngle = (float) spotOuterAngle.getValue();
        if(prevOuterAngle != outerAngle){
            updateCircleFromAngle((Node) widget, "outerCircle", outerAngle);
            prevOuterAngle = outerAngle;
        }

    }

    private void syncDirection(Spatial widget, Property direction) {
        prevSpatialRot.set(widget.getWorldRotation());
        prevLightDir.set((Vector3f)direction.getValue());
    }

    private void syncScale(Spatial widget, Property spotRange) {
        prevSpatialScale.set(widget.getWorldScale());
        prevLightSpotRange = (float)spotRange.getValue();
    }

    private void syncPosition(Spatial widget, Property position) {
        prevSpatialPos.set(widget.getWorldTranslation());
        prevLightPos.set((Vector3f)position.getValue());
    }

    @Override
    protected Spatial makeWidget() {
        Geometry lightGeom = makeCircleGeometry("lightDebug", 0.11f, 16);

        Mesh line = new Mesh();
        line.setMode(Mesh.Mode.Lines);
        int lineSegments = 30;

        FloatBuffer posBuf = BufferUtils.createVector3Buffer((lineSegments + 1));
        FloatBuffer texBuf = BufferUtils.createVector2Buffer((lineSegments + 1));
        ShortBuffer idxBuf = BufferUtils.createShortBuffer(2 * lineSegments);

        line.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        line.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        line.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        makeSegmentedLine(lineSegments, Axis.Z , 1f, 0, Vector3f.ZERO, posBuf, texBuf, idxBuf, 0);

        line.updateBound();
        line.setStatic();

        Geometry lightDirection = new Geometry("lightDirection", line);
        lightDirection.setQueueBucket(RenderQueue.Bucket.Transparent);
        lightDirection.setMaterial(dashed);


        Node widget = new Node();
        Node cone = new Node("cone");
        Geometry innerCircle = makeCircleGeometry("innerCircle", 1, 128);
        Geometry outerCircle = makeCircleGeometry("outerCircle", 1, 128);
        innerCircle.move(0,0,1);
        outerCircle.move(0,0,1);

        center.attachChild(lightGeom);
        widget.attachChild(center);
        cone.attachChild(lightDirection);
        cone.attachChild(innerCircle);
        cone.attachChild(outerCircle);
        widget.attachChild(cone);

        return widget;
    }

}
