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

package spix.props;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

import org.slf4j.*;

import com.google.common.base.MoreObjects;

import spix.type.Type;

/**
 *
 *
 *  @author    Paul Speed
 */
public abstract class AbstractPropertySet implements PropertySet {

    static Logger log = LoggerFactory.getLogger(AbstractPropertySet.class);

    private Object object;
    private Property parent;
    private Type type;
    private final Map<String, Property> properties = new LinkedHashMap<>();
    private final Thread creatingThread;

    protected AbstractPropertySet( Property parent, Object object, Property... props ) {
        this(parent, object, Arrays.asList(props));
    }

    protected AbstractPropertySet( Property parent, Object object, Iterable<Property> props ) {
        this.parent = parent;
        this.object = object;
        this.type = object != null ? new Type(object.getClass()) : null;
        this.creatingThread = Thread.currentThread();

        // We own the child properties we are passed so it should be safe to
        // register a listener without having to worry about unregistering it later.
        PropertyObserver observer = parent == null ? null : new PropertyObserver();
        for( Property p : props ) {
            properties.put(p.getId(), p);
            if( observer != null ) {
                p.addPropertyChangeListener(observer);
            }
        }
    }

    @Override
    public final Type getType() {
        return type;
    }

    @Override
    public final Property getProperty( String name ) {
        return properties.get(name);
    }

    @Override
    public Iterator<Property> iterator() {
        return properties.values().iterator();
    }

    @Override
    public Thread getCreatingThread() {
        return creatingThread;
    }

    protected Property getParent() {
        return parent;
    }

    protected void setObject( Object o ) {
        if( log.isTraceEnabled() ) {
            log.trace(getType() + ":setObject(" + o + ")");
        }
        this.object = o;
        if( parent != null ) {
            parent.setValue(object);
        }
    }

    protected Object getObject() {
        return object;
    }

    protected abstract void propertyChange( PropertyChangeEvent e );

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getName())
                .add("object", object)
                .add("properties", properties)
                .toString();
    }

    private class PropertyObserver implements PropertyChangeListener {

        public void propertyChange( PropertyChangeEvent e ) {
            AbstractPropertySet.this.propertyChange(e);
        }
    }
}
