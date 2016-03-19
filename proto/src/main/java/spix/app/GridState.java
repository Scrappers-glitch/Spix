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
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.*;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.*;
import com.simsilica.lemur.input.*;

/**
 *  A standard app state for displaying a local-relative x,z grid at y=0.
 *
 *  @author    Paul Speed
 */
public class GridState extends BaseAppState {
 
    private Node grid;
    private Geometry grid1;
    private Geometry grid2;
 
    public GridState() {
    }
    
    @Override   
    protected void initialize( Application app ) {
 
        this.grid = new Node("grid");
 
        GuiGlobals globals = GuiGlobals.getInstance();       
        {
            Grid mesh = new Grid(11, 11, 1);
            this.grid1 = new Geometry("grid-lines", mesh);
            Material mat = globals.createMaterial(new ColorRGBA(0.5f, 0.5f, 0.5f, 1), false).getMaterial();
            grid1.setMaterial(mat);
            grid1.setLocalTranslation(-5, 0, -5);
            grid.attachChild(grid1);
        }
        
        { 
            Quad mesh = new Quad(10, 10);
            mesh.scaleTextureCoordinates(new Vector2f(10, 10));
            Texture gridTexture = globals.loadTexture("Interface/grid-cell.png", true, false);
            Material mat = globals.createMaterial(gridTexture, false).getMaterial();
            this.grid2 = new Geometry("grid-quads", mesh);
            mat.setColor("Color", new ColorRGBA(0.5f, 0.45f, 0.45f, 0.25f));
            mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            grid2.setMaterial(mat);
            grid2.setQueueBucket(Bucket.Transparent);
        
            grid2.rotate(-FastMath.HALF_PI, 0, 0);
            grid2.setLocalTranslation(-5, -0.001f, 5);
            grid.attachChild(grid2);
        }
    }
        
    @Override   
    protected void cleanup( Application app ) {
    }
    
    @Override   
    protected void onEnable() {
        Node root = ((SimpleApplication)getApplication()).getRootNode();
        root.attachChild(grid);
    }
    
    @Override   
    protected void onDisable() {
        grid.removeFromParent();
    }
 
    @Override
    public void update( float tpf ) {
    }

}
