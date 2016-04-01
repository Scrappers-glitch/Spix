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

package spix.app;

import java.util.*;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import spix.core.*;
import spix.props.*;
import spix.type.Type;


/**
 *
 *
 *  @author    Paul Speed
 */
public class SpatialPropertySetFactory implements PropertySetFactory<Spatial> {

    public SpatialPropertySetFactory() {
    }

    public PropertySet createPropertySet( Spatial spatial, Spix spix ) {
        System.out.println("Need to create a property set for:" + spatial);

        // Just expose some minimum properties so we can test
        // editing and manipulator widgets

        List<Property> props = new ArrayList<>();

        props.add(BeanProperty.create(spatial, "name"));
        props.add(BeanProperty.create(spatial, "cullHint"));
        props.add(BeanProperty.create(spatial, "queueBucket"));

        // For manipulators, create some special transform properties that work in world
        // space.
        props.add(new WorldTranslationProperty(spatial));

        // Configure the local translation property
        props.add(BeanProperty.create(spatial, "localTranslation"));
        props.add(BeanProperty.create(spatial, "localRotation"));

        return new DefaultPropertySet(spatial, props);
    }

    private class WorldTranslationProperty extends AbstractProperty {
        private final Spatial spatial;
        private Vector3f last = new Vector3f();

        public WorldTranslationProperty( Spatial spatial ) {
            super("worldTranslation");
            this.spatial = spatial;
        }

        public Type getType() {
            return new Type(Vector3f.class);
        }

        public void setValue( Object value ) {
            if( value == null ) {
                return;
            }
            Vector3f v = (Vector3f)value;
            last.set(spatial.getWorldTranslation());
            if( v.equals(last) ) {
                return;
            }

            // Else see how we'd have to move the spatial to get it
            // to the specified world translation... so find the point
            // in the parent's local space and calculate a delta from
            // the child's currentl location.
            Vector3f delta;
            if( spatial.getParent() != null ) {
                Vector3f local = spatial.getParent().worldToLocal(v, null);
                delta = local.subtract(spatial.getLocalTranslation());
            } else {
                // It is the root... so everything is already in world space
                delta = v.subtract(spatial.getLocalTranslation());
            }

            //...and that should be how far we need to move it
            spatial.move(delta);

            firePropertyChange(last, spatial.getWorldTranslation(), true);
        }

        public Object getValue() {
            return spatial.getWorldTranslation();
        }
    }
}
