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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

import spix.type.Type;

/**
 *  Wraps property sets to allow subclasses to intercept the setting
 *  of properties and the firing of change events.  By default, all
 *  interception methods delegate directly to the wrapped set.
 *
 *  @author    Paul Speed
 */
public class PropertySetWrapper implements PropertySet {
 
    private final PropertySet delegate;
    private final Type type;
    private final Map<String, Property> properties = new LinkedHashMap<>();
    private final WrapperListener listener = new WrapperListener();
    private boolean attached = false;    
    private boolean updating = false;
    
    public PropertySetWrapper( PropertySet delegate ) {
        this.delegate = delegate;
        this.type = delegate.getType();
        for( Property p : delegate ) {
             properties.put(p.getId(), wrap(p));
        }
    }
 
    public void attach() {
        if( attached ) {
            return;
        }        
        for( Property p : delegate ) {
System.out.println("Adding listener:" + listener + " to:" + p);        
             p.addPropertyChangeListener(listener);
             
             // And update the values to match
             properties.get(p.getId()).setValue(p.getValue());  
        }        
        attached = true;
    }
    
    public void release() {
        if( !attached ) {
            return;
        }
        for( Property p : delegate ) {
             p.removePropertyChangeListener(listener);  
        }
        attached = false;        
    }
    
    protected Property wrap( Property prop ) {
        return new PropertyWrapper(this, prop);
    }  
    
    public Property getProperty( String name ) {
        return properties.get(name);
    }
 
    public Iterator<Property> iterator() {
        return properties.values().iterator();
    }
    
    public Type getType() {
        return type; 
    }
        
    protected void updateProperty( Property wrapper, Property original, Object oldValue, Object newValue ) {
        // Right now just pass the property through
System.out.println("### updateProperty(" + wrapper.getId() + ", " + oldValue + ", " + newValue + ")");
        updating = true;
        try {        
            original.setValue(newValue);
        } finally {
            updating = false;
        }
    }
 
    protected boolean isUpdating() {
        return updating;
    }
    
    protected void updateWrapper( String id, Object value ) {
System.out.println("### updateWrapper(" + id + ", " + value + ")");
        if( updating ) {
            // This event is as a result of the updateProperty() causing an
            // update to the delegate which then calls us back.  Presumes
            // the updateProperty() and updateWrapper() are checking on the
            // same thread.
            return;
        }
        properties.get(id).setValue(value);    
    }
    
    private class WrapperListener implements PropertyChangeListener {
        
        public void propertyChange( PropertyChangeEvent event ) {
            System.out.println("Wrapped object changed:" + event);
            updateWrapper(event.getPropertyName(), event.getNewValue());
        }
    }
}
