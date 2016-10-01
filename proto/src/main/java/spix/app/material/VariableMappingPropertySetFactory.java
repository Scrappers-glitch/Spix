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

package spix.app.material;

import com.jme3.shader.*;
import spix.core.*;
import spix.props.*;
import spix.type.Type;

import java.util.*;


/**
 *
 *
 *  @author   Rémy Bouquet
 */
public class VariableMappingPropertySetFactory implements PropertySetFactory<VariableMapping>{

    public VariableMappingPropertySetFactory() {
    }

    public PropertySet createPropertySet(VariableMapping mapping, Spix spix ) {
        System.out.println("Need to create a property set for:" + mapping);
        List<Property> props = new ArrayList<>();

        props.add(BeanProperty.create(mapping, "condition"));
        props.add(BeanProperty.create(mapping.getRightVariable(), "type", "fromVariableType", false, null));
        props.add(BeanProperty.create(mapping.getRightVariable(), "name", "fromVariableName", false, null));
        props.add(BeanProperty.create(mapping.getRightVariable(), "nameSpace", "fromVariableNode", false, null));
        props.add(BeanProperty.create(mapping.getRightVariable(), "multiplicity", "fromVariableMultiplicity", false, null));
        props.add(BeanProperty.create(mapping, "rightSwizzling", "fromVariableSwizzle", false, null));
        props.add(BeanProperty.create(mapping.getLeftVariable(), "type", "toVariableType", false, null));
        props.add(BeanProperty.create(mapping.getLeftVariable(), "name", "toVariableName", false, null));
        props.add(BeanProperty.create(mapping.getLeftVariable(), "nameSpace", "toVariableNode", false, null));
        props.add(BeanProperty.create(mapping.getLeftVariable(), "multiplicity", "toVariableMultiplicity", false, null));
        props.add(BeanProperty.create(mapping, "leftSwizzling", "toVariableSwizzle", false, null));
        return new DefaultPropertySet(mapping, props);
    }

    public static class MappingsListProperty extends AbstractProperty{
        public MappingsListProperty(String id) {
            super(id);
        }

        @Override
        public Type getType() {
            return new Type(List.class);
        }

        @Override
        public void setValue(Object value) {

        }

        @Override
        public Object getValue() {
            return null;
        }
    }
}
