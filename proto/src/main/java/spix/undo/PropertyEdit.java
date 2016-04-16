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

package spix.undo;

import com.google.common.base.MoreObjects;

import spix.core.Spix;
import spix.props.*;

/**
 *  Edits a property value by applying the old value or new value
 *  to its PropertySet depending on if the edit's undo() or redo()
 *  is called.
 *
 *  @author    Paul Speed
 */
public class PropertyEdit implements Edit {

    private Object target;
    private String property;
    private Object oldValue;
    private Object newValue;
    
    public PropertyEdit( Object target, String property, Object oldValue, Object newValue ) {
        this.target = target;
        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    protected Property getProperty( Spix spix ) {
        PropertySet set = spix.getPropertySet(target);
        return set.getProperty(property);
    }

    @Override
    public void undo( Spix spix ) {
        getProperty(spix).setValue(oldValue);        
    }
    
    @Override
    public void redo( Spix spix ) {
        getProperty(spix).setValue(newValue);        
    }
    
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .add("target", target)
            .add("property", property)
            .add("undoValue", oldValue)
            .add("redoValue", newValue)
            .toString();
    }
}
