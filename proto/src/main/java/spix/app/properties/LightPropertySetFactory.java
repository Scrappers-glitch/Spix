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

import com.jme3.bounding.BoundingVolume;
import com.jme3.light.*;
import com.jme3.math.FastMath;
import jdk.nashorn.internal.runtime.PropertyListeners;
import spix.app.DefaultConstants;
import spix.app.light.LightWrapper;
import spix.core.*;
import spix.props.*;
import spix.type.NumberRangeType;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;


/**
 *
 *
 *  @author   Rémy Bouquet
 */
public class LightPropertySetFactory implements PropertySetFactory<LightWrapper>{

    public LightPropertySetFactory() {
    }

    public PropertySet createPropertySet(LightWrapper wrapper, Spix spix ) {
        System.out.println("Need to create a property set for:" + wrapper);

        // Just expose some minimum properties so we can test
        // editing and manipulator widgets

        List<Property> props = new ArrayList<>();
        props.add(BeanProperty.create(wrapper.getLight(), "name"));
        props.add(BeanProperty.create(wrapper.getLight(), "enabled"));

        Light.Type type = wrapper.getLight().getType();
        if(type == Light.Type.Spot) {
            props.add(BeanProperty.create(wrapper, "color"));
            props.add(BeanProperty.create(wrapper, "intensity",
                    new NumberRangeType(1f, null, 0.1f)));
            props.add(BeanProperty.create(wrapper.getLight(), "direction"));
            props.add(BeanProperty.create(wrapper.getLight(), "spotRange", 
                                          new NumberRangeType(0f, null, 0.1f)));
            props.add(BeanProperty.create(wrapper.getLight(), "spotOuterAngle", 
                                          new NumberRangeType(0f, FastMath.HALF_PI, 0.01f)));
            props.add(BeanProperty.create(wrapper.getLight(), "spotInnerAngle", 
                                          new NumberRangeType(0f, FastMath.HALF_PI, 0.01f)));

            //HACK Alert: We create a property on the position attribute of the light and pass it as if it was the local translation of the widget
            //so that it's updated along with the worldTranslation of the widget,
            //That's probably utterly wrong and we should probably do as suggested in the
            //WorldTranslationProperty and have a central dispatcher.
            Property position = BeanProperty.create(wrapper.getLight(), "position", true);
            props.add(new WorldTranslationProperty(wrapper.getWidget(), position));
            props.add(new WorldRotationProperty(wrapper.getWidget(), null));
            props.add(new WorldScaleProperty(wrapper.getWidget(), null, false));
        } else if (type == Light.Type.Directional){
            props.add(BeanProperty.create(wrapper, "color"));
            props.add(BeanProperty.create(wrapper, "intensity",
                    new NumberRangeType(1f, null, 0.1f)));
            props.add(BeanProperty.create(wrapper.getLight(), "direction"));
            props.add(new WorldTranslationProperty(wrapper.getWidget(), null));
        } else if (type == Light.Type.Point){
            props.add(BeanProperty.create(wrapper, "color"));
            props.add(BeanProperty.create(wrapper, "intensity",
                    new NumberRangeType(1f, null, 0.1f)));
            props.add(BeanProperty.create(wrapper.getLight(), "radius",
                    new NumberRangeType(0f, null, 0.1f)));

            //props.add(new WorldRotationProperty(wrapper.getWidget(), null));
            //HACK Alert: We create a property on the position attribute of the light and pass it as if it was the local translation of the widget
            //so that it's updated along with the worldTranslation of the widget,
            //That's probably utterly wrong and we should probably do as suggested in the
            //WorldTranslationProperty and have a central dispatcher.
            Property position = BeanProperty.create(wrapper.getLight(), "position", true);
            props.add(new WorldTranslationProperty(wrapper.getWidget(), position));
            props.add(new WorldScaleProperty(wrapper.getWidget(), null, false));
        } else if (type == Light.Type.Probe) {
            LightProbe probe = ((LightProbe) wrapper.getLight());
            if(probe.getAreaType() == LightProbe.AreaType.Spherical) {
                props.add(BeanProperty.create(probe.getArea(), "radius",
                        new NumberRangeType(0f, null, 0.1f)));
            }

            Property areaType = BeanProperty.create(wrapper, "areaType");
            areaType.addPropertyChangeListener(new AreaTypePropListener(spix));
            props.add(areaType);

            //props.add(new WorldRotationProperty(wrapper.getWidget(), null));
            //HACK Alert: We create a property on the position attribute of the light and pass it as if it was the local translation of the widget
            //so that it's updated along with the worldTranslation of the widget,
            //That's probably utterly wrong and we should probably do as suggested in the
            //WorldTranslationProperty and have a central dispatcher.
            Property position = BeanProperty.create(wrapper.getLight(), "position", true);
            props.add(new WorldTranslationProperty(wrapper.getWidget(), position));
            if(probe.getAreaType() == LightProbe.AreaType.OrientedBox) {
                Property rotation = BeanProperty.create(((LightProbe) wrapper.getLight()).getArea(), "rotation", true);
                props.add(new WorldRotationProperty(wrapper.getWidget(), rotation));

                Property scale = BeanProperty.create(((LightProbe) wrapper.getLight()).getArea(), "extent", true);
                props.add(new WorldScaleProperty(wrapper.getWidget(), scale));

            } else {
                props.add(new WorldScaleProperty(wrapper.getWidget(), null, false));
            }

        } else {
            //ambient light
            props.add(new WorldTranslationProperty(wrapper.getWidget(), null));
        }

        wrapper.setLightPropertySet(new DefaultPropertySet(wrapper, props));

        return wrapper.getLightPropertySet();
    }

    private class AreaTypePropListener implements PropertyChangeListener{
        Spix spix;

        public AreaTypePropListener(Spix spix) {
            this.spix = spix;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            SelectionModel model = spix.getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
            Object sel = model.getSingleSelection();
            spix.refresh(sel);
            model.setSingleSelection(null);
            model.setSingleSelection(sel);
        }
    }
}
