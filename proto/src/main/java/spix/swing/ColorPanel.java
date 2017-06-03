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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.*;

import com.google.common.base.*;

import com.jme3.math.ColorRGBA;

import spix.core.ColorRgbaStringFunction;
import spix.core.RequestCallback;
import spix.props.Property;
import spix.ui.ColorRequester;

/**
 *  A panel for editing color values.
 *
 *  @author    Paul Speed
 */
public class ColorPanel extends AbstractPropertyPanel<JButton> {

    private SwingGui gui;
    private Function<? super ColorRGBA, String> toString;
    private ColorCallback colorCallback = new ColorCallback(); 

    public ColorPanel( SwingGui gui, Property prop ) {
        this(gui, prop, new ColorRgbaStringFunction());
    }
    
    public ColorPanel( SwingGui gui, Property prop, Function<? super ColorRGBA, String> toString ) {
        super(prop);
        this.gui = gui;
        this.toString = toString;
        setView(new JButton());
        getView().addActionListener(new ButtonListener());                
    }
    
    protected void updateView( JButton button, Object value ) {
        ColorRGBA jmeColor = (ColorRGBA) value;
        if (jmeColor == null) {
            jmeColor = ColorRGBA.Black;
        }
        button.setText(toString.apply(jmeColor));
        Color c = SwingColorRequester.toSwing(jmeColor);
        button.setBackground(c);
        c = new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue());
        button.setForeground(c);
    }
 
    private class ColorCallback implements RequestCallback<ColorRGBA> {
        public void done( ColorRGBA color ) {
            updateProperty(color);
        }        
    } 
    
    private class ButtonListener implements ActionListener {
        public void actionPerformed( ActionEvent event ) {
            ColorRGBA jmeColor = (ColorRGBA)getProperty().getValue();
            gui.getService(ColorRequester.class).requestColor(getProperty().getName(), 
                                                              jmeColor.clone(), true, colorCallback);
        }
    } 
}
