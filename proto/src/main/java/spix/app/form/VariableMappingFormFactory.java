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

package spix.app.form;

import spix.core.Spix;
import spix.form.*;
import spix.props.*;

import java.util.*;

import static com.oracle.jrockit.jfr.Transition.From;

/**
 *
 *
 *  @author    Paul Speed
 */
public class VariableMappingFormFactory extends DefaultFormFactory {

    public VariableMappingFormFactory() {
    }

    @Override
    public Form createForm( Spix spix, PropertySet properties, String context ) {


        FormField rightForm = getFormField(spix, properties, context, "from");
        FormField leftForm = getFormField(spix, properties, context, "to");

        Form result = new Form();
        Field field = createField(spix, properties.getProperty("condition"), context);
        result.add(field);

        // After all of that, stick the subforms at the bottom
        result.add(rightForm);
        result.add(leftForm);

        return result;
    }

    public FormField getFormField(Spix spix, PropertySet properties, String context, String whatVar) {
        Property type = properties.getProperty(whatVar + "VariableType");
        Property name = properties.getProperty(whatVar + "VariableName");
        Property nameSpace = properties.getProperty(whatVar + "VariableNode");
        Property mult = properties.getProperty(whatVar + "VariableMultiplicity");
        Property swizzle = properties.getProperty(whatVar + "VariableSwizzle");

        Form form = new Form();
        FormField ff = new FormField(whatVar + " " + type.getValue() + " " + nameSpace.getValue() + "." + name.getValue(), form);
        Field field1 = createField(spix, mult, context);
        Field field2 = createField(spix, swizzle, context);
        form.add(field1);
        form.add(field2);
        return ff;
    }
}
