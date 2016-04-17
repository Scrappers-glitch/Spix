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

import java.beans.*;
import java.lang.reflect.*;
import java.util.Objects;

import com.google.common.base.MoreObjects;

import com.jme3.util.clone.Cloner;

import spix.type.Type;

/**
 *
 *
 *  @author    Paul Speed
 */
public class BeanProperty extends AbstractProperty {

    private static final Cloner CLONER = new Cloner();

    private Object object;
    private Method getter;
    private Method setter;
    private Type type;
    private boolean cloneOldValues;

    public BeanProperty( Object object, String name, Method getter, Method setter, boolean cloneOldValues ) {
        super(name);
        this.object = object;
        this.getter = getter;
        this.setter = setter;
        this.cloneOldValues = cloneOldValues;
        if( getter != null ) {
            this.type = new Type(getter.getReturnType());
        } else {
            // Must have a setter then
            this.type = new Type(setter.getParameterTypes()[0]);
        }
    }

    public BeanProperty( Object object, String name, Method getter, Method setter, boolean cloneOldValues, Type overrideType ) {
        this(object, name, getter, setter, cloneOldValues);
        
        if( overrideType != null ) { 
            // Make sure the detected type is compatible with the override type
            if( !this.type.isAssignableFrom(overrideType) ) {
                throw new IllegalArgumentException("Type override:" + overrideType + " is incompatible with bean property type:" + this.type);
            }
            this.type = overrideType;
        }       
    }

    private static Method findSetter( Class type, String methodName, Class parameterType ) {
        try {
            // Try the easy lookup first
            return type.getMethod(methodName, parameterType);
        } catch( NoSuchMethodException e ) {
            // Do a looser search
            for( Method m : type.getMethods() ) {
                if( m.getParameterTypes().length != 1 ) {
                    continue;
                }
                if( !methodName.equals(m.getName()) ) {
                    continue;
                }
                if( m.getParameterTypes()[0].isAssignableFrom(parameterType) ) {
                    return m;
                }
            }
            return null;
        }
    }

    public static BeanProperty create( Property parent, String name ) {
        return create(parent.getValue(), name);
    }

    public static BeanProperty create( Object object, String name ) {
        return create(object, name, null); 
    }

    public static BeanProperty create( Object object, String name, boolean cloneOldValues ) {
        return create(object, name, cloneOldValues, null); 
    }
    
    public static BeanProperty create( Object object, String name, Type overrideType ) {
        return create(object, name, false, overrideType);
    }
    
    public static BeanProperty create( Object object, String name, boolean cloneOldValues, Type overrideType ) {
        try {
            // Use the bean property info to find the appropriate property
            // We'll move this method later as we will likely have different
            // BeanProperty implementations for lists, etc.
            Class type = object.getClass();
            BeanInfo info = Introspector.getBeanInfo(type);

            //System.out.println("info:" + info);
            for( PropertyDescriptor pd : info.getPropertyDescriptors() ) {
                //System.out.println("    pd:" + pd);
                if( !name.equals(pd.getName()) ) {
                    continue;
                }

                Method write = pd.getWriteMethod();
                Method read = pd.getReadMethod();
                if( write == null ) {
                    // JME has some setters that return non-void types and Java Beans
                    // standard introspection doesn't like that.
                    String n = "s" + read.getName().substring(1);
                    write = findSetter(type, n, read.getReturnType());
                }

                return new BeanProperty(object, name, read, write, cloneOldValues, overrideType);
            }

            return null;
        } catch( IntrospectionException e ) {
            throw new RuntimeException("Error creating bean property:" + name, e);
        }
    }

    @Override
    public Type getType() {
        return type;
    }

    protected Object cloneValue( Object value ) {
        if( value instanceof Cloneable ) {
            try {
                return CLONER.javaClone(value); 
            } catch( CloneNotSupportedException e ) {
                throw new RuntimeException("Error cloning Cloneable object:" + value, e);
            }
        } else {
            return value;
        }
    }
    
    @Override
    public void setValue( Object value ) {
        try {
            Object old = getValue();

            boolean changed = !Objects.equals(old, value);
            if( !cloneOldValues && old == value ) {
                // Then we can't really tell so we have to assume it changed
                changed = true;
            }

            if( cloneOldValues ) {
                // Clone the old value so that the change listener will be 
                // guaranteed to have a different old value than new.
                // This is to cover cases like setLocalTranslation() where the
                // 'old' value is mutable and will change underneath us.                
                old = cloneValue(old); 
            } 
 
            setter.invoke(object, value);

            // We can't let the superclass do the check because many of our
            // setters set back to the existing value... so old and value will
            // be the same after invoke().
            if( changed ) {            
                firePropertyChange(old, value, false);
            }
        } catch( IllegalAccessException | InvocationTargetException e ) {
            throw new RuntimeException("Error setting property:" + getName(), e);
        }
    }

    @Override
    public Object getValue() {
        try {
            return getter == null ? null : getter.invoke(object);
        } catch( IllegalAccessException | InvocationTargetException e ) {
            throw new RuntimeException("Error getting property:" + getName(), e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("object", object)
                .add("name", getName())
                .add("getter", getter)
                .add("setter", setter)
                .toString();
    }
}
