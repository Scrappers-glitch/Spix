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

package spix.swing;

import java.awt.Component;
import java.lang.reflect.*;
import java.util.Arrays;

import spix.props.Property;

/**
 *
 *
 *  @author    Paul Speed
 */
public class DefaultComponentFactory implements ComponentFactory {

    private Class<? extends Component> componentClass;
    private Constructor ctor;
    private int guiIndex = -1;
    private int propIndex = -1;
    private Object[] parms;
    
    public DefaultComponentFactory(Object... args ) {
        this(DefaultViewPanel.class, args);
    }
    
    public DefaultComponentFactory( Class<? extends Component> componentClass, Object... args ) {
        this.componentClass = componentClass;
        
        // Find the appropriate constructor
        for( Constructor<?> ctor : componentClass.getConstructors() ) {
            Class[] parms = ctor.getParameterTypes();
            if( parms.length <= args.length ) {
                continue;
            }
            for( int i = 0; i < parms.length; i++ ) {
                Class c = parms[i];
                if( SwingGui.class.isAssignableFrom(c) ) {
                    guiIndex = i;
                } else if( Property.class.isAssignableFrom(c) ) {
                    propIndex = i;
                }                
            }
            if( propIndex >= 0 ) {
                // We found the one we want
                this.ctor = ctor;
                this.parms = new Object[parms.length];
                break;
            }              
        }
        
        // Kind of a hacky way to shuffle in custom parameters
        int arg = 0;
        for( int i = 0; i < parms.length; i++ ) {
            if( arg >= args.length ) {
                break;
            }
            if( i != guiIndex && i != propIndex ) {
                parms[i] = args[arg++];
            }
        } 
    } 

    public Component createComponent( SwingGui gui, Property property ) {
        parms[propIndex] = property;
        if( guiIndex >= 0 ) {
            parms[guiIndex] = gui;
        }
        try {       
            return (Component)ctor.newInstance(parms);
        } catch( InstantiationException | IllegalAccessException | InvocationTargetException e ) {
            throw new RuntimeException("Error creating component of type:" + componentClass, e);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[ctor=" + ctor 
                        + ", guiIndex=" + guiIndex 
                        + ", propIndex=" + propIndex 
                        + ", args=" + Arrays.asList(parms) + "]"; 
    }
}
