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

import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import spix.app.material.MatParamProperty;
import spix.app.material.MaterialProperty;
import spix.core.PropertySetFactory;
import spix.core.Spix;
import spix.props.*;

import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 *
 *  @author    Paul Speed
 */
public class SpatialPropertySetFactory implements PropertySetFactory<Spatial> {

    private final static Logger logger = Logger.getLogger(SpatialPropertySetFactory.class.getName());

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

        if(spatial instanceof Geometry){
            Geometry g = (Geometry) spatial;

            Material material = g.getMaterial();

            props.add(BeanProperty.create(material, "name", "materialName", false, null));
            props.add(BeanProperty.create(material.getMaterialDef(), "assetName", "matDefFile", false, null));
            props.add(BeanProperty.create(material, "key", "matKey", false, null));

            for (MatParam matParam : material.getMaterialDef().getMaterialParams()) {
                if (matParam.getVarType() != VarType.Matrix4Array
                        && matParam.getVarType() != VarType.Matrix3Array
                        && matParam.getVarType() != VarType.FloatArray
                        && matParam.getVarType() != VarType.IntArray
                        && matParam.getVarType() != VarType.Vector4Array
                        && matParam.getVarType() != VarType.Vector3Array
                        && matParam.getVarType() != VarType.Vector2Array) {
                    props.add(new MatParamProperty(matParam.getName(), matParam.getVarType(), material));
                }
            }
        }

        return new DefaultPropertySet(spatial, props);
    }
}
