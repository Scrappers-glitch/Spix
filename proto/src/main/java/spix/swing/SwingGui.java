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
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;

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
    private Thread edt; 

    public SwingGui( Spix spix, Component rootWindow ) {
        this(spix, rootWindow, STANDARD_REQUEST_HANDLERS); 
    }
    
    public SwingGui( Spix spix, Component rootWindow, Class... requestHandlers ) {
    
System.out.println("Creating SwingGui on thread:" + Thread.currentThread());    
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
        
        // If this is the AWT thread then save it.
        if( SwingUtilities.isEventDispatchThread() ) {
            this.edt = Thread.currentThread();
        } else {
            try {
                // Else we will force the issue
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        edt = Thread.currentThread();
                    }
                });
            } catch( InvocationTargetException | InterruptedException e ) {
                throw new RuntimeException("Error capturing EDT", e);
            }
        }   
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
      
    /**
     *  Wraps the specified property set for swing-render thread safety
     *  if necessary.  Returns the original delegate if there is no need to
     *  wrap the object.
     */
    public PropertySet wrap( PropertySet delegate ) {
System.out.println("wrap()  Current thread:" + Thread.currentThread() + "  property set thread:" + delegate.getCreatingThread());
        if( delegate.getCreatingThread() == edt ) {
            // It's already safe for swing as it was created on the swing thread.
            return delegate;
        }
        // Else we need to wrap it for swing.
        return new SwingPropertySetWrapper(this, delegate);
    }
 
    /**
     *  Executes a runnable on the swing thread.  If this is being called
     *  from the swing thread then it will execute immediately.
     */
    public void runOnSwing( Runnable run ) {
        if( SwingUtilities.isEventDispatchThread() ) {
System.out.println("runOnSwing(): running directly:" + run);        
            run.run();
        } else {
System.out.println("runOnSwing(): dispatching:" + run);        
            SwingUtilities.invokeLater(run);
        }
    }
    
    /**
     *  Executes a runnable on the render thread.  If this is being called
     *  from the render thread then it will execute immediately.
     */
    public void runOnRender( Runnable run ) {
        // For the moment, we'll pretend that this is only ever called from
        // the swing thread or the render thread.  We'll be more disciminating later
        // by keeping track of the render thread reference.
        if( SwingUtilities.isEventDispatchThread() ) {
System.out.println("runOnRender(): dispatching:" + run);        
            spix.enqueueTask(run);
        } else {
System.out.println("runOnRender(): running directly:" + run);        
            run.run();
        }         
    }
}
