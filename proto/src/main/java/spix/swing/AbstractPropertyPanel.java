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
import java.beans.PropertyChangeListener;
import javax.swing.*;

import spix.props.*;

/**
 *  Base class for property panel views that does some of the
 *  listener management work.
 *
 *  @author    Paul Speed
 */
public abstract class AbstractPropertyPanel<V extends Component> extends JPanel {
    
    private Property prop;
    private ChangeObserver changeObserver = new ChangeObserver();
    private boolean listenerAdded = false; // important enough to have a safety net
    private V view; 
    
    protected AbstractPropertyPanel( Property prop ) {
        super(new BorderLayout());
        this.prop = prop;
    }

    protected void setView( V view ) {
        if( this.view != null ) {
            remove(view);
        } 
        this.view = view;
        if( this.view != null ) {
            add(view, BorderLayout.CENTER);
            updateView(view, prop.getValue()); 
        }
    }
 
    protected Property getProperty() {
        return prop;
    }
 
    protected V getView() {
        return view;
    }
 
    protected abstract void updateView( V view, Object value );
    
    protected void updateProperty( Object value ) {
        prop.setValue(value);
    }

    @Override
    public void addNotify() {
        if( !listenerAdded ) {
            prop.addPropertyChangeListener(changeObserver);
            listenerAdded = true;
        }
        updateView(view, prop.getValue());
        super.addNotify();
    }
    
    @Override
    public void removeNotify() {
        super.removeNotify();
        if( listenerAdded ) {
            prop.removePropertyChangeListener(changeObserver);
            listenerAdded = false;
        }
    }    
 
    protected void propertyChange( PropertyChangeEvent event ) {
        updateView(view, prop.getValue());
    } 
    
    private class ChangeObserver implements PropertyChangeListener {
        public void propertyChange( final PropertyChangeEvent event ) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {           
                    AbstractPropertyPanel.this.propertyChange(event);
                }
            });
        }
    }    
}
