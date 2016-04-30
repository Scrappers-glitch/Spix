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
import com.jme3.input.*;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.*;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.*;
import com.jme3.scene.shape.Line;
import com.jme3.texture.Texture;
import com.jme3.util.SafeArrayList;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.*;
import com.simsilica.lemur.input.*;
import spix.app.utils.ShapeUtils;
import spix.core.*;
import spix.props.*;

import java.beans.*;


/**
 *  Draws a translation widget over the currently selected object(s)
 *  and allows dragging them using various manipulations of the widget.
 *
 *  @author    Paul Speed
 *   @author    Rémy Bouquet
 */
public class RotationWidgetState extends BaseAppState {

    public static final float AXIS_RADIUS = 0.75f;
    public static final float OUTER_RADIUS = 0.9f;
    private String selectionProperty = DefaultConstants.SELECTION_PROPERTY;
    private DragManager dragManager = new DragManager();

    private String highlightColorProperty = DefaultConstants.SELECTION_HIGHLIGHT_COLOR;
    private SelectionModel selection;
    private SelectionObserver selectionObserver = new SelectionObserver();
    private Node widget;
    private Node centerNode;
    private Geometry radial;
    private Geometry center;
    private Spatial[] axisSpatials = new Spatial[3];
    private Material[] axisMaterials = new Material[3];
    private ColorRGBA[] axisColors = new ColorRGBA[3];
    private Camera cam;

    private SafeArrayList<SelectedObject> selectedObjects = new SafeArrayList<>(SelectedObject.class);
    private Vector3f selectionCenter = new Vector3f();

    private int dragMouseButton = MouseInput.BUTTON_LEFT;

    private static final String GROUP = "Rotate State";
    private static final String GROUP_DRAG_ADDITIONAL_INPUTS = "Rotate additional inputs";
    private static final String GROUP_ROTATING = "Rotating";
    private static final FunctionId F_DONE = new FunctionId(GROUP_DRAG_ADDITIONAL_INPUTS, "Done");
    private static final FunctionId F_CANCEL = new FunctionId(GROUP_DRAG_ADDITIONAL_INPUTS, "Cancel");
    private static final FunctionId F_X_CONSTRAIN = new FunctionId(GROUP_DRAG_ADDITIONAL_INPUTS, "X axis constrain");
    private static final FunctionId F_Y_CONSTRAIN = new FunctionId(GROUP_DRAG_ADDITIONAL_INPUTS, "Y axis constrain");
    private static final FunctionId F_Z_CONSTRAIN = new FunctionId(GROUP_DRAG_ADDITIONAL_INPUTS, "Z axis constrain");
    private static final FunctionId F_HORIZONTAL_DRAG = new FunctionId(GROUP_ROTATING, "Drag Horizontally");
    private static final FunctionId F_VERTICAL_DRAG = new FunctionId(GROUP_ROTATING, "Drag vertically");

    //Axis move axis line visual cue.
    private Geometry axisLine;

    private InputMapper inputMapper;

    public RotationWidgetState(boolean enabled) {
        setEnabled(enabled);
    }

    public void setDragMouseButton( int i ) {
        this.dragMouseButton = i;
    }

    public int getDragMouseButton() {
        return dragMouseButton;
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

        widget = new Node("rotationWidget");

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


        Geometry outerCircle = ShapeUtils.makeCircleGeometry("outerCircle:", OUTER_RADIUS, 32);
        outerCircle.setMaterial(createMaterial(ColorRGBA.White));
        centerNode.attachChild(outerCircle);


        Geometry innerCircle = ShapeUtils.makeCircleGeometry("outerCircle:", AXIS_RADIUS, 32);
        innerCircle.setMaterial(createMaterial(ColorRGBA.Black));
        centerNode.attachChild(innerCircle);

        CursorEventControl.addListenersToSpatial(centerNode, new RadialManipulator());

        centerNode.addControl(new BillboardControl());
        widget.attachChild(centerNode);

        // Create the different axes
        widget.attachChild(createAxis(0, Vector3f.UNIT_X, ColorRGBA.Red, centerNode.getWorldTranslation()));
        widget.attachChild(createAxis(1, Vector3f.UNIT_Y, ColorRGBA.Green, centerNode.getWorldTranslation()));
        widget.attachChild(createAxis(2, Vector3f.UNIT_Z, ColorRGBA.Blue, centerNode.getWorldTranslation()));

        widget.setQueueBucket(Bucket.Transparent);

        //getRoot().attachChild(widget);

        initKeyMappings();

        Line l = new Line(new Vector3f(0, 0, -1000), new Vector3f(0, 0, 1000));
        axisLine = new Geometry("Axis Line", l);
        axisLine.setMaterial(globals.createMaterial(false).getMaterial());
        axisLine.setCullHint(Spatial.CullHint.Always);
        widget.attachChild(axisLine);

    }

