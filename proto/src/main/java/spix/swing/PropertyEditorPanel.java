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

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spix.form.*;
import spix.props.PropertySet;
import spix.props.PropertySetWrapper;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;

/**
 *  A UI that presents property value editors for a particular
 *  object.
 *
 *  @author    Paul Speed
 */
public class PropertyEditorPanel extends JPanel {

    static Logger log = LoggerFactory.getLogger(PropertyEditorPanel.class);

    private SwingGui gui;
    private PropertySet properties;
    private Form form;
    private String context;
    private boolean nested;
    private boolean isPeered;
    private Runnable onSetup;

    public PropertyEditorPanel( SwingGui gui ) {
        this(gui, null);
    }
    
    public PropertyEditorPanel( SwingGui gui, String context ) {
        this(gui, context, false);
    }
    
    public PropertyEditorPanel( SwingGui gui, String context, boolean nested ) {
        super(new GridBagLayout());
        this.gui = gui;
        this.context = context;
        this.nested = nested;
    }

    public void setOnSetupAction(Runnable runnable) {
        this.onSetup = runnable;
    }

    public void setObject( PropertySet properties ) {
        setObject(properties, null);
    }
    
    public void setObject( PropertySet properties, Form form ) {
System.out.println("PropertyEditorPanel.setObject(" + properties + ", " + form + ")");

        if( this.properties == properties ) {        
            return;
        }

        // Clear out any old setup
        if( this.properties != null ) {
        
            if( isPeered ) {
                // Make to release any wrappers
                detach();
            }        
        
            // for now we'll just clear the properties but there will
            // be more work to do when there is a real editor
            this.properties = null;
            this.form = null;
        }

        if( properties == null ) {
            return;
        }
        
        this.properties = gui.wrap(properties);        
        //this.properties = properties;        

        if( isPeered ) {
            // If we are already peered then make sure to attach
            // any wrapper to its delegate.
            attach();
        }        
 
        if( form == null ) {       
            this.form = gui.getSpix().createForm(this.properties, context);
        } else {
            this.form = form;
        }
System.out.println(this.form.debugString());
        
        gui.runOnSwing(new Runnable() {
                public void run() {
                    setupComponents();
                    if (onSetup != null) {
                        onSetup.run();
                    }
                    //This is a weird hack in order to have the GridBagLayout resize when its inner components are higher than its parent conatiner...
                    //This is the only way I managed to have it to play well within a scrollPane...
                    if (getParent() != null) {
                        Component[] components = getComponents();
                        int height = 0;
                        for (Component component : components) {
                            height += component.getPreferredSize().getHeight();
                        }
                        setPreferredSize(new Dimension(getParent().getWidth() - 25, height));
                    }
                }
            });
    }
    
    protected void setupComponents() {
        // Clear anything already in the UI
        removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.weightx = 0;
        Insets main = new Insets(0, 2, 0, 2);
        Insets multi = new Insets(0, 0, 0, 0);

        boolean lastTwoColumn = false;
        
        // Now each field
        for( Field field : form ) {
            if( field instanceof PropertyField ) {
                PropertyField pf = (PropertyField)field;

                Component view = gui.createComponent(SwingGui.EDIT_CONTEXT, pf.getProperty());
                if( view instanceof MulticolumnComponent ) {
                    if( view instanceof JComponent ) {
                        JComponent c = (JComponent)view;
                        if( c.getBorder() == null ) {
                            c.setBorder(BorderFactory.createTitledBorder(pf.getName()));
                        } 
                    }
 
                    if( lastTwoColumn ) {                   
                        gbc.insets = new Insets(5, 0, 0, 0);
                    } else {
                        gbc.insets = multi;
                    }
                    gbc.gridwidth = 2;
                    gbc.gridx = 0;
                    gbc.weightx = 1;
                    gbc.ipady = 10;
                    gbc.anchor = GridBagConstraints.WEST;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    add(view, gbc);
                    
                    lastTwoColumn = false;
                } else {
                    gbc.insets = main;

                    // It follows the normal name: value layout                 
                    JLabel label = new JLabel(pf.getName() + ":");
                    label.setPreferredSize(new Dimension(50, 16));
                    label.setMaximumSize(new Dimension(50, 16));
                    label.setToolTipText(pf.getName());
                    gbc.gridwidth = 1;
                    gbc.gridx = 0;
                    gbc.weightx = 0.3;
                    gbc.anchor = GridBagConstraints.EAST;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    add(label, gbc);
                
                    gbc.gridx++;
                    gbc.weightx = 1;
                    gbc.anchor = GridBagConstraints.WEST;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    add(view, gbc);
                    
                    lastTwoColumn = true;
                }
            } else if( field instanceof FormField ) {
                FormField ff = (FormField)field;
                PropertyEditorPanel subpanel = new PropertyEditorPanel(gui, context, true);
                subpanel.setObject(properties, ff.getForm());
                 
                //subpanel.setBorder(BorderFactory.createTitledBorder(ff.getName()));
 
                if( lastTwoColumn ) {                   
                    gbc.insets = new Insets(5, 0, 0, 0);
                } else {
                    gbc.insets = multi;
                }
                gbc.gridwidth = 2;
                gbc.gridx = 0;
                gbc.weightx = 1;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                //add(subpanel, gbc);

                RollupPanel rollup = new RollupPanel(ff.getName(), subpanel, ff.getIconPath() == null ? Icons.attrib : new ImageIcon(this.getClass().getResource(ff.getIconPath())));
                add(rollup, gbc);
                    
                lastTwoColumn = false;
            } else {
                throw new UnsupportedOperationException("Field type not supported:" + field);
            }
            
            gbc.gridy++;            
        }
        
        if( !nested ) {
            // Add a spacer at the end that can take up the slack
            gbc.gridx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            add(new JLabel(), gbc);
        }
        
        revalidate();
        repaint();
    }
    
    public PropertySet getObject() {
        return properties;
    }

    protected void attach() {
        if( properties instanceof PropertySetWrapper ) {
            ((PropertySetWrapper)properties).attach();
        }
    }

    protected void detach() {
        if( properties instanceof PropertySetWrapper ) {
            ((PropertySetWrapper)properties).release();
        }
    }

    public void addNotify() {
        log.trace("Editor panel addNotify():" + this);        
        super.addNotify();
        attach();
        isPeered = true;
    }
    
    public void removeNotify() {
        log.trace("Editor panel removeNotify():" + this);    
        isPeered = false;
        detach();
        super.removeNotify();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("properties", properties)
                .add("form", form)
                .toString();
    }
}
