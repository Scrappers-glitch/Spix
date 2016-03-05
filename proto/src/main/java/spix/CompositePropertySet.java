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
 *  A PropertySet implementation that resets the object's values
 *  all at once using a CompositeMutator.
 *
 *  @author    Paul Speed
 */
public class CompositePropertySet extends AbstractPropertySet {
     
    private Property[] propertyArray;
    private Object[] values;
    private CompositeMutator mutator;
    
    public CompositePropertySet( Object object, CompositeMutator mutator, Property... props ) {
        this(null, object, mutator, props);      
    }

    public CompositePropertySet( Property parent, Object object, CompositeMutator mutator, Property... props ) {
        super(parent, object, props);
        this.mutator = mutator;
        this.propertyArray = props;
        this.values = new Object[props.length];
    }
    
    public static CompositePropertySet create( Property parent, CompositeMutator mutator, Property... props ) {
        return new CompositePropertySet(parent, parent.getValue(), mutator, props);    
    }
    
    protected void resetValues() {
        for( int i = 0; i < values.length; i++ ) {
            values[i] = propertyArray[i].getValue();
        }
    }
 
    protected Object resetObject() {
        resetValues();
System.out.println("Reset values for:" + Arrays.asList(values));        
        mutator.apply(getObject(), values);
        return getObject();
    }
 
    @Override
    protected void propertyChange( PropertyChangeEvent e ) {
        setObject(resetObject());          
    }
}
