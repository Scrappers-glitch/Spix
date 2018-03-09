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

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.*;
import com.jme3.material.*;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.*;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.util.SafeArrayList;
import com.simsilica.lemur.GuiGlobals;
import spix.core.*;

import java.beans.*;
import java.util.*;


/**
 *  Displays the currently selected object as a wire-frame mesh or
 *  bounding shape.
 *
 *  @author    Paul Speed
 */
public class SelectionHighlightState extends BaseAppState {

    private String selectionProperty = DefaultConstants.SELECTION_PROPERTY;
    private String highlightColorProperty = DefaultConstants.SELECTION_HIGHLIGHT_COLOR;
    private SelectionModel selection;
    private HighlightMode highlightMode = HighlightMode.Wireframe;
    private SelectionObserver selectionObserver = new SelectionObserver();
    private SafeArrayList<SelectionLink> links = new SafeArrayList<>(SelectionLink.class);
    private Map<Object, SelectionLink> linkIndex = new HashMap<>();
    private Material wireMaterial;
    private Material nodeWireMaterial;
    private static final ColorRGBA DEFAULT_HIGHLIGHT_COLOR = new ColorRGBA(1, 1, 0, 0.7f);
    private ColorRGBA highlightColor = new ColorRGBA(DEFAULT_HIGHLIGHT_COLOR);
    private float alphaTime = 0;
    private float minAlpha = 0.1f;
    private float maxAlpha = 0.3f;

    public enum HighlightMode {
        Wireframe,
        Outline
    }

    public SelectionHighlightState() {
    }

    protected Spix getSpix() {
        return getState(SpixState.class).getSpix();
    }

    protected Node getRoot() {
        return getState(DecoratorViewPortState.class).getRoot();
    }

    @Override
    protected void initialize( Application app ) {
        wireMaterial = GuiGlobals.getInstance().createMaterial(highlightColor, false).getMaterial();

        resetHighlightMode();

        nodeWireMaterial = GuiGlobals.getInstance().createMaterial(highlightColor, false).getMaterial();
        nodeWireMaterial.getAdditionalRenderState().setWireframe(true);
        nodeWireMaterial.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);


        getState(SpixState.class).getSpix().getBlackboard().bind(highlightColorProperty, this, "highlightColor");
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    protected void onEnable() {
        this.selection = getSpix().getBlackboard().get(selectionProperty, SelectionModel.class);
        selection.addPropertyChangeListener(selectionObserver);
        updateSelection();

    }

    @Override
    protected void onDisable() {
        selection.removePropertyChangeListener(selectionObserver);
        resetSelection();
    }

    protected void resetSelection() {
        for (SelectionLink link : links) {
            link.release();
        }
        links.clear();
        linkIndex.clear();
    }

    protected void updateSelection() {
        // Get rid of any of the ones that are now gone
        for( Iterator<Map.Entry<Object, SelectionLink>> it = linkIndex.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Object, SelectionLink> e = it.next();
            if( !selection.contains(e.getKey()) ) {
                //System.out.println("Removing linkage for:" + e.getKey());
                SelectionLink link = e.getValue();
                links.remove(link);
                link.release();
                it.remove();
            }
        }

        // Now add any new ones
        for( Object o : selection ) {
            if( !(o instanceof Spatial) ) {
                // Then we don't care about it
                continue;
            }
            if( linkIndex.containsKey(o) ) {
                // We already have a link for it
                continue;
            }
            //System.out.println("Adding linkage for:" + o);
            SelectionLink link = new SelectionLink((Spatial)o);
            links.add(link);
            linkIndex.put(o, link);
        }
    }

    @Override
    public void update( float tpf ) {
        alphaTime += tpf;

        // Calculate the sin from 0 to 1
        float sine = (FastMath.sin(alphaTime * 6) + 1) * 0.5f;
        float a = minAlpha + sine * (maxAlpha - minAlpha);
        highlightColor.a = a;
    }

    @Override
    public void render( RenderManager renderManager ) {

        // Updating the position of the wire frames needs to be done in render()
        // because dragging is done in update and this lets the states be a little
        // more order independent.  We could also have just made sure this state
        // was always after the transform states.
        for( SelectionLink link : links.getArray() ) {
            link.update();
        }
    }

    public void setHighlightMode(HighlightMode mode){
        if( this.highlightMode == mode ) {
            return;
        }
        this.highlightMode = mode;
        resetHighlightMode();
    }

