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
import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import spix.core.RequestCallback;

/**
 *
 *
 *  @author    Paul Speed
 */
public class ColorChooser {

    private static JColorChooser colorChooser;
    private static JDialog colorDialog;
    private static ColorObserver colorObserver;

    public static Color getColor( Component owner, String title, Color defaultColor,
                                  RequestCallback<Color> interactiveListener ) {

        // For some reason, reusing the dialog doesn't reset the new initial
        // color in the selectors.  So we won't reuse it for now... and wait to see
        // if we have issues.
        if( colorDialog != null ) {
            // At least dispose the last one
            colorDialog.dispose();
        }
//        if( colorDialog == null ) {
            colorChooser = new JColorChooser(defaultColor == null ? Color.white : defaultColor);
            colorObserver = new ColorObserver();
            colorChooser.getSelectionModel().addChangeListener(colorObserver);
            colorDialog = JColorChooser.createDialog(owner, title, true, colorChooser,
                                                     colorObserver, colorObserver);
/*        } else {
            colorDialog.setTitle(title);
            if( defaultColor != null ) {
                colorChooser.setColor(defaultColor);
            } // else whatever the last color was
        }*/

        colorObserver.setup(defaultColor, interactiveListener);
        colorDialog.show();

        return colorObserver.getColor();
    }

    /**
     *  Keeps track of the current color selection and reverts to a 'canceled'
     *  state if the user clicks cancel.  If there is an interactive listener then
     *  all color selection changes will be forwarded to it.  If the user cancels
     *  then the initial color is sent to the listener.
     *  Otherwise, for a null interactive listener, a cancel always sets the color
     *  to null so that the caller knows the chooser was canceled.
     */
    private static class ColorObserver implements ActionListener, ChangeListener {
        private Color color;
        private Color initialColor;
        private RequestCallback<Color> interactiveListener;

        public void setup( Color initialColor, RequestCallback<Color> interactiveListener ) {
            this.initialColor = initialColor;
            this.interactiveListener = interactiveListener;
        }

        public Color getColor() {
            return color;
        }

        public void actionPerformed( ActionEvent event ) {
            if( "cancel".equals(event.getActionCommand()) ) {
                if( interactiveListener != null ) {
                    color = initialColor;
                    interactiveListener.done(color);
                    interactiveListener = null;
                } else {
                    color = null;
                }
            } else {
                // Still clear the listener and stuff
                interactiveListener = null;
            }
        }

        public void stateChanged( ChangeEvent e ) {
            this.color = colorChooser.getColor();
            if( interactiveListener != null ) {
                interactiveListener.done(color);
            }
        }
    }
}


