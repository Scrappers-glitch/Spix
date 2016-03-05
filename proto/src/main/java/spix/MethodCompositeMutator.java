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

import java.lang.reflect.*;
import java.util.Arrays;

/**
 *  Passes the values to a single Method object.
 *
 *  @author    Paul Speed
 */
public class MethodCompositeMutator<T> implements CompositeMutator<T> {
 
    private Method method;
    
    public MethodCompositeMutator( Method method ) {
        this.method = method;
    }
    
    public static <T> MethodCompositeMutator<T> create( Class<T> objectType, String name, Class... parameterTypes ) {
        Method m = findMethod(objectType, name, parameterTypes);
        if( m == null ) {
            throw new RuntimeException("Could not find method:" + name + " on " + objectType 
                                        + " with matching parameter types:" + Arrays.asList(parameterTypes)); 
        } 
        return new MethodCompositeMutator(m);
    }
    
    private static Method findMethod( Class objectType, String name, Class[] parameterTypes ) {
        try {
            // Try the simple lookup first
            return objectType.getMethod(name, parameterTypes);
        } catch( NoSuchMethodException e ) {
            // Do a more complicated lookup
            for( Method m : objectType.getMethods() ) {
                if( m.getParameterTypes().length != parameterTypes.length ) {
                    continue;
                }
                if( !name.equals(m.getName()) ) {
                    continue;
                }
                boolean matches = true;
                for( int i = 0; i < parameterTypes.length; i++ ) {
                    if( !m.getParameterTypes()[i].isAssignableFrom(parameterTypes[i]) ) {
                        matches = false;
                        break;
                    }
                }
                if( matches ) {
                    return m;
                }                               
            }
            return null;   
        }
    } 
    
    @Override   
    public void apply( T object, Object... values ) {
        try {
            method.invoke(object, values);
        } catch( IllegalAccessException | InvocationTargetException e ) {
            throw new RuntimeException("Error setting values using " + method, e);
        }
    }
 
    @Override          
    public String toString() {
        return getClass().getName() + "[" + method + "]";
    }
}
