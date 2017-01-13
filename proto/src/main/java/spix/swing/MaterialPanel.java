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
import com.jme3.shader.VarType;
import spix.app.material.MatParamProperty;
import spix.props.*;
import spix.type.Type;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A boolean Property view that uses a JCheckBox
 *
 * @author Rémy Bouquet
 */
public class MaterialPanel extends AbstractPropertyPanel<Component> implements MulticolumnComponent {

    private static final Logger log = Logger.getLogger(MaterialPanel.class.getName());

    private SwingGui gui;
    private JPanel panel;

    public MaterialPanel(SwingGui gui, Property prop) {
        super(prop);
        this.gui = gui;
        Material material = (Material) prop.getValue();
        java.util.List<Property> props = new ArrayList<>();
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder());
        setView(panel);

        MatParamChangeListener matParamChangeListener = new MatParamChangeListener(prop);
        ClassicChangeListener classicChangeListener = new ClassicChangeListener(prop);

        props.add(BeanProperty.create(material, "name"));
        props.add(new MatDefProperty(material));
        props.add(BeanProperty.create(material, "key", "J3m file", false, null));

        for (Property property : props) {
            property.addPropertyChangeListener(classicChangeListener);
        }

        createPanel(props, material, null);

        ArrayList params = gui.getSpix().getBlackboard().get("material.metadata." + material.getMaterialDef().getAssetName(), ArrayList.class);


        if (params == null) {
            //no meta data config, let's try to sort things up a bit.
            //let's sort by type, and by names then make a group for each type.
            handleNoConfig(material, matParamChangeListener);

        } else {
            try {
                handleConfig(params, material, matParamChangeListener, null);
            } catch (Exception e) {
                //catch anything that could go wrong (this might be a user input at some point) and fall back to the noConfig layout.
                log.log(Level.SEVERE, "Failed to setup property layout for material definition " + material.getMaterialDef().getName());
                e.printStackTrace();
                handleNoConfig(material, matParamChangeListener);
            }
        }

    }

    public void handleNoConfig(Material material, PropertyChangeListener matParamChangeListener) {
        List<Property> props = new ArrayList<>();
        List<MatParam> matParams = new ArrayList<>();
        matParams.addAll(material.getMaterialDef().getMaterialParams());
        matParams.sort(new Comparator<MatParam>() {
            @Override
            public int compare(MatParam o1, MatParam o2) {
                String name1 = o1.getVarType().name();
                String name2 = o2.getVarType().name();
                //cheating
                if (name1.equals("Vector4")) {
                    name1 = "Color";
                }
                if (name2.equals("Vector4")) {
                    name2 = "Color";
                }

                int result = name1.compareTo(name2);
                if (result == 0) {
                    return o1.getName().compareTo(o2.getName());
                }
                return result;
            }
        });

        VarType curType = null;
        String groupName = "";
        for (MatParam matParam : matParams) {
            MatParamProperty mp = new MatParamProperty(matParam.getName(), matParam.getVarType(), material);
            if (mp.getType().getJavaType() != Object.class) {
                if (curType == null) {
                    curType = matParam.getVarType();
                    groupName = curType.name();
                    //Cheating again
                    if (curType.name().equals("Vector4")) {
                        groupName = "Color";
                    }
                }
                if (matParam.getVarType() != curType) {
                    createPanel(props, material, groupName);
                    curType = null;
                    props.clear();
                } else {
                    mp.addPropertyChangeListener(matParamChangeListener);
                    props.add(mp);
                }
            }
        }
        if (!props.isEmpty()) {
            createPanel(props, material, curType.name());
        }
    }

    private void handleConfig(ArrayList list, Material material, PropertyChangeListener matParamChangeListener, String name) {
        java.util.List<Property> props = null;
        for (Object entry : list) {
            String field;
            if (entry instanceof String) {
                if (props == null) {
                    props = new ArrayList<>();
                }
                field = (String) entry;
                MatParam matParam = material.getMaterialDef().getMaterialParam(field);
                if (matParam != null) {
                    MatParamProperty mp = new MatParamProperty(matParam.getName(), matParam.getVarType(), material);
                    mp.addPropertyChangeListener(matParamChangeListener);
                    props.add(mp);
                } else {
                    log.log(Level.WARNING, "Material parameter {0} doesn't exist for material definition {1}", new Object[]{
                            field, material.getMaterialDef().getAssetName()
                    });
                }
            } else {

                if (entry instanceof Map) {
                    //we encounter a named group let's layout existing props
                    if (props != null) {
                        createPanel(props, material, name);
                    }
                    props = null;
                    name = null;

                    Map group = (Map) entry;
                    for (Object o : group.keySet()) {
                        Object obj = group.get(o);
                        if (obj instanceof ArrayList) {
                            handleConfig((ArrayList) obj, material, matParamChangeListener, o.toString());
                        }
                    }
                }
            }
        }
        if (props != null) {
            createPanel(props, material, name);
        }
    }

    public void createPanel(java.util.List<Property> props, Material material, String name) {
        PropertyEditorPanel pePanel = new PropertyEditorPanel(gui, SwingGui.EDIT_CONTEXT);
        pePanel.setObject(new DefaultPropertySet(material, props));
        panel.add(pePanel);
        if (name != null) {
            pePanel.setBorder(BorderFactory.createTitledBorder(name));
        } else {
            pePanel.setBorder(BorderFactory.createEmptyBorder());
        }
    }


    protected void updateView(Component panel, Object value) {
        //nothing to do... for now... I guess when the material can be changed elsewhere it's gonna be important.
    }

    private class MatParamChangeListener implements PropertyChangeListener {

        Property prop;

        public MatParamChangeListener(Property prop) {
            this.prop = prop;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            //System.err.println(evt.getNewValue());
            MatParamProperty mpp = (MatParamProperty) evt.getSource();
            prop.setValue(mpp.getMaterial());
        }
    }

    private class ClassicChangeListener implements PropertyChangeListener {
        Property prop;

        public ClassicChangeListener(Property prop) {
            this.prop = prop;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
           // System.err.println(evt.getNewValue());
            if (evt.getSource() instanceof BeanProperty) {
                BeanProperty bp = (BeanProperty) evt.getSource();
                prop.setValue(bp.getObject());
            }
        }
    }

    //this will change later
    private class MatDefProperty extends AbstractProperty {

        Material material;

        protected MatDefProperty(Material material) {
            super("MaterialDefinition");
            this.material = material;
        }

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
    }

}
