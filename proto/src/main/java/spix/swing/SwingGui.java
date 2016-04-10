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

import com.jme3.math.ColorRGBA;

import spix.core.Spix;
import spix.props.*;
import spix.type.*;
import spix.ui.*;

/**
 *  Consolidates the general swing-related user interface configuration
 *  and handling.
 *
 *  @author    Paul Speed
 */
public class SwingGui {

    public static final Class[] STANDARD_REQUEST_HANDLERS = {
            FileRequester.class,
            MessageRequester.class,
            ColorRequester.class  
        }; 

    public static final String EDIT_CONTEXT = "edit";
    

    private Spix spix;
    private Component rootWindow;
    private ContextHandlerRegistry<ComponentFactory> componentFactories = new ContextHandlerRegistry<>(); 

    public SwingGui( Spix spix, Component rootWindow ) {
        this(spix, rootWindow, STANDARD_REQUEST_HANDLERS); 
    }
    
    public SwingGui( Spix spix, Component rootWindow, Class... requestHandlers ) {
        this.spix = spix;
        this.rootWindow = rootWindow;
 
        for( Class c : requestHandlers ) {
            setupSwingService(c);
        }
        
        // Setup some default component factories just to avoid hassles
        componentFactories.getRegistry(null).register(Object.class, new DefaultComponentFactory());
        
        HandlerRegistry<ComponentFactory> editFactories = componentFactories.getRegistry(EDIT_CONTEXT); 
        DefaultComponentFactory floatFactory = new DefaultComponentFactory(FloatPanel.class);
        editFactories.register(Float.class, floatFactory);
        editFactories.register(Float.TYPE, floatFactory);
        editFactories.register(ColorRGBA.class, new DefaultComponentFactory(ColorPanel.class));
        editFactories.register(Enum.class, new DefaultComponentFactory(EnumPanel.class));  
        editFactories.register(String.class, new DefaultComponentFactory(StringPanel.class));  
    }    
    
    public Spix getSpix() {
        return spix;
    }
 
    public <T> T getService( Class<T> type ) {
        return spix.getService(type);
    }
 
    public Component getRootWindow() {
        return rootWindow;
    } 
 
    /**
     *  Wraps the specified property set for swing-render thread safety
     *  if necessary.  Returns the original delegate if there is no need to
     *  wrap the object.
     */
    public PropertySet wrap( PropertySet delegate ) {
        // Always do it for now... we'll do some threading checks later
        return new SwingPropertySetWrapper(this, delegate);
    }
 
    public void setupSwingService( Class type ) {
        if( FileRequester.class.isAssignableFrom(type) ) {
            spix.registerService(type, new SwingFileRequester(this));
        } else if( MessageRequester.class.isAssignableFrom(type) ) { 
            spix.registerService(type, new SwingMessageRequester(this));
        } else if( ColorRequester.class.isAssignableFrom(type) ) {
            spix.registerService(type, new SwingColorRequester(this));
        } else {
            throw new IllegalArgumentException("Swing requester not found for:" + type);
        }  
    }   
 
    public void registerComponentFactory( Class type, ComponentFactory factory ) {
        componentFactories.getRegistry(null).register(type, factory);   
    }
    
    public void registerComponentFactory( String context, Class type, ComponentFactory factory ) {
        componentFactories.getRegistry(context).register(type, factory);   
    }
    
    public void registerComponentFactory( String context, Type type, ComponentFactory factory ) {
        componentFactories.getRegistry(context).register(type, factory);
    }
    
    public Component createComponent( String context, Property prop ) {
        ComponentFactory factory = componentFactories.getHandler(context, prop.getType(), false);
System.out.println("context:" + context + "  type:" + prop.getType() + "  factory=" + factory);        
        if( factory == null ) {
            // Might have requested for a primitive type so we'll retry with just object
            // Kind of a hack.
            factory = componentFactories.getHandler(context, Object.class, false); 
        }
        if( factory == null ) {
            throw new RuntimeException("ComponentFactory not found for context:" + context + "  type:" + prop.getType());
        }
        return factory.createComponent(this, prop);
    }  
}