    public HighlightMode getHighlightMode() {
        return highlightMode;
    }

    protected void resetHighlightMode() {
        if( wireMaterial == null ) {
            return;
        }
        switch( highlightMode ) {
            case Wireframe:
                wireMaterial.getAdditionalRenderState().setWireframe(true);
                wireMaterial.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
                wireMaterial.getAdditionalRenderState().setLineWidth(1);
                wireMaterial.getAdditionalRenderState().setPolyOffset(0,0);
                wireMaterial.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
                minAlpha = 0.3f;
                maxAlpha = 0.5f;
                break;
            case Outline:
                wireMaterial.getAdditionalRenderState().setWireframe(true);
                wireMaterial.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
                wireMaterial.getAdditionalRenderState().setLineWidth(2);
                wireMaterial.getAdditionalRenderState().setPolyOffset(-3f,-3f);
                wireMaterial.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);
                minAlpha = 0.5f;
                maxAlpha = 0.7f;
                break;
        }
    }

    protected class SelectionLink {
        private Spatial source;
        private Geometry wire;

        public SelectionLink( Spatial source ) {
            this.source = source;

            // Create the wire frame
            if( source instanceof Geometry ) {
                createWire((Geometry)source);
            } else if( source instanceof Node ) {
                createWire((Node)source);
            } else {
                throw new IllegalArgumentException("Unhandled source:" + source);
            }
        }

        private void createWire( Geometry geom ) {
            this.wire = new Geometry(geom.getName() + ".wire", geom.getMesh());
            wire.setMaterial(wireMaterial);
            update();
            getRoot().attachChild(wire);
        }

        private void createWire( Node node ) {
            // Need to create a wire frame for the bounding shape


            Vector3f points [] = new Vector3f[8];

            //BoundingBox bb = (BoundingBox) node.getWorldBound();
            BoundingBox bb2 = new BoundingBox(Vector3f.ZERO,0.1f,0.1f,0.1f);

            for (Spatial spatial : node.getChildren()) {
                BoundingVolume b = spatial.getWorldBound();
                if (b != null) {
                    b = b.clone();
                    b.setCenter(node.worldToLocal(b.getCenter(), null));
                    if (b instanceof BoundingBox) {
                        BoundingBox bb = (BoundingBox) b;
                        bb.setXExtent(bb.getXExtent() / node.getWorldScale().getX());
                        bb.setYExtent(bb.getYExtent() / node.getWorldScale().getY());
                        bb.setZExtent(bb.getZExtent() / node.getWorldScale().getZ());
                    }
                    bb2.mergeLocal(b);
                }
            }
            //bb2.mergeLocal(bb);

            float xe = bb2.getXExtent() + 0.1f/node.getWorldScale().getX();
            float ye = bb2.getYExtent() + 0.1f/node.getWorldScale().getY();
            float ze = bb2.getZExtent() + 0.1f/node.getWorldScale().getZ();
            float x = bb2.getCenter().x;
            float y = bb2.getCenter().y;
            float z = bb2.getCenter().z;

            points[0] = new Vector3f(x - xe, y - ye, z - ze);
            points[1] = new Vector3f(x - xe, y + ye, z - ze);
            points[2] = new Vector3f(x + xe, y + ye, z - ze);
            points[3] = new Vector3f(x + xe, y - ye, z - ze);

            points[4] = new Vector3f(x - xe, y - ye, z + ze);
            points[5] = new Vector3f(x - xe, y + ye, z + ze);
            points[6] = new Vector3f(x + xe, y + ye, z + ze);
            points[7] = new Vector3f(x + xe, y - ye, z + ze);

            WireFrustum frustum = new WireFrustum(points);
            wire = new Geometry(node.getName()+"Selection",frustum);
            wire.setMaterial(nodeWireMaterial);
            update();
            getRoot().attachChild(wire);
        }

        public void update() {
            wire.setLocalTransform(source.getWorldTransform());
        }

        public void release() {
            wire.removeFromParent();
        }
    }

    private class SelectionObserver implements PropertyChangeListener {

        public void propertyChange( PropertyChangeEvent event ) {
            updateSelection();
        }
    }


    public void setHighlightColor(ColorRGBA highlightColor) {
        if(highlightColor == null){
            this.highlightColor.set(DEFAULT_HIGHLIGHT_COLOR);
        } else {
            this.highlightColor.set(highlightColor);
        }
    }

    public ColorRGBA getHighlightColor() {
        return highlightColor;
    }
}
