/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package spix.app.material;

import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import spix.props.AbstractProperty;
import spix.type.Type;

/**
 * Created by bouquet on 05/10/16.
 */
public class MaterialProperty extends AbstractProperty {


    private Material material;
    private Geometry geometry;


    public MaterialProperty(Geometry geom, Material material) {
        super("material");
        this.geometry = geom;
        this.material = material;

    }

    @Override
    public Type getType() {
        return new Type<>(Material.class);
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof Material)) {
            return;
        }
        Material newMat = (Material) value;
        if (changed(newMat)) {
            Object old = material.clone();

            material.setName(newMat.getName());

            if (newMat.getKey() != null && !newMat.getKey().equals(material.getKey())) {
                material.setKey(newMat.getKey());
            }

            if (!material.contentEquals(value)) {
                if (newMat.getMaterialDef() != material.getMaterialDef()) {
                    material = newMat;
                    geometry.setMaterial(material);
                } else {
                    //TODO merge material
                    for (MatParam matParam : newMat.getParams()) {
                        MatParam param = material.getParam(matParam.getName());
                        if (!matParam.equals(param)) {
                            material.setParam(matParam.getName(), matParam.getVarType(), matParam.getValue());
                        }
                    }
                }
            }
            firePropertyChange(old, material, true);
        }
    }

    private boolean changed(Material newMat) {
        if (newMat.getName() != null && !newMat.getName().equals(material.getName()) ||
                newMat.getKey() != null && !newMat.getKey().equals(material.getKey()) ||
                !material.contentEquals(newMat)) {
            return true;
        }
        return false;
    }

    @Override
    public Object getValue() {
        return material;
    }
}
