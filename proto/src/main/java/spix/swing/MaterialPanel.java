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

import com.jme3.material.*;
import spix.app.material.MatParamProperty;
import spix.props.*;
import spix.type.Type;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * A boolean Property view that uses a JCheckBox
 *
 * @author Rémy Bouquet
 */
public class MaterialPanel extends AbstractPropertyPanel<Component> {

    private static final Logger log = Logger.getLogger(MaterialPanel.class.getName());

    private PropertySet mainProperties;
    private PropertySet renderStateProperties;

    public MaterialPanel(SwingGui gui, Property prop) {
        super(prop);
        Material material = (Material) prop.getValue();
        java.util.List<Property> props = new ArrayList<>();

        PropertyChangeListener matParamChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                System.err.println(evt.getNewValue());
                MatParamProperty mpp = (MatParamProperty) evt.getSource();
                prop.setValue(mpp.getMaterial());
            }
        };
        PropertyChangeListener classicChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                System.err.println(evt.getNewValue());
                if (evt.getSource() instanceof BeanProperty) {
                    BeanProperty bp = (BeanProperty) evt.getSource();
                    prop.setValue(bp.getObject());
                }
            }
        };

        props.add(BeanProperty.create(material, "name"));
        props.add(new AbstractProperty("MaterialDefinition") {
            @Override
            public Type getType() {
                return new Type(MaterialDef.class);
            }

            @Override
            public void setValue(Object value) {

            }

            @Override
            public Object getValue() {
                return material.getMaterialDef();
            }
        });
        props.add(BeanProperty.create(material, "key", "J3m file", false, null));

        for (Property property : props) {
            property.addPropertyChangeListener(classicChangeListener);
        }

        for (MatParam matParam : material.getMaterialDef().getMaterialParams()) {
            MatParamProperty mp = new MatParamProperty(matParam.getName(), matParam.getVarType(), material);
            if (mp.getType().getJavaType() != Object.class) {
                mp.addPropertyChangeListener(matParamChangeListener);
                props.add(mp);
            }
        }

        mainProperties = new DefaultPropertySet(material, props);

        createView(gui);
    }

    public void createView(SwingGui gui) {
        JPanel panel = new JPanel(new BorderLayout());

        PropertyEditorPanel pePanel = new PropertyEditorPanel(gui, SwingGui.EDIT_CONTEXT);
        pePanel.setObject(mainProperties);
        panel.add(pePanel, BorderLayout.CENTER);
        setView(panel);
    }


    protected void updateView(Component panel, Object value) {
        // checkBox.setSelected((Boolean) value);
    }

}
