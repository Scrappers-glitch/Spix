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

import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.*;


/**
 *
 * @author Rémy Bouquet
 */
public class SpotLightControl extends BaseLightControl<SpotLight> {

    private Vector3f prevSpatialPos = new Vector3f();
    private Vector3f prevSpatialScale = Vector3f.UNIT_XYZ.normalize();
    private Quaternion prevSpatialRot = new Quaternion();
    private Quaternion tmpRot = new Quaternion();

    /**
     * @param light The light to be synced.
     */
    public SpotLightControl(SpotLight light, Spatial target) {
        super(light, target);
    }

    @Override
    protected void setPositionRelativeToTarget(Spatial target, Spatial widget, SpotLight light) {
        //Not relative, we have a position for that light
        widget.setLocalTranslation(light.getPosition());
        tmpRot.lookAt(light.getDirection(),Vector3f.UNIT_Y);
        widget.setLocalRotation(tmpRot);
        widget.setLocalScale(light.getSpotRange());

        Spatial innerCircle = ((Node)widget).getChild("innerCircle");
        Spatial outerCircle = ((Node)widget).getChild("outerCircle");
        float innerScale = FastMath.tan(light.getSpotInnerAngle());
        float outerScale = FastMath.tan(light.getSpotOuterAngle());
        innerCircle.setLocalScale(innerScale);
        outerCircle.setLocalScale(outerScale);
    }

    @Override
    protected void widgetUpdate(Spatial target, Spatial widget, SpotLight light, float tpf) {
        if(!prevSpatialPos.equals(widget.getWorldTranslation())) {
            light.setPosition(widget.getWorldTranslation());
            prevSpatialPos.set(widget.getWorldTranslation());
        }
        if(!prevSpatialScale.equals(widget.getWorldScale())) {
            light.setSpotRange(widget.getWorldScale().length() / Vector3f.UNIT_XYZ.length());
            prevSpatialScale.set(widget.getWorldScale());
        }
        if(!prevSpatialRot.equals(widget.getWorldRotation())) {
            tmpRot.lookAt(light.getDirection(),Vector3f.UNIT_Y);
            widget.setLocalRotation(tmpRot);
            prevSpatialRot.set(widget.getWorldRotation());
        }
    }

}
