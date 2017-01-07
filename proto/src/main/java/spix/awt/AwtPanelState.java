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

package spix.awt;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.system.awt.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 *
 *
 *  @author    Paul Speed
 */
public class AwtPanelState extends BaseAppState {
 
    // Other than initial setup these fields are _only_ accessed
    // from the swing thread except Viewport attachment
    private final Container container;
    private final Object constraints;
    private boolean gammaCorrection;
    private volatile AwtPanel panel;

    private List<Runnable> enabledCommands = new CopyOnWriteArrayList<>();

    public AwtPanelState(Container container, Object constraints, boolean gammaCorrection) {
        this.container = container;
        this.constraints = constraints;
        this.gammaCorrection = gammaCorrection;
    }
 
    public void addEnabledCommand( Runnable cmd ) {
        enabledCommands.add(cmd);
    }
    
    protected void initialize( Application app ) {
        try {        
            SwingUtilities.invokeAndWait(new InitializeCommand());
        } catch( InterruptedException | InvocationTargetException e ) {
            throw new RuntimeException("Error creating panel on swing thread", e);
        }

        // Can't unattach them so we might as well do it on init
        panel.attachTo(true, app.getViewPort(), app.getGuiViewPort());        
    }
    
    protected void onEnable() {
        SwingUtilities.invokeLater(new AttachPanelCommand());
    }
    
    protected void onDisable() {
        SwingUtilities.invokeLater(new DetachPanelCommand());
    }
    
    protected void cleanup( Application app ) {
    }
 
    private class InitializeCommand implements Runnable {
        public void run() {
 
            AwtPanelsContext ctx = (AwtPanelsContext)getApplication().getContext();
            panel = ctx.createPanel(PaintMode.Accelerated, AwtPanelState.this.gammaCorrection);
            panel.setPreferredSize(new Dimension(1280, 720));
            panel.setMinimumSize(new Dimension(400, 300));
            panel.setBackground(Color.black);
            ctx.setInputSource(panel);
        }
    }
 
    private class AttachPanelCommand implements Runnable {
        public void run() {
            // Add it to the container provided
            container.add(panel, constraints);
            
            for( Runnable r : enabledCommands ) {
                r.run();
            }
        }
    }
    
    private class DetachPanelCommand implements Runnable {
        public void run() {
            container.remove(panel);
        }
    }
}
