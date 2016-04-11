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

import spix.props.*;

/**
 *  PropertySetWrapper specialization that handles the swing vs 
 *  render thread transitions.
 *
 *  @author    Paul Speed
 */
public class SwingPropertySetWrapper extends PropertySetWrapper {
 
    private SwingGui gui;
    private String jmeUpdatingProperty = null;
    private String swingUpdatingProperty = null;
    
    public SwingPropertySetWrapper( SwingGui gui, PropertySet delegate ) {
        super(delegate);
        this.gui = gui;
    }
 
    protected void updateProperty( final Property wrapper, final Property original, final Object oldValue, final Object newValue ) {
System.out.println(wrapper.getId() + ":$$$ updateProperty(" + wrapper.getId() + ", " + oldValue + ", " + newValue + ")  " + Thread.currentThread());

        // Update property should generally always be called from the
        // AWT thread.
        
        if( wrapper.getId().equals(swingUpdatingProperty) ) {
System.out.println(wrapper.getId() + ":  $ ignoring swing feedback event.");         
            return;
        } 

        // Need to shunt this over to the JME thread
        gui.runOnRender(new Runnable() {
            public void run() {
System.out.println(wrapper.getId() + ": >>>>>   calling super updateProperty(" + wrapper.getId() + ", " + oldValue + ", " + newValue + ")");
                
                // We are on the JME thread.  updateWrapper() will also be called
                // on the JME thread... but if it's called while we are still in super.updateProperty()
                // then we want to ignore the feedback as it's redundant as far as the
                // original caller is concerned.  Meanwhile the JME side could be sending back
                // all kinds of other events that we don't want to ignore.  So we'll gate on the 
                // specific property
                jmeUpdatingProperty = wrapper.getId();
                try {             
                    SwingPropertySetWrapper.super.updateProperty(wrapper, original, oldValue, newValue);
                } finally {
                    jmeUpdatingProperty = null;
                }
System.out.println(wrapper.getId() + ": <<<<<   done calling super updateProperty(" + wrapper.getId() + ", " + oldValue + ", " + newValue + ")");            
            }
            
            public String toString() {
                return "JmeRunner[" + wrapper.getId() + "]";
            }
        });
    }
 
    protected void updateWrapper( final String id, final Object value ) {
System.out.println(id + ":$$$ updateWrapper(" + id + ", " + value + ")  " + Thread.currentThread());

        // Update wrapper should generally always be called from the
        // the render thread.
        // If the render thread has already marked this particular property
        // as being updated then that means we are still inside an updateProperty() 
        // call and this event is unneeded feedback.
        if( id.equals(jmeUpdatingProperty) ) {
System.out.println(id + ":  $ ignoring JME feedback event.");         
            return;
        } 

        gui.runOnSwing(new Runnable() {
            public void run() {
System.out.println(id + ":     >>>>> calling super.updateWrapper(" + id + ", " + value + ")");            
                swingUpdatingProperty = id;
                try {
                    SwingPropertySetWrapper.super.updateWrapper(id, value);
                } finally {
                    swingUpdatingProperty = null;
                }
System.out.println(id + ":     <<<<< DONE calling super.updateWrapper(" + id + ", " + value + ")");            
            }
            
            public String toString() {
                return "SwingRunner[" + id + "]";
            }
        });
        
System.out.println(id + ":    exiting: updateWrapper(" + id + ", " + value + ")  " + Thread.currentThread());        
    }

}

