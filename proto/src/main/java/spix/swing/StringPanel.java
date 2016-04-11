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

import org.slf4j.*;

import com.google.common.base.*;

import spix.core.ToStringFunction;
import spix.props.Property;
import spix.type.Type;

/**
 *  A panel for editing String values or values that can be converted
 *  to/from strings with a pair of Guava functions.
 *
 *  @author    Paul Speed
 */
public class StringPanel extends AbstractPropertyPanel<JTextField> {

    static Logger log = LoggerFactory.getLogger(StringPanel.class);

    private Function<Object, String> toString;    
    private Function<String, Object> fromString;    

    public StringPanel( Property prop ) {
        this(prop, ToStringFunction.INSTANCE, null);
    }

    public StringPanel( Property prop, 
                        Function<Object, String> toString, 
                        Function<String, Object> fromString ) {
        super(prop);
        this.toString = toString;
        this.fromString = fromString;
 
        setView(new JTextField());
        getView().getKeymap().addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 
                                                    new CancelAction());

        TextFieldObserver observer = new TextFieldObserver();
        getView().addActionListener(observer);
        getView().addFocusListener(observer);
    }
    
    protected void updateView( JTextField view, Object value ) {
        if( log.isTraceEnabled() ) {
            log.trace(getProperty().getId() + ": updateView(" + value + ")");
        }
        view.setText(toString.apply(value));
    }
    
    protected void resetValue() {
        if( log.isTraceEnabled() ) {
            log.trace(getProperty().getId() + ": resetValue()");
        }
        updateView(getView(), getProperty().getValue());
    }

    protected void commitValue() {
        String value = getView().getText();
        if( log.isTraceEnabled() ) {
            log.trace(getProperty().getId() + ": commitValue() =" + value);
        }
        if( fromString != null ) {
            updateProperty(fromString.apply(value));
        } else {
            updateProperty(value);
        }
    }

    private class TextFieldObserver extends FocusAdapter implements ActionListener {
        public void actionPerformed( ActionEvent event ) {
            commitValue();
        }
        
        public void focusLost( FocusEvent e ) {
            commitValue();
        }
    }
    
    private class CancelAction extends AbstractAction {
        public void actionPerformed( ActionEvent event ) {
            resetValue();
        }
    }    
}
