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

package spix.swing;

import java.awt.event.*;
import javax.swing.*;                 
import javax.swing.event.*;

import com.google.common.base.*;

import spix.core.ToStringFunction;
import spix.props.Property;
import spix.type.Type;
import spix.type.NumberRangeType;

/**
 *  A panel for editing enum values.
 *
 *  @author    Paul Speed
 */
public class EnumPanel extends AbstractPropertyPanel<JComboBox> {

    private Function<Object, String> toString;    

    public EnumPanel( Property prop ) {
        this(prop, ToStringFunction.INSTANCE);
    }

    public EnumPanel( Property prop, Function<Object, String> toString ) {
        super(prop);
        this.toString = toString;
 
        Object[] enumValues = prop.getType().getJavaType().getEnumConstants();
        setView(new JComboBox(enumValues));
        getView().addItemListener(new ComboObserver());        
    }
    
    protected void updateView( JComboBox combo, Object value ) {
        combo.setSelectedItem(value);
    }

    private class ComboObserver implements ItemListener {
        public void itemStateChanged( ItemEvent e ) {
            updateProperty(getView().getSelectedItem());
        }
    }    
}
