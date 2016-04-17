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

import java.util.*;

import spix.core.Spix;
import spix.form.*;
import spix.props.Property;
import spix.props.PropertySet;

/**
 *
 *
 *  @author    Paul Speed
 */
public class SpatialFormFactory extends DefaultFormFactory {

    private static final Set<String> LOCAL_XFORMS = new HashSet<>();
    private static final Set<String> WORLD_XFORMS = new HashSet<>();
    static {
        LOCAL_XFORMS.add("localTranslation");
        LOCAL_XFORMS.add("localScale");
        LOCAL_XFORMS.add("localRotation");
        WORLD_XFORMS.add("worldTranslation");
        WORLD_XFORMS.add("worldScale");
        WORLD_XFORMS.add("worldRotation");
    }

    public SpatialFormFactory() {
    }

    @Override
    public Form createForm( Spix spix, PropertySet properties, String context ) {

        // We basically want the default behavior except we will pull out the
        // world transforms into their own form and the local transforms into
        // their own form.
        
        Form localTrans = new Form();        
        Form worldTrans = new Form();
        
        Form result = new Form();
        for( Property property : properties ) {
 
            Field field = createField(spix, property, context);
                       
            if( LOCAL_XFORMS.contains(property.getId()) ) {
                localTrans.add(field);
            } else if( WORLD_XFORMS.contains(property.getId()) ) {
                worldTrans.add(field);
            } else {
                // Default behavior           
                result.add(field);
            }
        }

        // After all of that, stick the subforms at the bottom
        result.add(new FormField("Local Transform", localTrans));
        result.add(new FormField("World Transform", worldTrans));

        return result;
    }
}
