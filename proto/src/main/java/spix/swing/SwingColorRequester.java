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
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import com.jme3.math.ColorRGBA;

import spix.core.*;
import spix.ui.*;

/**
 *
 *
 *  @author    Paul Speed
 */
public class SwingColorRequester implements ColorRequester {

    private SwingGui swingGui;

    public SwingColorRequester( SwingGui swingGui ) {
        this.swingGui = swingGui;
    }

    private static float clamp( float f ) {
        if( f < 0 ) {
            return 0;
        }
        if( f > 1 ) {
            return 1;
        }
        return f;        
    }

    public static Color toSwing( ColorRGBA color ) {
        if( color == null ) {
            return null;
        }
        return new Color(clamp(color.r), clamp(color.g), clamp(color.b), clamp(color.a));
    }

    public static ColorRGBA fromSwing( Color color ) {
        if( color == null ) {
            return null;
        }
        float[] comps = color.getComponents(null);
        return new ColorRGBA(comps[0], comps[1], comps[2], comps[3]);
    }

    @Override
    public void requestColor( final String title, final ColorRGBA initialColor, final boolean interactive,
                              final RequestCallback<ColorRGBA> callback ) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Color defaultColor = toSwing(initialColor);
                
                if( interactive ) {                
                    ColorChooser.getColor(swingGui.getRootWindow(), title, defaultColor, 
                                          new CallbackAdapter(callback));

                    // No need to forward a final color because all color changes were already sent
                } else {
                    Color color = ColorChooser.getColor(swingGui.getRootWindow(), title, defaultColor, null);
                    if( color != null ) {
                        swingGui.getSpix().sendResponse(callback, fromSwing(color));
                    } // else was canceled
                }
            }
        });
    }

    private class CallbackAdapter implements RequestCallback<Color> {
        private final RequestCallback<ColorRGBA> delegate;

        public CallbackAdapter( RequestCallback<ColorRGBA> delegate ) {
            this.delegate = delegate;
        }

        public void done( Color color ) {
            swingGui.getSpix().sendResponse(delegate, fromSwing(color));
        }
    }
}


