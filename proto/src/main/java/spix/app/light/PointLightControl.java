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
import com.jme3.math.Vector3f;
import com.jme3.scene.*;


/**
 *
 * @author Rémy Bouquet
 */
public class PointLightControl extends BaseLightControl<PointLight> {

    private Vector3f prevSpatialPos = new Vector3f();
    private Vector3f prevSpatialScale = new Vector3f();

    /**
     * @param light The light to be synced.
     */
    public PointLightControl(PointLight light, Spatial target) {
        super(light, target);
    }

    @Override
    protected void setPositionRelativeToTarget(Spatial target, Spatial widget, PointLight light) {
        //Not relative, we have a position for that light
        widget.setLocalTranslation(light.getPosition());
        widget.setLocalScale(light.getRadius());
    }

    @Override
    protected void widgetUpdate(Spatial target, Spatial widget, PointLight light, float tpf) {
        if(!prevSpatialPos.equals(widget.getWorldTranslation())) {
            light.setPosition(widget.getWorldTranslation());
            prevSpatialPos.set(widget.getWorldTranslation());
        }
        if(!prevSpatialScale.equals(widget.getWorldScale())) {
            light.setRadius(widget.getWorldScale().length() / Vector3f.UNIT_XYZ.length());
            prevSpatialScale.set(widget.getWorldScale());
        }
    }

}