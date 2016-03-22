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
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;
import com.jme3.scene.control.CameraControl;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.*;
import spix.core.SelectionModel;

/**
 *  A standard app state for a blender like camera motion.
 *
 *  Press mouse wheel button and move the mouse to rotate the view.
 *  Press mouse wheel + left shift and move the mouse to pan the view.
 *  Use the mouse wheel to zoom in and out.
 *  Type NumPad0 to center the view on the selection, to scene center if no selection.
 *
 *  @author    Rémy Bouquet
 */
public class BlenderCameraState extends BaseAppState {

    public static final String GROUP = "Blender Camera";
    public static final FunctionId F_CENTER = new FunctionId(GROUP, "Center");
    public static final FunctionId F_VERTICAL_MOVE = new FunctionId(GROUP, "Vertical Move");
    public static final FunctionId F_HORIZONTAL_MOVE = new FunctionId(GROUP, "Horizontal Move");
    public static final FunctionId F_VERTICAL_ROTATE = new FunctionId(GROUP, "Vertical Rotate");
    public static final FunctionId F_HORIZONTAL_ROTATE = new FunctionId(GROUP, "Horizontal Rotate");
    public static final FunctionId F_ZOOM = new FunctionId(GROUP, "Zoom");

    private final static float PAN_FACTOR = 0.1f;
    private final static float ROT_FACTOR = 0.1f;

    private String selectionProperty = DefaultConstants.SELECTION_PROPERTY;
    private Camera cam;
    private Node target;
    private Node verticalPivot;
    private CameraNode camNode;

    private InputHandler inputHandler = new InputHandler();

    public BlenderCameraState() {
        this(true);
    }

    public BlenderCameraState(boolean enabled ) {
        setEnabled(enabled);
    }

    public void setCamera( Camera cam ) {
        this.cam = cam;
    }

    public Camera getCamera() {
        return cam;
    }

    @Override
    protected void initialize( Application app ) {

        if( this.cam == null ) {
            this.cam = app.getCamera();
        }

        target = new Node("Blender cam target");
        verticalPivot = new Node("Blender cam vertical pivot");
        camNode = new CameraNode("Blender cam holder", cam);
        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        camNode.setEnabled(false);
        target.attachChild(verticalPivot);
        verticalPivot.attachChild(camNode);

        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.addAnalogListener(inputHandler, F_VERTICAL_MOVE, F_HORIZONTAL_MOVE, F_VERTICAL_ROTATE, F_HORIZONTAL_ROTATE, F_ZOOM);
        inputMapper.addStateListener(inputHandler, F_CENTER);

        // See if there are already mappings for these functions.
        if( !inputMapper.hasMappings(F_VERTICAL_ROTATE) && ! inputMapper.hasMappings(F_HORIZONTAL_ROTATE)) {
            System.out.println("Initializing default mappings for:" + F_VERTICAL_ROTATE + " and " + F_HORIZONTAL_ROTATE);
            inputMapper.map(F_HORIZONTAL_ROTATE, Axis.MOUSE_X, Button.MOUSE_BUTTON3);
            inputMapper.map(F_VERTICAL_ROTATE, Axis.MOUSE_Y, Button.MOUSE_BUTTON3);
        }

        if( !inputMapper.hasMappings(F_VERTICAL_MOVE) && ! inputMapper.hasMappings(F_HORIZONTAL_MOVE)) {
            System.out.println("Initializing default mappings for:" + F_VERTICAL_MOVE + " and " + F_HORIZONTAL_MOVE);
            inputMapper.map(F_HORIZONTAL_MOVE, Axis.MOUSE_X, Button.MOUSE_BUTTON3, KeyInput.KEY_LSHIFT);
            inputMapper.map(F_VERTICAL_MOVE, Axis.MOUSE_Y, Button.MOUSE_BUTTON3, KeyInput.KEY_LSHIFT);
        }

        if( !inputMapper.hasMappings(F_ZOOM)) {
            System.out.println("Initializing default mappings for:" + F_ZOOM);
            inputMapper.map(F_ZOOM, Axis.MOUSE_WHEEL);
        }

        if( !inputMapper.hasMappings(F_CENTER)) {
            System.out.println("Initializing default mappings for:" + F_CENTER);
            inputMapper.map(F_CENTER, KeyInput.KEY_NUMPAD0);
        }

        System.out.println("Input functions:" + inputMapper.getFunctionIds());

    }

    @Override
    protected void cleanup( Application app ) {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.removeAnalogListener(inputHandler, F_VERTICAL_MOVE, F_HORIZONTAL_MOVE, F_VERTICAL_ROTATE, F_HORIZONTAL_ROTATE, F_ZOOM);
        inputMapper.removeStateListener(inputHandler, F_CENTER);
    }

    @Override
    protected void onEnable() {
        System.out.println(getClass().getName() + " Enabled");
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.activateGroup(GROUP);

        camNode.setLocalTranslation(0,0,10);
        camNode.lookAt(target.getWorldTranslation(),Vector3f.UNIT_Y);
        camNode.setEnabled(true);

    }

    @Override
    protected void onDisable() {
        System.out.println(getClass().getName() + " Disabled");
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.deactivateGroup(GROUP);

        target.removeFromParent();
        camNode.setEnabled(false);
    }

    @Override
    public void update( float tpf ) {
        target.updateLogicalState(tpf);
        target.updateGeometricState();
    }

    @Override
    public void postRender() {
    }

    private class InputHandler implements AnalogFunctionListener, StateFunctionListener {

        private Vector3f tmpVec = new Vector3f();
        private Quaternion tmpRot = new Quaternion();

        public void valueChanged( FunctionId func, InputState value, double tpf ) {
            if( func == F_CENTER ) {
                System.out.println("Function:" + func + "  value:" + value + "  tpf:" + tpf );
                if(value == InputState.Off){
                    SelectionModel model = getState(SpixState.class).getSpix().getBlackboard().get(selectionProperty, SelectionModel.class);
                    Object o = model.getSingleSelection();
                    if(o != null){
                        Spatial selected = (Spatial)o;
                        target.setLocalTranslation(selected.getWorldTranslation());
                    } else {
                        target.setLocalTranslation(Vector3f.ZERO);
                    }
                }
            }
        }

        public void valueActive( FunctionId func, double value, double tpf ) {
            if(func == F_HORIZONTAL_MOVE){
                tmpVec.set(cam.getLeft()).multLocal((float)value * PAN_FACTOR);
                target.move(tmpVec);
            } else if(func == F_VERTICAL_MOVE){
                tmpVec.set(cam.getUp()).multLocal((float)-value * PAN_FACTOR);
                target.move(tmpVec);
            } else if(func == F_HORIZONTAL_ROTATE){
                tmpRot.fromAngleAxis((float)-value * ROT_FACTOR, Vector3f.UNIT_Y);
                target.rotate(tmpRot);
            } else if(func == F_VERTICAL_ROTATE){
                tmpRot.set(verticalPivot.getLocalRotation());
                tmpVec.set(tmpRot.getRotationColumn(0));
                tmpRot.fromAngleAxis((float)value * ROT_FACTOR, tmpVec);
                verticalPivot.rotate(tmpRot);
            } else if(func == F_ZOOM){
                float factor = Math.min(camNode.getLocalTranslation().z * 0.1f, 2);
                tmpVec.set(camNode.getLocalTranslation()).addLocal(0,0,(float) -value * factor);
                tmpVec.z = Math.max(tmpVec.z, 1f);
                camNode.setLocalTranslation(tmpVec);
            }
        }
    }
}
