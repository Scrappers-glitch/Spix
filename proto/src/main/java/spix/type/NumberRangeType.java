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

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 *
 *
 *  @author    Paul Speed
 */
public class NumberRangeType<T extends Number> extends Type<T> {
    
    private T min;
    private T max;
    private T stepSize;
    
    public NumberRangeType( Class<T> javaClass, T min, T max, T stepSize ) {
        super(javaClass);
        if( min != null && !javaClass.isInstance(min) ) {
            throw new IllegalArgumentException("Minimum value is not an instance of class:" + javaClass);
        }
        if( max != null && !javaClass.isInstance(max) ) {
            throw new IllegalArgumentException("Maximum value is not an instance of class:" + javaClass);
        }
        if( stepSize != null && !javaClass.isInstance(stepSize) ) {
            throw new IllegalArgumentException("Step size value is not an instance of class:" + javaClass);
        }
        this.min = min;
        this.max = max;
        this.stepSize = stepSize;
    }

    public NumberRangeType( T min, T max, T stepSize ) {
        this((Class<T>)stepSize.getClass(), min, max, stepSize);
    }
 
    public T getMinimum() {
        return min;
    }
    
    public T getMaximum() {
        return max;
    }
    
    public T getPreferredStepSize() {
        return stepSize;
    }
    
    @Override
    public boolean equals( Object o ) {
        if( !super.equals(o) ) {
            return false;
        }
        NumberRangeType other = (NumberRangeType)o;
        if( !Objects.equals(min, other.min) ) {
            return false;
        }
        if( !Objects.equals(max, other.max) ) {
            return false;
        }
        if( !Objects.equals(stepSize, other.stepSize) ) {
            return false;
        }
        return true;
    }
 
    @Override   
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .omitNullValues()
            .add("min=", min)
            .add("max=", max)
            .add("stepSize=", stepSize)
            .add("javaType=", getJavaType())
            .toString();  
    }
}
