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
import spix.app.properties.WorldScaleProperty;
import spix.core.*;
import spix.props.*;

import java.beans.*;


/**
 * Draws a translation widget over the currently selected object(s)
 * and allows dragging them using various manipulations of the widget.
 *
 * @author Paul Speed
 */
public class ScaleWidgetState extends BaseAppState {

    private String selectionProperty = DefaultConstants.SELECTION_PROPERTY;
    private ScaleManager scaleManager = new ScaleManager();

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

    private static final String GROUP = "Scale State";
    private static final String GROUP_DRAG_ADDITIONAL_INPUTS = "Scaling additional inputs";
    private static final String GROUP_MOVING = "Scaling";
    private static final FunctionId F_DONE = new FunctionId(GROUP_DRAG_ADDITIONAL_INPUTS, "Done");
    private static final FunctionId F_CANCEL = new FunctionId(GROUP_DRAG_ADDITIONAL_INPUTS, "Cancel");
    private static final FunctionId F_X_CONSTRAIN = new FunctionId(GROUP_DRAG_ADDITIONAL_INPUTS, "X axis constrain");
    private static final FunctionId F_Y_CONSTRAIN = new FunctionId(GROUP_DRAG_ADDITIONAL_INPUTS, "Y axis constrain");
    private static final FunctionId F_Z_CONSTRAIN = new FunctionId(GROUP_DRAG_ADDITIONAL_INPUTS, "Z axis constrain");
    private static final FunctionId F_HORIZONTAL_DRAG = new FunctionId(GROUP_MOVING, "Drag Horizontally");
    private static final FunctionId F_VERTICAL_DRAG = new FunctionId(GROUP_MOVING, "Drag vertically");

    //Axis move axis line visual cue.
    private Geometry axisLine;

    private InputMapper inputMapper;

    public ScaleWidgetState(boolean enabled) {
        setEnabled(enabled);
    }