    private void initKeyMappings() {
        inputMapper = GuiGlobals.getInstance().getInputMapper();
        //inputMapper.map(F_GRAB, KeyInput.KEY_G);

        inputMapper.map(F_DONE, KeyInput.KEY_RETURN);
        inputMapper.map(F_DONE, Button.MOUSE_BUTTON1);

        inputMapper.map(F_CANCEL, KeyInput.KEY_ESCAPE);
        inputMapper.map(F_CANCEL, Button.MOUSE_BUTTON2);

        inputMapper.map(F_HORIZONTAL_DRAG, Axis.MOUSE_X);
        inputMapper.map(F_VERTICAL_DRAG, Axis.MOUSE_Y);

        inputMapper.map(F_X_CONSTRAIN, KeyInput.KEY_X);
        inputMapper.map(F_Y_CONSTRAIN, KeyInput.KEY_Y);
        inputMapper.map(F_Z_CONSTRAIN, KeyInput.KEY_Z);

        //Deactivating the additional inputs avilable while dragging (they'll be activated when a drag is initiated)
        inputMapper.deactivateGroup(GROUP_DRAG_ADDITIONAL_INPUTS);
        //Deactivating the dragging mouse listener (It will be activated if a drag is initiated with the keyboard (the F_GRAB function)
        inputMapper.deactivateGroup(GROUP_ROTATING);
        //Activating keyboard manipulators to initiate dragging with the keyboard.
        inputMapper.activateGroup(GROUP);
        inputMapper.addStateListener(new StateFunctionListener() {
            @Override
            public void valueChanged(FunctionId func, InputState value, double tpf) {
                if(func == F_DONE && value == InputState.Positive){
                    //The dragging is done.
                    inputMapper.deactivateGroup(GROUP_ROTATING);
                    dragManager.stopDrag();
                }
                if(func == F_CANCEL && value == InputState.Positive){
                    //the user canceled the dragging.
                    inputMapper.deactivateGroup(GROUP_ROTATING);
                    dragManager.cancel();
                }
            }
        },F_DONE, F_CANCEL);

        inputMapper.addStateListener(new StateFunctionListener() {
            @Override
            public void valueChanged(FunctionId func, InputState value, double tpf) {
                //User typed one of the Axis keys to constrain the drag.
                //We set the axis to the dragManager.
                if (func == F_X_CONSTRAIN) {
                    dragManager.setConstrainedAxis(0);
                } else if (func == F_Y_CONSTRAIN) {
                    dragManager.setConstrainedAxis(1);
                } else {
                    dragManager.setConstrainedAxis(2);
                }
            }
        }, F_X_CONSTRAIN, F_Y_CONSTRAIN, F_Z_CONSTRAIN);

        // Here I don't think we can use a CursorListener, as no click has occurred on the selection.
        // We could emulate the click by creating a CursorButtonEvent and calling the listener manually,
        // but seams hackish to me.
        inputMapper.addAnalogListener(new AnalogFunctionListener() {
            @Override
            public void valueActive(FunctionId func, double value, double tpf) {
                Vector2f cursor = getApplication().getInputManager().getCursorPosition();
                dragManager.drag(cursor.getX(), cursor.getY());

            }
        }, F_HORIZONTAL_DRAG, F_VERTICAL_DRAG);
    }

    public void startKeyTransform() {
        //Activating the mouse listener and starting to drag at current cursor coordinates.
        inputMapper.activateGroup(GROUP_ROTATING);
        Vector2f cursor = getApplication().getInputManager().getCursorPosition();
        dragManager.startDrag(cursor.getX(), cursor.getY());
    }

    protected Material createMaterial( ColorRGBA color ) {
        Material mat = GuiGlobals.getInstance().createMaterial(ColorRGBA.White, false).getMaterial();
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setDepthTest(false);
        //mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        return mat;
    }

