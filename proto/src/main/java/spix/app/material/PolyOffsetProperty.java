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

import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.shader.VarType;
import com.jme3.texture.*;
import spix.props.AbstractProperty;
import spix.type.Type;

/**
 * Created by bouquet on 05/10/16.
 */
public class PolyOffsetProperty extends AbstractProperty {


    private Type type;

    private RenderState state;

    public PolyOffsetProperty(RenderState state, String name) {
        super(name);
        this.type = new Type(float.class);
        this.state = state;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setValue(Object value) {
        Object old = getValue();
        switch (getId()) {
            case "offsetFactor":
                state.setPolyOffset((float) value, state.getPolyOffsetUnits());
                break;
            case "offsetUnits":
                state.setPolyOffset(state.getPolyOffsetFactor(), (float) value);
                break;
        }

        firePropertyChange(old, value, false);
    }


    @Override
    public Object getValue() {
        switch (getId()) {
            case "offsetFactor":
                return state.getPolyOffsetFactor();
            case "offsetUnits":
                return state.getPolyOffsetUnits();
        }
        return null;
    }

}
