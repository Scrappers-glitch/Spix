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
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.*;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.*;
import com.jme3.texture.Texture;
import com.jme3.util.SafeArrayList;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.*;
import com.simsilica.lemur.input.*;
import spix.app.utils.CameraUtils;
import spix.app.utils.ShapeUtils;
import spix.core.SelectionModel;
import spix.core.Spix;
import spix.props.Property;
import spix.props.PropertySet;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


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
    private RotateManager rotateManager = new RotateManager();

    private String highlightColorProperty = DefaultConstants.SELECTION_HIGHLIGHT_COLOR;
    private SelectionModel selection;
    private SelectionObserver selectionObserver = new SelectionObserver();
    private Node widget;
    private Node centerNode;
    private Node radial;
    private Geometry center;
    private Spatial[] axisSpatials = new Spatial[3];
    private Material[] axisMaterials = new Material[3];
    private ColorRGBA[] axisColors = new ColorRGBA[3];
    private Camera cam;

    private SafeArrayList<SelectedObject> selectedObjects = new SafeArrayList<>(SelectedObject.class);
    private Vector3f selectionCenter = new Vector3f();

    private int dragMouseButton = MouseInput.BUTTON_LEFT;

    private static final String GROUP = "Rotate State";
    private static final String GROUP_ROTATE_ADDITIONAL_INPUTS = "Rotate additional inputs";
    private static final String GROUP_ROTATING = "Rotating";
    private static final FunctionId F_DONE = new FunctionId(GROUP_ROTATE_ADDITIONAL_INPUTS, "Done");
    private static final FunctionId F_CANCEL = new FunctionId(GROUP_ROTATE_ADDITIONAL_INPUTS, "Cancel");
    private static final FunctionId F_X_CONSTRAIN = new FunctionId(GROUP_ROTATE_ADDITIONAL_INPUTS, "X axis constrain");
    private static final FunctionId F_Y_CONSTRAIN = new FunctionId(GROUP_ROTATE_ADDITIONAL_INPUTS, "Y axis constrain");
    private static final FunctionId F_Z_CONSTRAIN = new FunctionId(GROUP_ROTATE_ADDITIONAL_INPUTS, "Z axis constrain");
    private static final FunctionId F_HORIZONTAL_DRAG = new FunctionId(GROUP_ROTATING, "Drag Horizontally");
    private static final FunctionId F_VERTICAL_DRAG = new FunctionId(GROUP_ROTATING, "Drag vertically");

    //Axis move axis line visual cue.
    private Geometry axisLine;
    private InputMapper inputMapper;

    private Quaternion tmpQuat = new Quaternion();
    private Vector3f tmpVec3 = new Vector3f();

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
        radial = new Node("radial");

        Geometry radGeom = new Geometry("centerRadial", mesh);
        Texture texture = globals.loadTexture("Interface/circle.png", false, false);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        Material mat = globals.createMaterial(texture, false).getMaterial();
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        radGeom.setMaterial(mat);
        radGeom.center();
        radial.attachChild(radGeom);
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
        radial.attachChild(outerCircle);
        //Dummy torus geom around the circle to be able to pick it, and with a wider hit box.
        Torus t = new Torus(12,4, 0.05f , OUTER_RADIUS );
        Geometry pick = new Geometry("pickOuter", t);
        pick.setCullHint(Spatial.CullHint.Always);
        radial.attachChild(pick);

        Geometry innerCircle = ShapeUtils.makeCircleGeometry("innerCircle:", AXIS_RADIUS, 32);
        innerCircle.setMaterial(createMaterial(ColorRGBA.Black));
        radial.attachChild(innerCircle);

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

        //Deactivating the additional inputs available while dragging (they'll be activated when a drag is initiated)
        inputMapper.deactivateGroup(GROUP_ROTATE_ADDITIONAL_INPUTS);
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
                    rotateManager.stopDrag();
                    getState(SelectionAppState.class).cancelNextEvent();
                }
                if(func == F_CANCEL && value == InputState.Positive){
                    //the user canceled the dragging.
                    inputMapper.deactivateGroup(GROUP_ROTATING);
                    rotateManager.cancel();
                    getState(SelectionAppState.class).cancelNextEvent();
                }
            }
        },F_DONE, F_CANCEL);

        inputMapper.addStateListener(new StateFunctionListener() {
            @Override
            public void valueChanged(FunctionId func, InputState value, double tpf) {
                //User typed one of the Axis keys to constrain the drag.
                //We set the axis to the rotateManager.
                if (func == F_X_CONSTRAIN) {
                    rotateManager.setConstrainedAxis(0);
                } else if (func == F_Y_CONSTRAIN) {
                    rotateManager.setConstrainedAxis(1);
                } else {
                    rotateManager.setConstrainedAxis(2);
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
                rotateManager.drag(cursor.getX(), cursor.getY());

            }
        }, F_HORIZONTAL_DRAG, F_VERTICAL_DRAG);
    }

    public void startKeyTransform() {
        //Activating the mouse listener and starting to drag at current cursor coordinates.
        inputMapper.activateGroup(GROUP_ROTATING);
        Vector2f cursor = getApplication().getInputManager().getCursorPosition();
        rotateManager.startDrag(cursor.getX(), cursor.getY());
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

        axisColors[index] = color.clone();
        Material m = new Material(getApplication().getAssetManager(), "MatDefs/circle/circle.j3md");
        m.setColor("Color", axisColors[index]);
        m.setVector3("DiscardPosition", position);
        m.getAdditionalRenderState().setLineWidth(2);
        m.getAdditionalRenderState().setDepthTest(false);
        axisMaterials[index] = m;

        //Dummy torus geom around the circle to be able to pick it, and with a wider hit box.
        Torus t = new Torus(12,4, 0.05f , AXIS_RADIUS );
        Geometry pick = new Geometry("pick"+index, t);
        pick.setCullHint(Spatial.CullHint.Always);
        axis.attachChild(pick);

        Vector3f up = Vector3f.UNIT_Y;
        if( dir.distanceSquared(up) < 0.1 ) {
            up = Vector3f.UNIT_Z;
        }

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

    @Override
    public void update( float tpf ) {

        updateWidgetPosition();

        float scale = CameraUtils.getConstantScale(cam, widget.getWorldTranslation(), tmpVec3);
        widget.setLocalScale(scale);

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
        //Maybe something better could be done as the direction is already computed in the RotateManager.
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

    protected void rotateSelectedObjects(Quaternion delta ) {

        //Rotating in world space according to the selection center (median point of all the selections)
        //We could add an option to rotate each object according to it's own center.
        //We have to apply the rotation to each object's transforms in world space according to the selection center.
        for( SelectedObject s : selectedObjects.getArray() ) {
            //extracting the offset position of the object relative to the pivot.
            tmpVec3.set(s.getWorldTranslation()).subtractLocal(selectionCenter);
            //Applying the rotation to the position.
            delta.mult(tmpVec3, tmpVec3);
            //we combine the delta with the rotation of the selection
            //note that the order is important because we want rotation in world space.
            tmpQuat.set(delta).multLocal(s.getWorldRotation());
            s.setWorldRotation(tmpQuat);
            //world translation is the addition of pivot position and the rotated offset position.
            s.setWorldTranslation(tmpVec3.addLocal(selectionCenter));
        }
    }

    /**
     * Handles rotating behaviour.
     * to rotate the selection you have to pick a handle (axis or the white circle),
     * then rotate your mouse cursor around the center of the selection
     */
    private class RotateManager {

        Vector3f lastDirection;
        Vector3f originScreen;
        Vector3f newDirection = new Vector3f();
        Quaternion deltaRot = new Quaternion();
        private Vector2f lastCursor = new Vector2f();
        private Vector2f startCursor = new Vector2f();

        private int axis = -1;
        private Vector3f dir = new Vector3f();

        /**
         * initiates the dragging of the current selection at x,y screen coordinates.
         * @param startX
         * @param startY
         */
        public void startDrag(float startX, float startY){
            if(axis == -1){
                onStartRadialDrag();
            } else {
                onStartAxisDrag(axis);
            }

            for (SelectedObject selectedObject : selectedObjects.getArray()) {
                selectedObject.capture();
            }

            //finding the direction from the origin of the selection to the mouse cursor position
            originScreen = cam.getScreenCoordinates(centerNode.getWorldTranslation());
            lastDirection = new Vector3f(startX, startY, 0).subtractLocal(originScreen).normalizeLocal();
            inputMapper.activateGroup(GROUP_ROTATE_ADDITIONAL_INPUTS);
            startCursor.set(startX, startY);
            lastCursor.set(startCursor);
        }

        /**
         * Sops dragging and reset all states.
         */
        public void stopDrag(){
            if(axis == -1){
                onStopRadialDrag();
            } else {
                onStopAxisDrag(axis);
            }
            lastDirection = null;
            axis = -1;
            inputMapper.deactivateGroup(GROUP_ROTATE_ADDITIONAL_INPUTS);
        }

        /**
         * drags the selection according to x,y screen coordinates
         * @param x
         * @param y
         */
        public void drag(float x, float y){
            if( lastDirection == null ) {
                // Not dragging
                return;
            }

            //here we compute the direction from the origin of the selection to the mouse cursor position.
            newDirection.set(x, y, 0).subtractLocal(originScreen).normalizeLocal();
            //then we compute the angle between this direction vector and the one from previous tick
            float angle = FastMath.acos(lastDirection.dot(newDirection));

            //we find the proper axis to rotate around depending of the picked handle
            Vector3f rotAxis = dir;
            if(axis == -1){
                rotAxis = cam.getDirection().negate();
            }

            //here we compute the sign of the angle depending of the orientation of the view.
            float sign = Math.signum(lastDirection.crossLocal(newDirection).dot(Vector3f.UNIT_Z) *  - cam.getDirection().dot(rotAxis));

            //make a quaternion out of this angle
            deltaRot.fromAngleAxis(sign * angle, rotAxis);
            //and rotate the object
            rotateSelectedObjects(deltaRot);

            //saving this tick direction to use on next tick.
            lastDirection.set(newDirection);
            lastCursor.set(x, y);
        }

        /**
         * Resets the selection to its start position and stops the drag.
         */
        public void cancel(){
            for (SelectedObject selectedObject : selectedObjects.getArray()) {
                selectedObject.reset();
            }
            stopDrag();
        }

        /**
         * Constrains the dragging movement to one world axis
         * @param axis
         */
        public void setConstrainedAxis(int axis){

            boolean dragging = lastDirection != null;
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
    }

    //Radial and Axis manipulator are now pretty similar, maybe something could be done to factor the code.
    private class RadialManipulator implements CursorListener {


        public void cursorButtonEvent( CursorButtonEvent event, Spatial target, Spatial capture ) {

            if( event.getButtonIndex() != dragMouseButton ) {
                return;
            }

            if( event.isPressed() ) {
                rotateManager.startDrag(event.getX(), event.getY());

            } else {
                rotateManager.stopDrag();
            }
            event.setConsumed();
        }

        public void cursorEntered( CursorMotionEvent event, Spatial target, Spatial capture ) {
        }

        public void cursorExited( CursorMotionEvent event, Spatial target, Spatial capture ) {
        }

        public void cursorMoved( CursorMotionEvent event, Spatial target, Spatial capture ) {
            rotateManager.drag(event.getX(), event.getY());
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
                rotateManager.setConstrainedAxis(axis);
                rotateManager.startDrag(event.getX(), event.getY());
            } else {
                rotateManager.stopDrag();
            }
            event.setConsumed();
        }

        public void cursorEntered( CursorMotionEvent event, Spatial target, Spatial capture ) {
        }

        public void cursorExited( CursorMotionEvent event, Spatial target, Spatial capture ) {
        }

        public void cursorMoved( CursorMotionEvent event, Spatial target, Spatial capture ) {
            rotateManager.drag(event.getX(), event.getY());
        }
    }

    private class SelectedObject {
        private PropertySet properties;
        private Property translation;
        private Property rotation;
        private Quaternion initialRot = new Quaternion();

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

        public void capture() {
            initialRot.set(getWorldRotation());
        }

        public void reset() {
            setWorldRotation(initialRot);
        }

    }

    private class SelectionObserver implements PropertyChangeListener {

        public void propertyChange( PropertyChangeEvent event ) {
            updateSelection();
        }
    }
}

