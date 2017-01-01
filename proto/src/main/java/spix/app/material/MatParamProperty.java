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
import com.jme3.math.*;
import com.jme3.shader.VarType;
import com.jme3.texture.*;
import spix.props.AbstractProperty;
import spix.type.Type;

import java.util.ArrayList;

/**
 * Created by bouquet on 05/10/16.
 */
public class MatParamProperty extends AbstractProperty {


    private Type type;
    private VarType varType;
    private Material material;

    public MatParamProperty(String name, VarType varType, Material material) {
        super(name);
        this.type = new Type(getJavaType(varType));
        this.material = material;
        this.varType = varType;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setValue(Object value) {
        MatParam param = material.getParam(getId());
        Object old = null;
        if (param != null) {
           old = param.getValue();
        }
        this.material.setParam(getId(), varType, value);
        firePropertyChange(old, value, true);
    }

    public Material getMaterial() {
        return material;
    }

    @Override
    public Object getValue() {
        MatParam param = material.getParam(getId());
        if (param == null) {
            try {
                return getNewInstance(varType);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return param.getValue();
    }

    private Class getJavaType(VarType type) {
        switch (type) {
            case Boolean:
                return Boolean.class;
            case Float:
                return Float.class;
            case Int:
                return Integer.class;
            case Vector2:
                return Vector2f.class;
            case Vector3:
                return Vector3f.class;
            case Vector4:
                //This is not perfect, but most of the time this is meant as a color.
                return ColorRGBA.class;
            case Texture2D:
                return Texture2D.class;
            case Texture3D:
                return Texture3D.class;
            case TextureArray:
                return TextureArray.class;
            case TextureCubeMap:
                return TextureCubeMap.class;
            case Matrix3:
                return Matrix3f.class;
            case Matrix4:
                return Matrix4f.class;
            case Matrix4Array:
            case Matrix3Array:
            case FloatArray:
            case IntArray:
            case Vector2Array:
            case Vector3Array:
            case Vector4Array:
                return ArrayList.class;
            default:
                return Object.class;
        }
    }

    private Object getNewInstance(VarType type) throws IllegalAccessException, InstantiationException {
        switch (type) {
            case Boolean:
                return new Boolean(false);
            case Float:
                return new Float(0);
            case Int:
                return new Integer(0);
            default:
                return getJavaType(type).newInstance();
        }
    }
}
