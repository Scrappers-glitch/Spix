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

package spix;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 *  A PropertySet implementation that recreates its main object
 *  whenever one of the nested properties changes.  This is useful
 *  for allowing property editing on objects that are otherwise
 *  immutable.  Property values are passed to a constructor in the
 *  order that they were specifed on the PropertySet constructor.
 *
 *  @author    Paul Speed
 */
public class ImmutableObjectPropertySet extends AbstractPropertySet {
     
    private Property[] propertyArray;
    private Object[] values;
    private Constructor ctor;
    
    public ImmutableObjectPropertySet( Object object, Constructor ctor, Property... props ) {
        this(null, object, ctor, props);      
    }

    public ImmutableObjectPropertySet( Property parent, Object object, Constructor ctor, Property... props ) {
        super(parent, object, props);
        this.ctor = ctor;
        this.propertyArray = props;
        this.values = new Object[props.length];
    }
    
    public static ImmutableObjectPropertySet create( Property parent, Property... props ) {
        Object object = parent.getValue();
        Class[] types = new Class[props.length];
        for( int i = 0; i < types.length; i++ ) {
            types[i] = props[i].getType();
        }
        return new ImmutableObjectPropertySet(parent, object, findCtor(object.getClass(), types), props);    
    }
    
    private static Constructor findCtor( Class type, Class[] types ) {
        try {
            // Try the easy way first
            return type.getConstructor(types);
        } catch( NoSuchMethodException e ) {
            // Do the more difficult lookup
            for( Constructor c : type.getConstructors() ) {
                if( c.getParameterTypes().length != types.length ) {
                    continue;
                }
                // Else do a looser check
                Class[] parmTypes = c.getParameterTypes();
                boolean matches = true; 
                for( int i = 0; i < types.length; i++ ) {
                    if( !parmTypes[i].isAssignableFrom(types[i]) ) {
                        matches = false;
                        break;       
                    }   
                }
                if( matches ) {
                    return c;
                }
            }
            return null;
        }         
    }    
    
    protected void resetValues() {
        for( int i = 0; i < values.length; i++ ) {
            values[i] = propertyArray[i].getValue();
        }
    }
 
    protected Object createObject() {
        resetValues();
System.out.println("Create values for:" + Arrays.asList(values));        
        try {
            return ctor.newInstance(values);
        } catch( InstantiationException | IllegalAccessException | InvocationTargetException e ) {
            throw new RuntimeException("Error running " + ctor + " with:" + Arrays.asList(values));
        }
    }
 
    @Override
    protected void propertyChange( PropertyChangeEvent e ) {
        setObject(createObject());          
    }
}
