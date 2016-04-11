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

import java.awt.*;
import java.beans.PropertyChangeEvent;
import javax.swing.*;

import org.slf4j.*;

import com.jme3.math.*;

import spix.props.*;
import spix.type.*;

/**
 *  Presents several tabs or subpanels for configuring a quaternion
 *  value in different ways.
 *
 *  @author    Paul Speed
 */
public class QuaternionPanel extends AbstractPropertyPanel<Component> 
                             implements MulticolumnComponent {

    static Logger log = LoggerFactory.getLogger(QuaternionPanel.class); 
    
    private SwingGui gui;
    private JTabbedPane tabs;
    private QuaternionProperties quatProps;
    private EulerProperties eulerProps;
    private AngleAxisProperties angleAxisProps;
    
    public QuaternionPanel( SwingGui gui, Property prop ) {
        super(prop);
        this.gui = gui;
        
        // We may want to use tabs or we may want to use something
        // else... so we'll let the parent class think it's its own
        // view and we'll do what we want internally.  Parent will
        // manage our property listeners and stuff for us.
        
        tabs = new JTabbedPane();
        tabs.addTab("Euler", createEulerPanel());
        tabs.addTab("Angle/Axis", createAngleAxisPanel());
        tabs.addTab("Quaternion", createQuaternionPanel());
        
        setView(tabs);
    }
    
    protected String getId() {
        return getProperty().getId();
    }

    protected void updateView( Component view, Object value ) {
        Quaternion q = (Quaternion)value;
        quatProps.updateValue(q);
        eulerProps.updateValue(q);
        angleAxisProps.updateValue(q);
    }
  
    protected JComponent createEulerPanel() {    
        eulerProps = new EulerProperties(getProperty());        
        PropertyEditorPanel panel = new PropertyEditorPanel(gui); 
        panel.setObject(eulerProps);        
        return panel;              
    }

    protected JComponent createAngleAxisPanel() {
        angleAxisProps = new AngleAxisProperties(getProperty());        
        PropertyEditorPanel panel = new PropertyEditorPanel(gui); 
        panel.setObject(angleAxisProps);        
        return panel;              
    }
    
    protected JComponent createQuaternionPanel() {    
        quatProps = new QuaternionProperties(getProperty());
        PropertyEditorPanel panel = new PropertyEditorPanel(gui); 
        panel.setObject(quatProps);
        return panel;              
    }

    private class QuaternionProperties extends AbstractPropertySet {
        private boolean updating = false;
        
        public QuaternionProperties( Property parent ) {
            super(parent, new Quaternion(),
                  new DefaultProperty("x", Float.class, 0f),
                  new DefaultProperty("y", Float.class, 0f),
                  new DefaultProperty("z", Float.class, 0f),
                  new DefaultProperty("w", Float.class, 1f));
        }
    
        public void updateValue( Quaternion quat ) {
            if( log.isTraceEnabled() ) {
                log.trace(getId() + ".QuaternionProperties: updateValue(" + quat + ")");
            }
            
            updating = true;
            try {           
                // Just reset them all... it's easier
                getProperty("x").setValue(quat.getX());
                getProperty("y").setValue(quat.getY());
                getProperty("z").setValue(quat.getZ());
                getProperty("w").setValue(quat.getW());
            } finally {
                updating = false;
            }
        }
        
        @Override
        protected void propertyChange( PropertyChangeEvent e ) {
            if( updating ) {
                // This is a property change because of our own changes
                return;
            }        
            if( log.isTraceEnabled() ) {
                log.trace(getId() + ".QuaternionProperties: propertyChange(" + e + ")");
            }
            // Just reset them all... it's easier
            float x = (Float)getProperty("x").getValue();    
            float y = (Float)getProperty("y").getValue();    
            float z = (Float)getProperty("z").getValue();    
            float w = (Float)getProperty("w").getValue();    
            Quaternion rotation = (Quaternion)getObject(); 
            rotation.set(x, y, z, w);
            updateProperty(rotation);              
        }        
    } 
 
    private class EulerProperties extends AbstractPropertySet {
 
        private float[] angles = new float[3];
        private boolean updating = false;
        
        public EulerProperties( Property parent ) {
            super(parent, new Quaternion(),
                  new DefaultProperty("yaw", new NumberRangeType(null, null, 0.01f), 0f),
                  new DefaultProperty("pitch", new NumberRangeType(-FastMath.HALF_PI, FastMath.HALF_PI, 0.01f), 0f),
                  new DefaultProperty("roll", new NumberRangeType(-FastMath.HALF_PI, FastMath.HALF_PI, 0.01f), 0f));
        }
 
        public void updateValue( Quaternion quat ) {
            if( log.isTraceEnabled() ) {
                log.trace(getId() + ".EulerProperties: updateValue(" + quat + ")");
            }
            
            angles = quat.toAngles(angles);            
 
            updating = true;
            try {           
                // Just reset them all... it's easier
                getProperty("pitch").setValue(angles[0]);
                getProperty("yaw").setValue(angles[1]);
                getProperty("roll").setValue(angles[2]);
            } finally {
                updating = false;
            }
        }
        
        @Override
        protected void propertyChange( PropertyChangeEvent e ) {
            if( updating ) {
                // This is a property change because of our own changes
                return;
            }        
            if( log.isTraceEnabled() ) {
                log.trace(getId() + ".EulerProperties: propertyChange(" + e + ")");
            }
            // Just reset them all... it's easier
            angles[0] = (Float)getProperty("pitch").getValue();    
            angles[1] = (Float)getProperty("yaw").getValue();   
            angles[2] = (Float)getProperty("roll").getValue();
            Quaternion rotation = (Quaternion)getObject(); 
            rotation.fromAngles(angles);
            updateProperty(rotation);              
        }        
    }
    
    private class AngleAxisProperties extends AbstractPropertySet {
 
        private Vector3f axis = new Vector3f(0, 1, 0);
        private Vector3f tempAxis = new Vector3f();
        private Vector3f normalizedAxis = new Vector3f();
        private float angle = 0; 
        private boolean updating = false;
        
        public AngleAxisProperties( Property parent ) {
            super(parent, new Quaternion(),
                  new DefaultProperty("angle", new NumberRangeType(-FastMath.PI, FastMath.PI, 0.01f), 0f),
                  new DefaultProperty("x", Float.class, 0f),
                  new DefaultProperty("y", Float.class, 1f),
                  new DefaultProperty("z", Float.class, 0f));
        }
 
        public void updateValue( Quaternion quat ) {
        
            if( log.isTraceEnabled() ) {
                log.trace(getId() + ".AngleAxisProperties: updateValue(" + quat + ")");
            }
            
            updating = true;
            try {                       
                angle = quat.toAngleAxis(tempAxis);
 
                // See if our axis is reasonable consistent with the
                // quaternions... if so then we won't update.
                float dist = tempAxis.distanceSquared(normalizedAxis);
                if( dist > 0.01f ) {
                    // If the tips are closer than 0.01 away then we'll consider
                    // the current axis to just be a scaled version of the new 
                    // normalized axis.  Could be that's not close enough... but whatever.
                    axis.set(tempAxis);               
                }                
 
                // Just reset them all... it's easier
                getProperty("x").setValue(axis.x);
                getProperty("y").setValue(axis.y);
                getProperty("z").setValue(axis.z);
                getProperty("angle").setValue(angle);
            } finally {
                updating = false;
            }
        }
        
        @Override
        protected void propertyChange( PropertyChangeEvent e ) {
            if( updating ) {
                // This is a property change because of our own changes in
                // updateValue
                return;
            }
                    
            if( log.isTraceEnabled() ) {
                log.trace(getId() + ".AngleAxisProperties: propertyChange(" + e + ")");
            }
            
            // Just reset them all... it's easier
            axis.x = (Float)getProperty("x").getValue();    
            axis.y = (Float)getProperty("y").getValue();    
            axis.z = (Float)getProperty("z").getValue();
            normalizedAxis.set(axis);
            normalizedAxis.normalizeLocal();
            angle = (Float)getProperty("angle").getValue();   
            Quaternion rotation = (Quaternion)getObject(); 
            rotation.fromAngleAxis(angle, axis);
            updateProperty(rotation);              
        }        
    }
}
