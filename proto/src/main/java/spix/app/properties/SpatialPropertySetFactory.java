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

package spix.app.properties;

import java.util.*;

import com.jme3.math.Vector3f;
import com.jme3.scene.*;

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

        Property localTranslation = BeanProperty.create(spatial, "localTranslation", true);
        Property localScale = BeanProperty.create(spatial, "localScale", true);
        Property localRotation = BeanProperty.create(spatial, "localRotation", true);

        // For manipulators, create some special transform properties that work in world
        // space.
        props.add(new WorldTranslationProperty(spatial, localTranslation));
        //only allowing non uniform scaling for Geometries (leaves of the graph)
        props.add(new WorldScaleProperty(spatial, localScale, spatial instanceof Geometry));
        props.add(new WorldRotationProperty(spatial, localRotation));

        // Configure the local translation property
        props.add(localTranslation);
       // props.add(localScale);
        props.add(localRotation);

        return new DefaultPropertySet(spatial, props);
    }


}
