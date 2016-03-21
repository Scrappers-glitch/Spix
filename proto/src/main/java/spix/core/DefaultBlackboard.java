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

import java.beans.*;
import java.util.*;
import java.util.Objects;
import java.util.concurrent.*;

import com.google.common.base.*;

import org.slf4j.*;

import groovy.util.ObservableList;
import groovy.util.ObservableMap;

import spix.*;
import spix.reflect.*;
import spix.props.*;
import spix.type.*;

/**
 *
 *
 *  @author    Paul Speed
 */
public class DefaultBlackboard implements Blackboard {

    static Logger log = LoggerFactory.getLogger(DefaultBlackboard.class);

    private final Spix spix;

    private final ObservableMap properties = new ObservableMap();

    private final Map<DispatcherKey, Dispatcher> dispatchers = new ConcurrentHashMap<>();

    public DefaultBlackboard( Spix spix ) {
        this.spix = spix;
    }

    public void set( String path, Object value ) {
        Object original = properties.put(path, value);
        if( value == original ) {
            return;
        }

        if( original instanceof ObservableList ) {
            detachDispatcher(path, (ObservableList)original);
        }

        if( value instanceof ObservableList ) {
            attachDispatcher(path, (ObservableList)value);
        }
    }

    public Object get( String path ) {
        return properties.get(path);
    }

    public <T> T get( String path, Class<T> type ) {
        Object result = get(path);
        if( !type.isInstance(result) ) {
            return null;
        }
        return type.cast(result);
    }

    /**
     *  Returns a PropertySet wrapper for a specified blackboard value.
     */
    public PropertySet getPropertySet( String path ) {
        return spix.getPropertySet(properties.get(path));
    }

    public void bind( String billboardProperty, Object target, String targetProperty ) {
        bind(billboardProperty, target, targetProperty, (Function)null);
    }

    public void bind( String billboardProperty, Object target, String targetProperty, Predicate transform ) {
        bind(billboardProperty, target, targetProperty,
             transform == null ? null : Functions.forPredicate(transform));
    }

    public void bind( String billboardProperty, Object target, String targetProperty, Function transform ) {
        BeanProperty property = BeanProperty.create(target, targetProperty);
        if( property == null ) {
            throw new RuntimeException("No property found:" + targetProperty + " on " + target);
        }
        Binding binding = new Binding(property, transform);
        addListener(billboardProperty, binding);
    }

    public void addListener( String property, PropertyChangeListener l ) {
        properties.addPropertyChangeListener(property, l);
    }

    public void removeListener( String property, PropertyChangeListener l ) {
        properties.removePropertyChangeListener(property, l);
    }

    protected void detachDispatcher( String property, ObservableList list ) {
        DispatcherKey key = new DispatcherKey(property, list);
        Dispatcher listener = dispatchers.remove(key);
        if( listener != null ) {
            list.removePropertyChangeListener(listener);
        }
    }

    protected void attachDispatcher( String property, ObservableList list ) {
        DispatcherKey key = new DispatcherKey(property, list);
        Dispatcher listener = dispatchers.get(key);
        if( listener != null ) {
            log.warn("Property already has a dispatcher:" + property + " for value:" + list);
            return;
        }
        listener = new Dispatcher(property, list);
        list.addPropertyChangeListener(listener);
        dispatchers.put(key, listener);
    }

    private class Binding implements PropertyChangeListener {
        private Property target;
        private Function transform;

        public Binding( Property target, Function transform ) {
            if( target == null ) {
                throw new IllegalArgumentException("Target cannot be null.");
            }
            this.target = target;
            this.transform = transform;
        }

        public void propertyChange( PropertyChangeEvent event ) {
            Object value = event.getNewValue();
            if( transform != null ) {
                value = transform.apply(value);
            }
            target.setValue(value);
        }
    }

    private class DispatcherKey {
        private String property;
        private Object object;

        public DispatcherKey( String property, Object object ) {
            this.property = property;
            this.object = object;
        }

        public boolean equals( Object o ) {
            if( o == this ) {
                return true;
            }
            if( o == null || o.getClass() != getClass() ) {
                return false;
            }
            DispatcherKey other = (DispatcherKey)o;
            if( !Objects.equals(property, other.property) ) {
                return false;
            }
            return other.object == object;
        }

        public int hashCode() {
            return Objects.hash(property, System.identityHashCode(object));
        }
    }

    private class Dispatcher implements PropertyChangeListener {
        private String property;
        private Object object;

        public Dispatcher( String property, Object object ) {
            this.property = property;
            this.object = object;
        }

        public void propertyChange( PropertyChangeEvent event ) {
            if( event instanceof ObservableList.ElementEvent ) {
                // Forward it
                PropertyChangeEvent newEvent = new PropertyChangeEvent(object, property, null, object);
                for( PropertyChangeListener l : properties.getPropertyChangeListeners(property) ) {
                    // So we can't forward the list events along which is a shame.
                    // It's because we can't change their properties.
                    l.propertyChange(newEvent);
                }
            }
        }
    }

}

