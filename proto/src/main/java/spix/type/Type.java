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

package spix.type;

import com.google.common.primitives.Primitives;

/**
 *
 *
 *  @author    Paul Speed
 */
public class Type<T> {
    
    private final Class<T> javaClass;
    
    public Type( Class<T> javaClass ) {
        this.javaClass = javaClass;
    }
    
    public Class<T> getJavaType() {
        return javaClass;
    }
    
    public boolean isInstance( Object o ) {
        return javaClass.isInstance(o);
    }
    
    public boolean isAssignableFrom( Type type ) {
        if( javaClass.isAssignableFrom(type.javaClass) ) {
            return true;
        }
        // Else check for wrappers
        Class thisWrap = Primitives.wrap(javaClass);
        Class otherWrap = Primitives.wrap(type.javaClass);
        return thisWrap.isAssignableFrom(otherWrap);  
    }
 
    @Override
    public boolean equals( Object o ) {
        if( o == this ) {
            return true;
        }
        if( o == null || o.getClass() != getClass() ) {
            return false;
        }
        Type other = (Type)o;
        return other.javaClass == javaClass;       
    }
    
    @Override
    public int hashCode() {
        return javaClass.hashCode();
    }
    
    @Override   
    public String toString() {
        return "Type[" + javaClass.getName() + "]";
    }
}