    protected Spatial createAxis( int index, Vector3f dir, ColorRGBA color , Vector3f position) {

        Node axis = new Node("axis:" + index);
        axisSpatials[index] = axis;

        // Create the cone tip
//        Mesh mesh = new Cylinder(2, 12, 0f, 0.045f, 0.18f, true, false);
//        Geometry cone = new Geometry("axisCone:" + index, mesh);
        axisColors[index] = color.clone();
        Material m = new Material(getApplication().getAssetManager(), "MatDefs/circle/circle.j3md");
        m.setColor("Color", axisColors[index]);
        m.setVector3("DiscardPosition", position);
        m.getAdditionalRenderState().setLineWidth(2);
        m.getAdditionalRenderState().setDepthTest(false);
        axisMaterials[index] = m;

        //cone.setMaterial(axisMaterials[index]);

        Vector3f up = Vector3f.UNIT_Y;
        if( dir.distanceSquared(up) < 0.1 ) {
            up = Vector3f.UNIT_Z;
        }

//        cone.setLocalTranslation(0, 0, 0.75f);
//        axis.attachChild(cone);

        // Then the axis circle
        Geometry axisCircle = ShapeUtils.makeCircleGeometry("axisCircle:" + index, AXIS_RADIUS, 32);
        axisCircle.setMaterial(axisMaterials[index]);
        axis.attachChild(axisCircle);

        Quaternion rot = new Quaternion();
        rot.lookAt(dir, up);
        axis.setLocalRotation(rot);

        CursorEventControl.addListenersToSpatial(axis, new AxisManipulator(index));

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

        Spix spix = getSpix();

        // Calculate the selection center
        Vector3f pos = new Vector3f();
        selectedObjects.clear();
        System.out.println("RotationWidgetSelection: Selection:" + selection);
        for( Object o : selection ) {

            PropertySet wrapper = spix.getPropertySet(o);
            if( wrapper == null ) {
                continue;
            }

            //This is maybe an issue, the rotation widget won't work if the selected object has no translation prop...
            //Can't find an example when this could happen though...
            Property translation = wrapper.getProperty("worldTranslation");
            if( translation == null ) {
                continue;
            }
            Property rotation = wrapper.getProperty("worldRotation");
            if( rotation == null ) {
                continue;
            }

            Vector3f v = (Vector3f)translation.getValue();
            pos.addLocal(v);

            selectedObjects.add(new SelectedObject(wrapper, translation, rotation));

        }
        if( selectedObjects.isEmpty() ) {
            widget.removeFromParent();
        } else {
            getRoot().attachChild(widget);
            pos.divideLocal(selectedObjects.size());
            selectionCenter.set(pos);
            widget.setLocalTranslation(selectionCenter);
        }
    }

//    private float dirAlpha( Vector3f dir, Vector3f axis ) {
//        float dot = FastMath.abs(dir.dot(axis));
//        float alpha = 1f - ((FastMath.clamp(dot, 0.95f, 0.98f) - 0.95f) * (1f/0.03f));
//        return alpha;
//    }

