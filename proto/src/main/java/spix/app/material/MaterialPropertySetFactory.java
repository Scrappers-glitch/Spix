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

import com.jme3.material.MatParam;
import com.jme3.material.Material;
import spix.app.material.hack.MatDefWrapper;
import spix.core.PropertySetFactory;
import spix.core.Spix;
import spix.props.BeanProperty;
import spix.props.DefaultPropertySet;
import spix.props.Property;
import spix.props.PropertySet;

import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 *  @author   Rémy Bouquet
 */
public class MaterialPropertySetFactory implements PropertySetFactory<Material>{

    public MaterialPropertySetFactory() {
    }

    public PropertySet createPropertySet(Material material, Spix spix ) {
        System.out.println("Need to create a property set for:" + material);
        List<Property> props = new ArrayList<>();

        props.add(BeanProperty.create(material, "name"));
        props.add(BeanProperty.create(material.getMaterialDef(), "name", "matDefName", false, null));
        props.add(BeanProperty.create(material, "key", "j3m path", false, null));

        for (MatParam matParam : material.getMaterialDef().getMaterialParams()) {
            props.add(new MatParamProperty(matParam.getName(), matParam.getVarType(), material));
        }

        return new DefaultPropertySet(material, props);
    }
}
