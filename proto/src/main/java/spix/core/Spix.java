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

package spix.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.cache.*;

import spix.form.*;
import spix.props.*;
import spix.type.*;

/**
 *
 *
 *  @author    Paul Speed
 */
public class Spix {

    private final Blackboard blackboard = new DefaultBlackboard(this);

    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    private final Map<Class, Object> services = new ConcurrentHashMap<>();

    private final HandlerRegistry<PropertySetFactory> propertySetFactories = new HandlerRegistry<>();

    /**
     *  Special marker value indicating that an object has no property set wrapper.
     *  It's a peculiarity of the cache we use that it doesn't like nulls for values.
     */
    private static final DefaultPropertySet NULL_PROPERTIES = new DefaultPropertySet(null);

    private final ContextHandlerRegistry<FormFactory> formFactories = new ContextHandlerRegistry<>();

    /**
     *  A central cache for property set wrappers.  This is here instead of
     *  in the billboard class as there may be reason for classes other than
     *  the blackboard to create wrappers.  But all wrappers should be consistent, ie:
     *  one to one mapping for instances.
     */
    private final LoadingCache<Object, PropertySet> propertySetCache;

    /**
     *  Notified about various spix internal events.
     */
    private final List<SpixListener> listeners = new CopyOnWriteArrayList<>();

    public Spix() {
        this.propertySetCache = CacheBuilder.newBuilder()
                                       .weakKeys()
                                       .weakValues()
                                       .build(new PropertySetCacheLoader());

        // Setup the default form factory that will cover all objects that
        // have property set wrappers
        formFactories.getRegistry(ContextHandlerRegistry.DEFAULT).register(Object.class, new DefaultFormFactory());
    }

    public Blackboard getBlackboard() {
        return blackboard;
    }

    public <T> T registerService( Class<? super T> type, T service ) {
        services.put(type, service);
        return service;
    }

    public <T> T getService( Class<T> type ) {
        return type.cast(services.get(type));
    }

    public <T> PropertySetFactory<T> registerPropertySetFactory( Class<T> type, 
                                                                 PropertySetFactory<T> factory ) {
        propertySetFactories.register(type, factory);
        return factory;
    }

    public <T> PropertySetFactory<T> getPropertySetFactory( Class<T> type ) {
        return (PropertySetFactory<T>)propertySetFactories.get(type, false);
    }

    /**
     *  Returns a PropertySet wrapper for the specified value, reusing one if it
     *  already exists.
     */
    public PropertySet getPropertySet( Object value ) {
        if( value == null ) {
            return null;
        }
        PropertySet result = propertySetCache.getUnchecked(value);
        return result != NULL_PROPERTIES ? result : null;
    }

    public FormFactory registerFormFactory( Type type, FormFactory factory ) {
        return registerFormFactory(null, type, factory);
    }
    
    public FormFactory registerFormFactory( String context, Type type, FormFactory factory ) {
        context = context != null ? context : ContextHandlerRegistry.DEFAULT;
        formFactories.getRegistry(context).register(type, factory);
        return factory;
    }

    public FormFactory getFormFactory( Type type ) {
        return getFormFactory(null, type);
    }
    
    public FormFactory getFormFactory( String context, Type type ) {
        context = context != null ? context : ContextHandlerRegistry.DEFAULT;
        return formFactories.getHandler(context, type, false);
    }

    public Form createForm( PropertySet properties ) {
        return createForm(properties, null);
    }
    
    public Form createForm( PropertySet properties, String context ) {
        if( properties == null ) {
            return null;
        }
        context = context != null ? context : ContextHandlerRegistry.DEFAULT;
        FormFactory factory = getFormFactory(context, properties.getType());
        return factory.createForm(this, properties, context);
    }

    public <T> void sendResponse( final RequestCallback<T> callback, final T result ) {
        tasks.add(new Runnable() {
            public void run() {
                callback.done(result);
            }
        });
    }

    public void runAction( final Action action ) {
        tasks.add(new Runnable() {
            public void run() {
                action.performAction(Spix.this);
            }
        });
    }

    public void enqueueTask( Runnable runnable ) {
        tasks.add(runnable);
    }

    public void runTasks() {
        Runnable r;
        while( (r = tasks.poll()) != null ) {
            r.run();
        }
    }

    public void addSpixListener( SpixListener l ) {
        listeners.add(l);
    }

    public void removeSpixListener( SpixListener l ) {
        listeners.remove(l);
    }

    private PropertySet firePropertySetCreated( Object value, PropertySet set ) {        
        for( SpixListener l : listeners ) {
            PropertySet ps = l.propertySetCreated(value, set);
            if( ps != null ) {
                set = ps;
            }
        } 
        return set;       
    } 

    private class PropertySetCacheLoader extends CacheLoader<Object, PropertySet> {

        public PropertySet load( Object value ) {
System.out.println("Creating a property set for:" + value);
            PropertySetFactory factory = getPropertySetFactory(value.getClass());
            if( factory == null ) {
                // Have to return something or the loading cache gets unhappy
                return NULL_PROPERTIES;
            }
            PropertySet result = factory.createPropertySet(value, Spix.this);
            return firePropertySetCreated(value, result);
        }
    }
}
