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

import javax.swing.*;                 
import javax.swing.event.*;

import com.google.common.base.*;

import spix.core.ToStringFunction;
import spix.props.Property;
import spix.type.Type;
import spix.type.NumberRangeType;

/**
 *  A panel for editing float values.
 *
 *  @author    Paul Speed
 */
public class FloatPanel extends AbstractPropertyPanel<JSpinner> {

    private SpinnerNumberModel model;
    private boolean updating = false;

    public FloatPanel( Property prop ) {
        super(prop);
        this.model = new SpinnerNumberModel();
        Type type = prop.getType();
        if( type instanceof NumberRangeType ) {
            NumberRangeType numRange = (NumberRangeType)type; 
            model.setStepSize(numRange.getPreferredStepSize());
            model.setMinimum((Comparable)numRange.getMinimum());
            model.setMaximum((Comparable)numRange.getMaximum());
        } else {
            // Set it up with defaults and no range
            model.setStepSize(0.1f);
        }        
        model.setValue(prop.getValue());
        model.addChangeListener(new ModelObserver());
        setView(new JSpinner(model));                
    }
    
    protected void updateView( JSpinner label, Object value ) {
        model.setValue(value);
    }

    private class ModelObserver implements ChangeListener {
        public void stateChanged( ChangeEvent e ) {
            updateProperty(model.getValue());
        }
    }    
}
