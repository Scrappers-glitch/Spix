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
import com.jme3.material.*;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.*;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.*;
import com.jme3.scene.shape.Line;
import com.jme3.texture.Texture;
import com.jme3.util.SafeArrayList;
import com.simsilica.lemur.GuiGlobals;
import spix.core.*;

import java.beans.*;
import java.util.*;


/**
 *  Draws a translation widget over the currently selected object(s)
 *  and allows dragging them using various manipulations of the widget.
 *
 *  @author    Paul Speed
 */
public class TranslationWidgetState extends BaseAppState {

    private String selectionProperty = DefaultConstants.SELECTION_PROPERTY;
    private SelectionModel selection;
    private SelectionObserver selectionObserver = new SelectionObserver();
    private Node widget;
    private Node centerNode;
    private Geometry radial;
    private Geometry center;
    private Material[] axisMaterials = new Material[3];
    private ColorRGBA[] axisColors = new ColorRGBA[3];
    private Camera cam;

    private Vector3f selectionCenter = new Vector3f();

    public TranslationWidgetState() {
    }

    protected Spix getSpix() {
        return getState(SpixState.class).getSpix();
    }

    protected Node getRoot() {
        return getState(DecoratorViewPortState.class).getRoot();
    }

    @Override
    protected void initialize( Application app ) {

        cam = app.getCamera();

        GuiGlobals globals = GuiGlobals.getInstance();

        widget = new Node("translationWidget");

        Quad mesh = new Quad(0.32f, 0.32f);

        centerNode = new Node("center");
        radial = new Geometry("centerRadial", mesh);
        Texture texture = globals.loadTexture("Interface/circle.png", false, false);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        Material mat = globals.createMaterial(texture, false).getMaterial();
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        radial.setMaterial(mat);
        radial.center();
        centerNode.attachChild(radial);

        // Now the teeny tiny center that never disappears
        mesh = new Quad(0.09f, 0.09f);
        center = new Geometry("centerOrigin", mesh);
        texture = globals.loadTexture("Interface/small-circle.png", false, false);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        mat = globals.createMaterial(texture, false).getMaterial();
        mat.setColor("Color", new ColorRGBA(203/255f, 145/255f, 73/255f, 1));
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        center.setMaterial(mat);
        center.center();
        centerNode.attachChild(center);


        centerNode.addControl(new BillboardControl());
        widget.attachChild(centerNode);

        // Create the different axes
        widget.attachChild(createAxis(0, Vector3f.UNIT_X, ColorRGBA.Red));
        widget.attachChild(createAxis(1, Vector3f.UNIT_Y, ColorRGBA.Green));
        widget.attachChild(createAxis(2, Vector3f.UNIT_Z, ColorRGBA.Blue));

        widget.setQueueBucket(Bucket.Transparent);

        //getRoot().attachChild(widget);

    }

    protected Material createMaterial( ColorRGBA color ) {
        Material mat = GuiGlobals.getInstance().createMaterial(ColorRGBA.White, false).getMaterial();
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setDepthTest(false);
        //mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        return mat;
    }

    protected Spatial createAxis( int index, Vector3f dir, ColorRGBA color ) {

        Node axis = new Node("axis:" + index);

        // Create the cone tip
        Mesh mesh = new Cylinder(2, 12, 0f, 0.045f, 0.18f, true, false);
        Geometry cone = new Geometry("axisCone:" + index, mesh);
        axisColors[index] = color.clone();
        axisMaterials[index] = createMaterial(axisColors[index]);
        cone.setMaterial(axisMaterials[index]);

        Vector3f up = Vector3f.UNIT_Y;
        if( dir.distanceSquared(up) < 0.1 ) {
            up = Vector3f.UNIT_Z;
        }

        cone.setLocalTranslation(0, 0, 0.75f);
        axis.attachChild(cone);

        // Then the axis line
        Line line = new Line(new Vector3f(0, 0, 0.15f), new Vector3f(0, 0, 0.84f));
        Geometry axisLine = new Geometry("axisLine:" + index, line);
        axisLine.setMaterial(axisMaterials[index]);
        axis.attachChild(axisLine);

        Quaternion rot = new Quaternion();
        rot.lookAt(dir, up);
        axis.setLocalRotation(rot);

        return axis;
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
        widget.removeFromParent();
    }

    protected void updateSelection() {

        // Calculate the selection center
        Vector3f pos = new Vector3f();
        int count = 0;
        for( Object o : selection ) {
            if( o instanceof Spatial ) {
                Spatial s = (Spatial)o;
                pos.addLocal(s.getWorldTranslation());
                count++;
            }
        }
        if( count == 0 ) {
            widget.removeFromParent();
        } else {
            getRoot().attachChild(widget);
            pos.divideLocal(count);
            selectionCenter.set(pos);
            widget.setLocalTranslation(selectionCenter);
        }
    }

    private float dirAlpha( Vector3f dir, Vector3f axis ) {
        float dot = FastMath.abs(dir.dot(axis));
        float alpha = 1f - ((FastMath.clamp(dot, 0.95f, 0.98f) - 0.95f) * (1f/0.03f));
        return alpha;
    }

    @Override
    public void update( float tpf ) {
    }

    @Override
    public void render( RenderManager renderManager ) {

        // The blender camera state updates the camera using a camera node...
        // which means it happens post-update.  So we have to update our scaling
        // in render().

        Vector3f relative = widget.getWorldTranslation().subtract(cam.getLocation());
        Vector3f dir = relative.normalize();
        axisColors[0].a = dirAlpha(dir, Vector3f.UNIT_X);
        axisColors[1].a = dirAlpha(dir, Vector3f.UNIT_Y);
        axisColors[2].a = dirAlpha(dir, Vector3f.UNIT_Z);

        // Need to figure out how much to scale the widget so that it stays
        // the same size on screen.  In our case, we want 1 unit to be
        // 100 pixels.
        dir = cam.getDirection();
        float distance = dir.dot(widget.getWorldTranslation().subtract(cam.getLocation()));

        // m11 of the projection matrix defines the distance at which 1 pixel
        // is 1 unit.  Kind of.
        float m11 = cam.getProjectionMatrix().m11;

        // Magic scaling... trust the math... don't question the math... magic math...
        float halfHeight = cam.getHeight() * 0.5f;
        float scale = ((distance/halfHeight) * 100)/m11;
        widget.setLocalScale(scale);

        /*
        // But if you want to check the magic math...
        Vector3f s1 = cam.getScreenCoordinates(widget.getWorldTranslation());
        Vector3f s2 = cam.getScreenCoordinates(widget.getWorldTranslation().add(scale, 0, 0));

        System.out.println("screen dist:" + (s2.x - s1.x));
        // Should be 100 when facing directly down z axis
        */
    }

    private class SelectionObserver implements PropertyChangeListener {

        public void propertyChange( PropertyChangeEvent event ) {
            updateSelection();
        }
    }
}