    @Override
    public void update( float tpf ) {

        updateWidgetPosition();

//        Vector3f relative = widget.getWorldTranslation().subtract(cam.getLocation());
//        Vector3f dir = relative.normalize();
//        axisColors[0].a = dirAlpha(dir, Vector3f.UNIT_X);
//        axisColors[1].a = dirAlpha(dir, Vector3f.UNIT_Y);
//        axisColors[2].a = dirAlpha(dir, Vector3f.UNIT_Z);

        // Need to figure out how much to scale the widget so that it stays
        // the same size on screen.  In our case, we want 1 unit to be
        // 100 pixels.
        Vector3f dir = cam.getDirection();
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

    @Override
    public void render( RenderManager renderManager ) {
    }

    /**
     * Called when an axis drag starts.
     */
    private void onStartAxisDrag(int axis ) {
        //Hides the widget.
        radial.setCullHint(Spatial.CullHint.Always);
        for( Spatial s : axisSpatials ) {
            s.setCullHint(Spatial.CullHint.Always);
        }
        // sets the highlight color to white while dragging
        getState(SpixState.class).getSpix().getBlackboard().set(highlightColorProperty, ColorRGBA.White.clone());

        //Displays the current axis line, sets its color, then compute the corresponding rotation
        //Maybe something better could be done as the direction is already computed in the DragManager.
        axisLine.setCullHint(Spatial.CullHint.Dynamic);
        Quaternion rot = new Quaternion();
        switch (axis){
            case 0://X
                rot.lookAt(Vector3f.UNIT_X, Vector3f.UNIT_Y);
                axisLine.getMaterial().setColor("Color", ColorRGBA.Red);
                break;
            case 1://Y
                rot.lookAt(Vector3f.UNIT_Y, Vector3f.UNIT_Y);
                axisLine.getMaterial().setColor("Color", ColorRGBA.Green);
                break;
            case 2://Z
                rot.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
                axisLine.getMaterial().setColor("Color", ColorRGBA.Blue);
                break;
        }
        axisLine.setLocalRotation(rot);
    }

    /**
     * Called when an axis drag is finished.
     */
    private void onStopAxisDrag(int axis ) {
        //displays the widget
        radial.setCullHint(Spatial.CullHint.Inherit);
        for( Spatial s : axisSpatials ) {
            s.setCullHint(Spatial.CullHint.Inherit);
        }
        //resets the highlight color
        getState(SpixState.class).getSpix().getBlackboard().set(highlightColorProperty, null);
        //hides the axis line.
        axisLine.setCullHint(Spatial.CullHint.Always);
    }

    /**
     * Called when a radial drag starts.
     */
    private void onStartRadialDrag() {
        //Hides the widget.
        radial.setCullHint(Spatial.CullHint.Always);
        for( Spatial s : axisSpatials ) {
            s.setCullHint(Spatial.CullHint.Always);
        }
        // sets the highlight color to white while dragging
        getState(SpixState.class).getSpix().getBlackboard().set(highlightColorProperty, ColorRGBA.White.clone());
    }

    /**
     * Called when a radial drag is finished.
     */
    private void onStopRadialDrag() {
        //displays the widget
        radial.setCullHint(Spatial.CullHint.Inherit);
        for( Spatial s : axisSpatials ) {
            s.setCullHint(Spatial.CullHint.Inherit);
        }
        //resets the highlight color
        getState(SpixState.class).getSpix().getBlackboard().set(highlightColorProperty, null);
    }

    protected void updateWidgetPosition() {
        if( selectedObjects.isEmpty() ) {
            widget.removeFromParent();
            return;
        }
        
        // Recalculate the selected object center
        selectionCenter.set(0,0,0);
        for( SelectedObject s : selectedObjects.getArray() ) {
            selectionCenter.addLocal(s.getWorldTranslation());
        }
        selectionCenter.divideLocal(selectedObjects.size());
        widget.setLocalTranslation(selectionCenter);
    }

    protected void moveSelectedObjects( Vector3f delta ) {
        Vector3f pos = new Vector3f();
        for( SelectedObject s : selectedObjects.getArray() ) {
            // Translate the delta into the spatial's local space
            Vector3f v = s.getWorldTranslation().add(delta);
            s.setWorldTranslation(v);
            //v = s.worldToLocal(v, null);
            //s.move(v);
            pos.addLocal(v);
        }
        pos.divide(selectedObjects.size());
        selectionCenter.set(pos);
        widget.setLocalTranslation(selectionCenter);
    }

    /**
     * Handles dragging behaviour.
     */
    private class DragManager{
        protected Vector3f xDelta;
        protected Vector3f yDelta;
        protected Vector2f lastCursor = new Vector2f();
        private Vector3f basePosition = null;
        protected Vector2f startCursor = new Vector2f();

        private int axis = -1;
        private Vector3f dir = new Vector3f();
        private Vector2f cursor = new Vector2f();
        private float startDistance = 0;
        private Vector3f last;

        /**
         * initiates the dragging of the current selection at x,y screen coordinates.
         * @param startX
         * @param startY
         */
        public void startDrag(float startX, float startY){
            if(axis == -1){
                startRadialDrag(startX, startY);
            } else {
                startAxisDrag(startX, startY);
            }
        }

        /**
         * Sops dragging and reset all states.
         */
        public void stopDrag(){
            if(axis == -1){
                stopRadialDrag();
            } else {
                stopAxisDrag();
            }
        }

        /**
         * drags the selection according to x,y screen coordinates
         * @param x
         * @param y
         */
        public void drag(float x, float y){
            if(axis == -1){
                radialDrag(x, y);
            } else {
                axisDrag(x, y);
            }
        }

        /**
         * Initiates a radial drag at x,y screen coordinates.
         * @param x
         * @param y
         */
        public void startRadialDrag(float startX, float startY){
            onStartRadialDrag();

            Vector3f up = cam.getUp();
            Vector3f right = cam.getLeft().negate();

            Vector3f originScreen = cam.getScreenCoordinates(centerNode.getWorldTranslation());
            Vector3f xScreen = cam.getScreenCoordinates(centerNode.getWorldTranslation().add(right));
            Vector3f yScreen = cam.getScreenCoordinates(centerNode.getWorldTranslation().add(up));

            float x = xScreen.x - originScreen.x;
            float y = yScreen.y - originScreen.y;

            System.out.println("delta x:" + x + "  delta y:" + y);

            xDelta = right.divide(x);
            yDelta = up.divide(y);

            System.out.println("xDelta:" + xDelta + "  yDelta:" + yDelta);

            lastCursor.set(startX, startY);
            startCursor.set(startX, startY);

            inputMapper.activateGroup(GROUP_DRAG_ADDITIONAL_INPUTS);
            basePosition = selectionCenter.clone();
        }

        /**
         * Stops the radial drag, and reset all radial states.
         */
        public void stopRadialDrag(){
            onStopRadialDrag();
            xDelta = null;
            yDelta = null;
            inputMapper.deactivateGroup(GROUP_DRAG_ADDITIONAL_INPUTS);
            basePosition = null;
        }

        /**
         * Drags the selection according to the x,y screen coordinates.
         * @param x
         * @param y
         */
        public void radialDrag(float newX, float newY){
            if( xDelta == null ) {
                // Not dragging
                return;
            }

            float x = newX - lastCursor.x;
            float y = newY - lastCursor.y;

            moveSelectedObjects(xDelta.mult(x).addLocal(yDelta.mult(y)));

            lastCursor.set(newX, newY);
        }


        /**
         * Initiates an axis drag at x,y screen coordinates.
         * @param x
         * @param y
         */
        public void startAxisDrag(float x, float y){
            onStartAxisDrag(axis);

            // Keep track of the starting location for the object
            basePosition = selectionCenter.clone(); //target.getWorldTranslation();

            // Find the pick direction from our eye
            Vector3f pickDir = getPickDir(x, y);

            // Find the closest point between the axis line starting at the
            // object and the pick line starting at the camera.  This returns
            // the projected point on the first line (object -> axis)
            startDistance = closestPointProjected(basePosition, dir, cam.getLocation(), pickDir);

            last = new Vector3f();
            inputMapper.activateGroup(GROUP_DRAG_ADDITIONAL_INPUTS);
            startCursor.set(x,y);
        }

        /**
         * Stops the axis drag. deactivate additional key inputs and reset all axis drag states.
         */
        public void stopAxisDrag(){
            onStopAxisDrag(axis);
            basePosition = null;
            inputMapper.deactivateGroup(GROUP_DRAG_ADDITIONAL_INPUTS);
            axis = -1;
        }

        /**
         * Drags the selection according to the x,y screen coordinates, but constrained to the current axis.
         * @param x
         * @param y
         */
        public void axisDrag(float x, float y){
            if( basePosition == null ) {
                // Not dragging
                return;
            }

            // Find the pick direction from our eye
            Vector3f pickDir = getPickDir(x, y);

            // Find the closest point between the axis line starting at the
            // object and the pick line starting at the camera.  This returns
            // the projected point on the first line (object -> axis)
            float distance = closestPointProjected(basePosition, dir, cam.getLocation(), pickDir);

            //System.out.println("distance:" + distance + "  Dragged:" + (distance - startDistance));
            float dragged = distance - startDistance;
            Vector3f newOffset = dir.mult(dragged);
            Vector3f delta = newOffset.subtract(last);
            last.set(newOffset);
            lastCursor.set(x, y);
            moveSelectedObjects(delta);
        }

        /**
         * Resets the selection to its start position and stops the drag.
         */
        public void cancel(){
            moveSelectedObjects(basePosition.subtractLocal(selectionCenter));
            stopDrag();
        }

        /**
         * Constrains the dragging movement to one world axis
         * @param axis
         */
        public void setConstrainedAxis(int axis){

            boolean dragging = basePosition != null;
            //If we are currently dragging, we reset the drag states
            if(dragging) {
                cancel();
            }

            //setting the axis and the direction from that axis.
            this.axis = axis;
            dir.set(0,0,0);
            dir.set(axis, 1);

            //If we were dragging, we start over
            if(dragging) {
                startDrag(startCursor.getX(), startCursor.getY());
                drag(lastCursor.getX(), lastCursor.getY());
            }
        }

        /**
         *  Find the closest points between two lines p0 + u(t) and q0 + v(t)
         *  based on: http://geomalgorithms.com/a07-_distance.html
         */
        protected float closestPointProjected( Vector3f p0, Vector3f u, Vector3f q0, Vector3f v ) {

            //System.out.println("P0:" + p0 + "  u:" + u);
            //System.out.println("Q0:" + q0 + "  v:" + v);
            Vector3f w0 = p0.subtract(q0);

            float a = u.dot(u);
            float b = u.dot(v);
            float c = v.dot(v);
            float d = u.dot(w0);
            float e = v.dot(w0);

            float sc = ((b * e) - (c * d)) / ((a * c) - (b * b));

            /*
            For testing, it's fun to calculate the rest and project them onto the line
            float tc = ((a * e) - (b * d)) / ((a * c) - (b * b));

            System.out.println("sc:" + sc + "  tc:" + tc);
            System.out.println("psc:" + p0.add(u.mult(sc)) + "  qtc:" + q0.add(v.mult(tc)));
            */

            return sc;
        }

        protected Vector3f getPickDir( float x, float y ) {
            cursor.set(x, y);
            Vector3f near = cam.getWorldCoordinates(cursor, 0);
            Vector3f far = cam.getWorldCoordinates(cursor, 1);
            return far.subtract(near).normalizeLocal();
        }
    }

    //Radial and Axis manipulator are now pretty similar, maybe something could be done to factor the code.
    private class RadialManipulator implements CursorListener {


        public void cursorButtonEvent( CursorButtonEvent event, Spatial target, Spatial capture ) {

            if( event.getButtonIndex() != dragMouseButton ) {
                return;
            }

            if( event.isPressed() ) {
                dragManager.startDrag(event.getX(), event.getY());

            } else {
                dragManager.stopDrag();
            }
            event.setConsumed();
        }

        public void cursorEntered( CursorMotionEvent event, Spatial target, Spatial capture ) {
        }

        public void cursorExited( CursorMotionEvent event, Spatial target, Spatial capture ) {
        }

        public void cursorMoved( CursorMotionEvent event, Spatial target, Spatial capture ) {
            dragManager.drag(event.getX(), event.getY());
        }
    }

    private class AxisManipulator implements CursorListener {

        private int axis;

        public AxisManipulator( int axis ) {
            this.axis = axis;
        }

        public void cursorButtonEvent( CursorButtonEvent event, Spatial target, Spatial capture ) {
            if( event.getButtonIndex() != dragMouseButton ) {
                return;
            }

            if( event.isPressed() ) {
                dragManager.setConstrainedAxis(axis);
                dragManager.startDrag(event.getX(), event.getY());
            } else {
                dragManager.stopDrag();
            }
            event.setConsumed();
        }

        public void cursorEntered( CursorMotionEvent event, Spatial target, Spatial capture ) {
        }

        public void cursorExited( CursorMotionEvent event, Spatial target, Spatial capture ) {
        }

        public void cursorMoved( CursorMotionEvent event, Spatial target, Spatial capture ) {
            dragManager.drag(event.getX(), event.getY());
        }
    }

    private class SelectedObject {
        private PropertySet properties;
        private Property translation;
        private Property rotation;

        public SelectedObject( PropertySet properties, Property translation, Property rotation ) {
            this.properties = properties;
            this.translation = translation;
            this.rotation = rotation;
        }

        public Vector3f getWorldTranslation() {
            return (Vector3f)translation.getValue();
        }

        public void setWorldTranslation( Vector3f v ) {
            translation.setValue(v);
        }

        public Quaternion getWorldRotation() {
            return (Quaternion)rotation.getValue();
        }

        public void setWorldRotation( Quaternion q ) {
            rotation.setValue(q);
        }

    }

    private class SelectionObserver implements PropertyChangeListener {

        public void propertyChange( PropertyChangeEvent event ) {
            updateSelection();
        }
    }
}

