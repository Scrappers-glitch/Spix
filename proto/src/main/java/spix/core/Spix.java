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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.cache.*;

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

    private final HandlerRegistry<PropertySetFactory> handlers = new HandlerRegistry<>();

    /**
     *  A central cache for property set wrappers.  This is here instead of
     *  in the billboard class as there may be reason for classes other than
     *  the blackboard to create wrappers.  But all wrappers should be consistent, ie:
     *  one to one mapping for instances.
     */
    private final LoadingCache<Object, PropertySet> propertySetCache;

    public Spix() {
        this.propertySetCache = CacheBuilder.newBuilder()
                                       .weakKeys()
                                       .weakValues()
                                       .build(new PropertySetCacheLoader());
    }

    public Blackboard getBlackboard() {
        return blackboard;
    }

    public <T> void registerService( Class<? super T> type, T service ) {
        services.put(type, service);
    }

    public <T> T getService( Class<T> type ) {
        return type.cast(services.get(type));
    }

    public <T> void registerPropertySetFactory( Class<T> type, PropertySetFactory<T> factory ) {
        handlers.register(type, factory);
    }

    public <T> PropertySetFactory<T> getPropertySetFactory( Class<T> type ) {
        return (PropertySetFactory<T>)handlers.get(type, false);
    }

    /**
     *  Returns a PropertySet wrapper for the specified value, reusing one if it
     *  already exists.
     */
    public PropertySet getPropertySet( Object value ) {
        if( value == null ) {
            return null;
        }
        return propertySetCache.getUnchecked(value);
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

    public void runTasks() {
        Runnable r;
        while( (r = tasks.poll()) != null ) {
            r.run();
        }
    }

    private class PropertySetCacheLoader extends CacheLoader<Object, PropertySet> {

        public PropertySet load( Object value ) {
System.out.println("Creating a property set for:" + value);
            PropertySetFactory factory = getPropertySetFactory(value.getClass());
            if( factory == null ) {
                // Have to return something or the loading cache gets unhappy
                return new DefaultPropertySet(value);
            }
            return factory.createPropertySet(value, Spix.this);
        }
    }
}
