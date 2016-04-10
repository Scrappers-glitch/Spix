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

package spix.props;

import java.util.Objects;

import spix.type.Type;

/**
 *  Used by the PropertySetWrapper to wrap the contained properties
 *  of its delegate/wrapped PropertySet.
 *
 *  @author    Paul Speed
 */
public class PropertyWrapper extends AbstractProperty {
    private PropertySetWrapper parent;
    private Property delegate;

    // Keep our own copy of the value just in case
    private Object value;
    private Type type;
    
    public PropertyWrapper( PropertySetWrapper parent, Property delegate ) {
        super(delegate.getId(), delegate.getName());
        this.parent = parent;
        this.delegate = delegate;
        this.type = delegate.getType();
        this.value = delegate.getValue();
    }
 
    @Override
    public Type getType() {
        return type;
    }
    
    @Override
    public void setValue( Object value ) {
System.out.println("  ## PropertyWrapper.setValue(" + value + ")  old:" + this.value);    
        Object old = this.value;
        boolean changed = !Objects.equals(old, value);
        if( old == value ) {
            // We can't really tell... we might have been given an altered
            // version of ourselves back.  Could be fixed with a second clone
            changed = true;
        }        
        this.value = value;
        if( changed ) {
            // Let the parent know that the property has changed
            parent.updateProperty(this, delegate, old, value);
            
            // And let our own listeners know
            firePropertyChange(old, value, false);            
        }
    }
    
    @Override
    public Object getValue() {
        return value;
    }
}