    public void setDragMouseButton(int i) {
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
    protected void initialize(Application app) {
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
        mat.setColor("Color", new ColorRGBA(203 / 255f, 145 / 255f, 73 / 255f, 1));
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        center.setMaterial(mat);
        center.center();
        centerNode.attachChild(center);

        CursorEventControl.addListenersToSpatial(centerNode, new RadialManipulator());

        centerNode.addControl(new BillboardControl());
        widget.attachChild(centerNode);

        // Create the different axes
        widget.attachChild(createAxis(0, Vector3f.UNIT_X, ColorRGBA.Red));
        widget.attachChild(createAxis(1, Vector3f.UNIT_Y, ColorRGBA.Green));
        widget.attachChild(createAxis(2, Vector3f.UNIT_Z, ColorRGBA.Blue));

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
//        inputMapper.map(F_GRAB, KeyInput.KEY_G);

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
        inputMapper.deactivateGroup(GROUP_MOVING);
        //Activating keyboard manipulators to initiate dragging with the keyboard.
        inputMapper.activateGroup(GROUP);
        inputMapper.addStateListener(new StateFunctionListener() {
            @Override
            public void valueChanged(FunctionId func, InputState value, double tpf) {
                if (func == F_DONE && value == InputState.Positive) {
                    //The dragging is done.
                    inputMapper.deactivateGroup(GROUP_MOVING);
                    scaleManager.stopDrag();
                }
                if (func == F_CANCEL && value == InputState.Positive) {
                    //the user canceled the dragging.
                    inputMapper.deactivateGroup(GROUP_MOVING);
                    scaleManager.cancel();
                }
            }
        }, F_DONE, F_CANCEL);

        inputMapper.addStateListener(new StateFunctionListener() {
            @Override
            public void valueChanged(FunctionId func, InputState value, double tpf) {
                //User typed one of the Axis keys to constrain the drag.
                //We set the axis to the scaleManager.
                if (func == F_X_CONSTRAIN) {
                    scaleManager.setConstrainedAxis(0);
                } else if (func == F_Y_CONSTRAIN) {
                    scaleManager.setConstrainedAxis(1);
                } else {
                    scaleManager.setConstrainedAxis(2);
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
                scaleManager.drag(cursor.getX(), cursor.getY());

            }
        }, F_HORIZONTAL_DRAG, F_VERTICAL_DRAG);
    }

    public void startKeyTransform() {
        //Activating the mouse listener and starting to drag at current cursor coordinates.
        inputMapper.activateGroup(GROUP_MOVING);
        Vector2f cursor = getApplication().getInputManager().getCursorPosition();
        scaleManager.startDrag(cursor.getX(), cursor.getY());
    }

    protected Material createMaterial(ColorRGBA color) {
        Material mat = GuiGlobals.getInstance().createMaterial(ColorRGBA.White, false).getMaterial();
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        return mat;
    }

    protected Spatial createAxis(int index, Vector3f dir, ColorRGBA color) {

        Node axis = new Node("axis:" + index);
        axisSpatials[index] = axis;

        // Create the box tip
        Mesh mesh = new Box(0.05f, 0.05f, 0.05f);
        Geometry box = new Geometry("axisCone:" + index, mesh);
        axisColors[index] = color.clone();
        axisMaterials[index] = createMaterial(axisColors[index]);
        box.setMaterial(axisMaterials[index]);

        Vector3f up = Vector3f.UNIT_Y;
        if (dir.distanceSquared(up) < 0.1) {
            up = Vector3f.UNIT_Z;
        }

        box.setLocalTranslation(0, 0, 0.90f);
        axis.attachChild(box);

        // Then the axis line
        Line line = new Line(new Vector3f(0, 0, 0.15f), new Vector3f(0, 0, 0.84f));
        Geometry axisLine = new Geometry("axisLine:" + index, line);
        axisLine.setMaterial(axisMaterials[index]);
        axis.attachChild(axisLine);

        Quaternion rot = new Quaternion();
        rot.lookAt(dir, up);
        axis.setLocalRotation(rot);

        CursorEventControl.addListenersToSpatial(axis, new AxisManipulator(index));

        return axis;
    }

    @Override
    protected void cleanup(Application app) {
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
        System.out.println("ScaleWidgetSelection: Selection:" + selection);
        for (Object o : selection) {

            PropertySet wrapper = spix.getPropertySet(o);
            if (wrapper == null) {
                continue;
            }
            Property translation = wrapper.getProperty("worldTranslation");
            if (translation == null) {
                continue;
            }
            WorldScaleProperty scale = (WorldScaleProperty)wrapper.getProperty("worldScale");
            if (scale == null) {
                continue;
            }
            enableNonUniformScale(scale.isAllowNonUniformScale());

            Property rotation = wrapper.getProperty("worldRotation");
            if (rotation == null) {
                //we don't have rotation meaning that we can't support non uniform scale
                enableNonUniformScale(false);
            }

            Vector3f v = (Vector3f) translation.getValue();
            pos.addLocal(v);


            selectedObjects.add(new SelectedObject(wrapper, scale, translation, rotation));

/*
System.out.println("Translation:" + translation + "  value:" + translation.getValue());
            if( o instanceof Spatial ) {
                Spatial s = (Spatial)o;
                selectedObjects.add(s);
                pos.addLocal(s.getWorldTranslation());
            }*/

        }
        if (selectedObjects.isEmpty()) {
            widget.removeFromParent();
        } else {
            getRoot().attachChild(widget);
            pos.divideLocal(selectedObjects.size());
            selectionCenter.set(pos);
            widget.setLocalTranslation(selectionCenter);
        }
    }

    private void enableNonUniformScale(boolean enable){
        if(enable){
            for (Spatial axisSpatial : axisSpatials) {
                widget.attachChild(axisSpatial);
            }
        } else {
            for (Spatial axisSpatial : axisSpatials) {
                axisSpatial.removeFromParent();
            }
        }
    }

    private float dirAlpha(Vector3f dir, Vector3f axis) {
        float dot = FastMath.abs(dir.dot(axis));
        float alpha = 1f - ((FastMath.clamp(dot, 0.95f, 0.98f) - 0.95f) * (1f / 0.03f));

        return alpha;
    }

    @Override
    public void update(float tpf) {

        //This is here to update the position and rotation of the widget in the case where
        //the selection is translated from the property panel
        //I thought of adding a listener on the translation property, but it would be complicated to properly remove them afterward.
        //This is the most straight forward way I could think of.
        Vector3f pos = new Vector3f();
        for (SelectedObject selectedObject : selectedObjects.getArray()) {
            pos.addLocal(selectedObject.getWorldTranslation());
        }
        pos.divideLocal(selectedObjects.size());
        selectionCenter.set(pos);
        widget.setLocalTranslation(selectionCenter);
        //rotate the widget if a geometry is selected
        if(selectedObjects.size() == 1){
            SelectedObject selected = selectedObjects.get(0);
            Quaternion q = selected.getWorldRotation();
            if(q != null) {
                widget.setLocalRotation(selected.getWorldRotation());
            }
        }


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
        float scale = ((distance / halfHeight) * 100) / m11;
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
    public void render(RenderManager renderManager) {
    }

    /**
     * Called when an axis drag starts.
     */
    private void onStartAxisDrag(int axis) {
        //Hides the widget.
        radial.setCullHint(Spatial.CullHint.Always);
        for (Spatial s : axisSpatials) {
            s.setCullHint(Spatial.CullHint.Always);
        }
        // sets the highlight color to white while dragging
        getState(SpixState.class).getSpix().getBlackboard().set(highlightColorProperty, ColorRGBA.White.clone());

        //Displays the current axis line, sets its color, then compute the corresponding rotation
        //Maybe something better could be done as the direction is already computed in the ScaleManager.
        axisLine.setCullHint(Spatial.CullHint.Dynamic);
        Quaternion rot = new Quaternion();
        switch (axis) {
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
    private void onStopAxisDrag(int axis) {
        //displays the widget
        radial.setCullHint(Spatial.CullHint.Inherit);
        for (Spatial s : axisSpatials) {
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
        for (Spatial s : axisSpatials) {
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
        for (Spatial s : axisSpatials) {
            s.setCullHint(Spatial.CullHint.Inherit);
        }
        //resets the highlight color
        getState(SpixState.class).getSpix().getBlackboard().set(highlightColorProperty, null);
    }

    protected void scaleSelectedObjects(Vector3f delta) {
        Vector3f pos = new Vector3f();
        for (SelectedObject s : selectedObjects) {
            Vector3f v = s.initialScale.mult(delta);
            s.setWorldScale(v);

            pos.set(s.initialPos).subtractLocal(selectionCenter).multLocal(delta);
            pos.addLocal(selectionCenter);
            s.setWorldTranslation(pos);
        }
    }


    /**
     * Handles dragging behaviour.
     */
    private class ScaleManager {

        private Vector2f lastCursor = new Vector2f();
        private Vector2f startCursor = new Vector2f();
        private Vector2f screenPos = new Vector2f();

        private int axis = -1;
        private Vector3f axisScale = new Vector3f();

        private float baseLength;
        private Vector3f originScreen;

        /**
         * Initiates a drag at x,y screen coordinates.
         *
         * @param startX
         * @param startY
         */
        public void startDrag(float startX, float startY) {
            if (axis == -1) {
                onStartRadialDrag();
            } else {
                onStartAxisDrag(axis);
            }

            originScreen = cam.getScreenCoordinates(centerNode.getWorldTranslation());

            for (SelectedObject selectedObject : selectedObjects.getArray()) {
                selectedObject.capture();
            }
            screenPos.set(startX, startY);
            baseLength = screenPos.subtractLocal(originScreen.x, originScreen.y).length();

            inputMapper.activateGroup(GROUP_DRAG_ADDITIONAL_INPUTS);
            startCursor.set(startX, startY);
            lastCursor.set(startCursor);
        }

        /**
         * Stops the drag, and reset all radial states.
         */
        public void stopDrag() {
            if (axis == -1) {
                onStopRadialDrag();
            } else {
                onStopAxisDrag(axis);
            }
            inputMapper.deactivateGroup(GROUP_DRAG_ADDITIONAL_INPUTS);
            originScreen = null;
            axis = -1;
        }

        /**
         * Drags the selection according to the x,y screen coordinates.
         *
         * @param newX the new x
         * @param newY the new y
         */
        public void drag(float newX, float newY) {
            if (originScreen == null) {
                // Not dragging
                return;
            }

            screenPos.set(newX, newY);
            float length = screenPos.subtractLocal(originScreen.x, originScreen.y).length();
            float ratio = length / baseLength;

            if (axis == -1) {
                scaleSelectedObjects(new Vector3f(ratio, ratio, ratio));
            } else {
                axisScale.set(1, 1, 1);
                axisScale.set(axis, ratio);
                scaleSelectedObjects(axisScale);
            }
            lastCursor.set(newX, newY);
        }

        /**
         * Resets the selection to its start position and stops the drag.
         */
        public void cancel() {
            for (SelectedObject selectedObject : selectedObjects.getArray()) {
                selectedObject.reset();
            }
            stopDrag();
        }

        /**
         * Constrains the dragging movement to one world axis
         *
         * @param axis
         */
        public void setConstrainedAxis(int axis) {

            boolean dragging = originScreen != null;
            //If we are currently dragging, we reset the drag states
            if (dragging) {
                cancel();
            }

            //setting the axis and the direction from that axis.
            this.axis = axis;

            //If we were dragging, we start over
            if (dragging) {
                startDrag(startCursor.getX(), startCursor.getY());
                drag(lastCursor.getX(), lastCursor.getY());
            }
        }

    }

    //Radial and Axis manipulator are now pretty similar, maybe something could be done to factor the code.
    private class RadialManipulator implements CursorListener {


        public void cursorButtonEvent(CursorButtonEvent event, Spatial target, Spatial capture) {

            if (event.getButtonIndex() != dragMouseButton) {
                return;
            }

            if (event.isPressed()) {
                scaleManager.startDrag(event.getX(), event.getY());

            } else {
                scaleManager.stopDrag();
            }
            event.setConsumed();
        }

        public void cursorEntered(CursorMotionEvent event, Spatial target, Spatial capture) {
        }

        public void cursorExited(CursorMotionEvent event, Spatial target, Spatial capture) {
        }

        public void cursorMoved(CursorMotionEvent event, Spatial target, Spatial capture) {
            scaleManager.drag(event.getX(), event.getY());
        }
    }

    private class AxisManipulator implements CursorListener {

        private int axis;

        public AxisManipulator(int axis) {
            this.axis = axis;
        }

        public void cursorButtonEvent(CursorButtonEvent event, Spatial target, Spatial capture) {
            if (event.getButtonIndex() != dragMouseButton) {
                return;
            }

            if (event.isPressed()) {
                scaleManager.setConstrainedAxis(axis);
                scaleManager.startDrag(event.getX(), event.getY());
            } else {
                scaleManager.stopDrag();
            }
            event.setConsumed();
        }

        public void cursorEntered(CursorMotionEvent event, Spatial target, Spatial capture) {
        }

        public void cursorExited(CursorMotionEvent event, Spatial target, Spatial capture) {
        }

        public void cursorMoved(CursorMotionEvent event, Spatial target, Spatial capture) {
            scaleManager.drag(event.getX(), event.getY());
        }
    }

    private class SelectedObject {
        private PropertySet properties;
        private Property scale;
        private Property translation;
        private Property rotation;

        private Vector3f initialScale = new Vector3f();
        private Vector3f initialPos = new Vector3f();

        public SelectedObject(PropertySet properties, Property scale, Property translation, Property rotation) {
            this.properties = properties;
            this.scale = scale;
            this.translation = translation;
            this.rotation = rotation;
        }

        public Vector3f getWorldScale() {
            return (Vector3f) scale.getValue();
        }

        public void setWorldScale(Vector3f v) {
            scale.setValue(v);
        }


        public Vector3f getWorldTranslation() {
            return (Vector3f) translation.getValue();
        }

        public void setWorldTranslation(Vector3f v) {
            translation.setValue(v);
        }

        public Quaternion getWorldRotation() {
            return rotation != null?(Quaternion) rotation.getValue():null;
        }

        public void setWorldRotation(Quaternion q) {
            rotation.setValue(q);
        }

        public void capture() {
            initialScale.set(getWorldScale());
            initialPos.set(getWorldTranslation());
        }

        public void reset() {
            setWorldScale(initialScale);
            setWorldTranslation(initialPos);
        }
    }

    private class SelectionObserver implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent event) {
            updateSelection();
        }
    }
}

