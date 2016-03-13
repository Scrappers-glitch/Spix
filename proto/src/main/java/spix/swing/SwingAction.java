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

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.SwingUtilities;

import spix.core.Action;
import spix.core.ToggleAction;
import spix.core.Spix;

/**
 *  Wraps a Spix action to make it compatible with Swing actions.
 *
 *  @author    Paul Speed
 */
public class SwingAction implements javax.swing.Action {

    private Spix spix;
    private Action action;
    private DelegateObserver observer = new DelegateObserver();
    private PropertyChangeSupport dispatcher = new PropertyChangeSupport(this);
    
    public SwingAction( Action action, Spix spix ) {
        this.action = action;
        this.spix = spix;
        
        // Action is thread safe so it's safe to add this here...
        // but we have to have a delegate because we want the events dispatched
        // on the swing thread.  Also we need to rename the properties on the
        // way out.
        this.action.addPropertyChangeListener(observer);
    }
 
    public Action getSpixAction() {
        return action;
    }
    
    public Object getValue( String key ) {
        key = toSpixProperty(key);
        return action.get(key);
    }
    
    public void setEnabled( boolean b ) {
        action.setEnabled(b);
    }
    
    public boolean isEnabled() {
        return action.isEnabled();
    }
    
    public void putValue( String key, Object value ) {
        key = toSpixProperty(key);
        value = toSpixValue(value); 
        action.put(key, value);
    }
    
    public void removePropertyChangeListener( PropertyChangeListener listener ) {
        dispatcher.removePropertyChangeListener(listener);
    }
    
    public void addPropertyChangeListener( PropertyChangeListener listener ) {
        dispatcher.addPropertyChangeListener(listener);
    }
 
    public void actionPerformed( ActionEvent e ) {
        spix.runAction(action);
        //action.performAction(spix);
    }
 
    public static String toSwingProperty( String s ) {
        switch( s ) {
            case Action.NAME:
                return NAME;
            case ToggleAction.TOGGLED:
                return SELECTED_KEY;
            default:
                return s;
        }
    }
    
    public static String toSpixProperty( String s ) {
        switch( s ) {
            case NAME:
                return Action.NAME;
            case SELECTED_KEY:
                return ToggleAction.TOGGLED;
            default:
                return s;
        }
/*
public static final String 	ACCELERATOR_KEY 	"AcceleratorKey"
public static final String 	ACTION_COMMAND_KEY 	"ActionCommandKey"
public static final String 	DEFAULT 	"Default"
public static final String 	DISPLAYED_MNEMONIC_INDEX_KEY 	"SwingDisplayedMnemonicIndexKey"
public static final String 	LARGE_ICON_KEY 	"SwingLargeIconKey"
public static final String 	LONG_DESCRIPTION 	"LongDescription"
public static final String 	MNEMONIC_KEY 	"MnemonicKey"
public static final String 	NAME 	"Name"
public static final String 	SELECTED_KEY 	"SwingSelectedKey"
public static final String 	SHORT_DESCRIPTION 	"ShortDescription"
public static final String 	SMALL_ICON 	"SmallIcon"
*/
    }
 
    public static Object toSwingValue( Object o ) {
        return o;   
    }
    
    public static Object toSpixValue( Object o ) {
        return o;
    } 
 
    protected void firePropertyChange( PropertyChangeEvent event ) {
System.out.println("SwingAction.firePropertyChange(" + event + ") on thread:" + Thread.currentThread());        
        String name = toSwingProperty(event.getPropertyName());
        dispatcher.firePropertyChange(name, event.getOldValue(), event.getNewValue());    
    } 
    
    private class DelegateObserver implements PropertyChangeListener {
        public void propertyChange( final PropertyChangeEvent event ) {
System.out.println("DelegateObserver.propertyChange(" + event + ") on thread:" + Thread.currentThread());        
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    firePropertyChange(event);
                }
            });
        }
    }    
}


