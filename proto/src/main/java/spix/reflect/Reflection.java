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

package spix.reflect;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.google.common.base.*;
import com.google.common.primitives.Primitives;

/**
 *  Static methods for creating reflection-based guava Functions and
 *  Suppliers.
 *
 *  @author    Paul Speed
 */
public class Reflection {
 
    /**
     *  Returns true if (in reflection) that source can be assigned to the
     *  target.  Generally, this means that target.isAssignableFrom(source)
     *  but a larger test is made with respect to primitive types and their
     *  autobox wrapper classes.
     */
    public static boolean isAssignable( Class target, Class source ) {
        // The easy way
        if( target.isAssignableFrom(source) ) {
            return true;
        }
        if( source.isPrimitive() ) {
            // Check to see if it will be autoboxed
            Class type = Primitives.wrap(source);
            if( target.isAssignableFrom(type) ) {
                return true;
            }   
        }
        if( target.isPrimitive() ) {
            // Check to see if it will be auto-unboxed
            Class type = Primitives.wrap(target);
            if( type.isAssignableFrom(source) ) {
                return true;
            }
        }
        return false;       
    }
 
    public static <T> Function<T, Void> setter( Object o, String methodName, Class<T> type ) {
        if( o == null ) {
            throw new IllegalArgumentException("Object cannot be null.");
        }
        // For now just find the first/best one.  We'll create an
        // overloaded setter wrapper for ambiguous cases later.
        for( Method m : o.getClass().getMethods() ) {
            Class[] types = m.getParameterTypes();
            if( types.length != 1 ) {
                continue;
            }
            if( !methodName.equals(m.getName()) ) {
                continue;
            }
            if( !isAssignable(types[0], type) ) {
                continue;               
            }
            // then this is the first match 
            return new Setter(o, m);
        } 
        
        throw new RuntimeException("No method found for:" + methodName + " types:" + type + " on:" + o.getClass());
    }
    
    private static class Setter<T> implements Function<T, Void> {
        private Object object;
        private Method method;
        
        public Setter( Object object, Method method ) {
            this.object = object;
            this.method = method;
        }
        
        public Void apply( T parameter ) {
            try {
                method.invoke(object, parameter);
                return null;
            } catch( IllegalAccessException | InvocationTargetException e ) {
                throw new RuntimeException("Error calling:" + method + " with:" + parameter, e);
            }
        }
        
        @Override
        public String toString() {
            return MoreObjects.toStringHelper("Setter")
                .add("method", method)
                .add("object", object)
                .toString();
        }
    }    
}
