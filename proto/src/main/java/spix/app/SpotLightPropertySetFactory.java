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

import com.jme3.light.*;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import spix.core.*;
import spix.props.*;
import spix.type.Type;

import java.util.*;


/**
 *
 *
 *  @author   Rémy Bouquet
 */
public class SpotLightPropertySetFactory implements PropertySetFactory<SpotLight> {

    public SpotLightPropertySetFactory() {
    }

    public PropertySet createPropertySet(SpotLight light, Spix spix ) {
        System.out.println("Need to create a property set for:" + light);

        // Just expose some minimum properties so we can test
        // editing and manipulator widgets

        List<Property> props = new ArrayList<>();

        props.add(BeanProperty.create(light, "name"));
        props.add(BeanProperty.create(light, "color"));

        // For manipulators, create some special transform properties that work in world
        // space.
        props.add(new WorldTranslationProperty(light));

        // Configure the local translation property
     //   props.add(BeanProperty.create(light, "rotation"));

        return new DefaultPropertySet(light, props);
    }

    //TODO try to listen to the WorldTranslation property to update the light widget.
    //Implementing this allows the translation widget to automatically activate when the light  is selected.
    //Also moving the translation widget does move the light. Though the light widget needs to be updated.
    private class WorldTranslationProperty extends AbstractProperty {
        private final SpotLight light;

        public WorldTranslationProperty( SpotLight light ) {
            super("worldTranslation");
            this.light = light;
        }

        public Type getType() {
            return new Type(Vector3f.class);
        }

        public void setValue( Object value ) {
            if( value == null ) {
                return;
            }
            Vector3f v = (Vector3f)value;
            Vector3f last = light.getPosition();
            if( v.equals(last) ) {
                return;
            }

            light.setPosition(v);

            firePropertyChange(last, light.getPosition(), true);
        }

        public Object getValue() {
            return light.getPosition();
        }
    }
}
