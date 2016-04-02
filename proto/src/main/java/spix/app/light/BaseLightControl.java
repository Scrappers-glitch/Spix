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

import com.jme3.bounding.*;
import com.jme3.light.*;
import com.jme3.math.Vector3f;
import com.jme3.renderer.*;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;


/**
 *
 * @author Rémy Bouquet
 */
public abstract class BaseLightControl<L extends Light> extends AbstractControl {

    private L light;
    private Spatial target;
    private Vector3f prevTargetPos = new Vector3f();

    /**
     * @param light The light to be synced.
     */
    public BaseLightControl(L light, Spatial target) {
        this.light = light;
        this.target = target;
    }

    public L getLight() {
        return light;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (spatial != null && light != null) {
            if(!prevTargetPos.equals(target.getWorldTranslation())){
                setPositionRelativeToTarget(target, spatial, light);
                prevTargetPos.set(target.getWorldTranslation());
            }
        }
        widgetUpdate(target, spatial, light, tpf);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        if(this.spatial != null) {
            setPositionRelativeToTarget(target, this.spatial, light);
        }
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

    protected abstract void setPositionRelativeToTarget(Spatial target, Spatial widget, L light);
    protected abstract void widgetUpdate(Spatial target, Spatial widget, L light, float tpf);

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // nothing to do
    }

}