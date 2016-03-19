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

package spix.app;

import com.jme3.app.*;
import com.jme3.app.state.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

/**
 *  A state that manages a viewport that can be used for scene annotations
 *  like selector widgets, grid backgrounds, etc. that don't affect the
 *  regular scene. 
 *
 *  @author    Paul Speed
 */
public class DecoratorViewPortState extends BaseAppState {

    private ViewPort viewport;
    private Node root = new Node("root");
    
    public DecoratorViewPortState() {
    }
    
    public Node getRoot() {
        return root;
    }
    
    @Override   
    protected void initialize( Application app ) {
        viewport = app.getRenderManager().createMainView("decorator", app.getCamera());
        viewport.setEnabled(false);
        
        viewport.attachScene(root);
    }
    
    @Override   
    protected void cleanup( Application app ) {
        app.getRenderManager().removeMainView(viewport);
    }
    
    @Override   
    protected void onEnable() {
        viewport.setEnabled(true);
    }
    
    @Override   
    protected void onDisable() {
        viewport.setEnabled(false);
    }
    
    @Override   
    public void update( float tpf ) {
        root.updateLogicalState(tpf);
    }
 
    @Override   
    public void render( RenderManager renderManager ) {
        root.updateGeometricState();
    } 
}
