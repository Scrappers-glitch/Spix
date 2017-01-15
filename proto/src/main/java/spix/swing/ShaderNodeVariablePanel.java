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

import com.jme3.math.Vector3f;
import com.jme3.shader.ShaderNodeVariable;
import com.jme3.util.clone.Cloner;
import org.slf4j.*;
import spix.props.*;

import java.awt.*;
import java.beans.PropertyChangeEvent;

/**
 *  Presents a general editor for Vector3f objects.
 *
 *  @author    Paul Speed
 */
public class ShaderNodeVariablePanel extends AbstractPropertyPanel<Component>
                             implements MulticolumnComponent {

    static Logger log = LoggerFactory.getLogger(ShaderNodeVariablePanel.class);

    private SwingGui gui;
    private PropertySet props;
    private boolean updating = false;

    public ShaderNodeVariablePanel(SwingGui gui, Property prop ) {
        super(prop);
        this.gui = gui;
        Cloner cloner = new Cloner();
 
        // So Vector3f is pretty easy... we could even move this
        // definition to be more global.
        
        // Make a clone for our local property reference so that we can
        // properly check old values against new for property changes
        // and stuff.
        ShaderNodeVariable var = cloner.clone((ShaderNodeVariable) prop.getValue());

        Property condition = BeanProperty.create(var, "condition");
        Property multiplicity = BeanProperty.create(var, "multiplicity");

 
        props = new DefaultPropertySet(prop, var, condition, multiplicity) {
            @Override
            protected void propertyChange( PropertyChangeEvent e ) {    
                if( updating ) {
                    return;
                }
                if( log.isTraceEnabled() ) {
                    log.trace("propertyChange(" + e + ")");
                }
                super.propertyChange(e);
            }                   
        };       
        PropertyEditorPanel panel = new PropertyEditorPanel(gui, null, true); 
        panel.setObject(props);
        setView(panel);
    }
    
    protected void updateView( Component view, Object value ) {
 
        if( log.isTraceEnabled() ) {
            log.trace(getProperty().getId() + ": updateView(" + value + ")");
        }
        
        // In these cases where we hold a value outside in its own
        // property set, it's _extremely_ disruptive to the event chains
        // to send back property change events that are only due to us
        // adjusting our local values.
        updating = true;
        try {       
            // Just set each value
            ShaderNodeVariable v = (ShaderNodeVariable)value;
            props.getProperty("condition").setValue(v.getCondition());
            props.getProperty("multiplicity").setValue(v.getMultiplicity());
        } finally {
            updating = false;
        }
    }
}
