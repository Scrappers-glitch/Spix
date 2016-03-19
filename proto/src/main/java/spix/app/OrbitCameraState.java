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
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;

import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.*;
import com.simsilica.lemur.input.*;

/**
 *  A standard app state for orbit camera motion.
 *
 *  @author    Paul Speed
 */
public class OrbitCameraState extends BaseAppState {
 
    public static final String GROUP = "Orbit Camera";
    public static final FunctionId F_ORBIT_X = new FunctionId(GROUP, "Orbit X"); 
    public static final FunctionId F_ORBIT_Y = new FunctionId(GROUP, "Orbit Y");
    public static final FunctionId F_ORBIT_ON = new FunctionId(GROUP, "Orbit On"); 
    public static final FunctionId F_MOVE = new FunctionId(GROUP, "Move"); 
    public static final FunctionId F_STRAFE = new FunctionId(GROUP, "Strafe"); 
    public static final FunctionId F_ELEVATION = new FunctionId(GROUP, "Elevation"); 
    public static final FunctionId F_FAST = new FunctionId(GROUP, "Fast"); 

    private Camera cam;
    private InputHandler inputHandler = new InputHandler();
    private FocalPointListener focalPointListener = new FocalPointListener();
    private float normalSpeed = 5;
    private float fastSpeed = 20;
    private float speed = normalSpeed;
    
    private Vector3f focalPoint = null;
    private Vector3f calculatedFocalPoint = new Vector3f(); 
    
    public OrbitCameraState() {
        this(true);
    }
    
    public OrbitCameraState( boolean enabled ) {
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
    
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.addAnalogListener(inputHandler, F_ORBIT_X, F_ORBIT_Y, F_MOVE, F_STRAFE, F_ELEVATION);
        inputMapper.addStateListener(inputHandler, F_FAST, F_ORBIT_ON);
        
        // See if there are already mappings for these functions.
        if( !inputMapper.hasMappings(F_ORBIT_ON) ) {
            System.out.println("Initializing default mappings for:" + F_ORBIT_ON);
            inputMapper.map(F_ORBIT_ON, Button.MOUSE_BUTTON2);
            setupMouseOrbit(inputMapper, Button.MOUSE_BUTTON2); 
        } else {
            // Go through the configured mappings and make sure orbit will
            // work for all of the buttons
            for( InputMapper.Mapping m : inputMapper.getMappings(F_ORBIT_ON) ) {
                // Only handle single values right now
                Object primary = m.getPrimaryActivator();
                setupMouseOrbit(inputMapper, primary);
            }
        }
        if( !inputMapper.hasMappings(F_MOVE) ) {
            System.out.println("Initializing default mappings for:" + F_MOVE);
            inputMapper.map(F_MOVE, Axis.MOUSE_Y, Button.MOUSE_BUTTON3);
            inputMapper.map(F_MOVE, KeyInput.KEY_W); 
            inputMapper.map(F_MOVE, InputState.Negative, KeyInput.KEY_S); 
        }
        if( !inputMapper.hasMappings(F_STRAFE) ) {
            System.out.println("Initializing default mappings for:" + F_STRAFE);
            inputMapper.map(F_STRAFE, Axis.MOUSE_X, Button.MOUSE_BUTTON3); 
            inputMapper.map(F_STRAFE, Axis.MOUSE_X, Button.MOUSE_BUTTON3, Button.MOUSE_BUTTON2); 
            inputMapper.map(F_STRAFE, KeyInput.KEY_D); 
            inputMapper.map(F_STRAFE, InputState.Negative, KeyInput.KEY_A); 
        }
        if( !inputMapper.hasMappings(F_ELEVATION) ) {
            System.out.println("Initializing default mappings for:" + F_STRAFE);
            inputMapper.map(F_ELEVATION, Axis.MOUSE_Y, Button.MOUSE_BUTTON3, Button.MOUSE_BUTTON2); 
            inputMapper.map(F_ELEVATION, KeyInput.KEY_Q); 
            inputMapper.map(F_ELEVATION, InputState.Negative, KeyInput.KEY_Z); 
        }
        if( !inputMapper.hasMappings(F_FAST) ) {
            System.out.println("Initializing default mappings for:" + F_FAST);
            inputMapper.map(F_FAST, KeyInput.KEY_LSHIFT);
            inputMapper.map(F_FAST, KeyInput.KEY_RSHIFT);
        }
        
        System.out.println("Input functions:" + inputMapper.getFunctionIds());
        
        System.out.println("Show mappings:" + inputMapper.getMappings(F_ORBIT_X));
    }
    
    protected void setupMouseOrbit( InputMapper inputMapper, Object button ) {
        System.out.println("Setting up mouse orbit for:" + button);
        inputMapper.map(F_ORBIT_X, Axis.MOUSE_X, button); 
        inputMapper.map(F_ORBIT_Y, Axis.MOUSE_Y, button); 
    }
    
    @Override   
    protected void cleanup( Application app ) {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.removeAnalogListener(inputHandler, F_ORBIT_X, F_ORBIT_Y, F_MOVE, F_STRAFE, F_ELEVATION);
        inputMapper.removeStateListener(inputHandler, F_FAST, F_ORBIT_ON);
    }
    
    @Override   
    protected void onEnable() {
        System.out.println(getClass().getName() + " Enabled");
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.activateGroup(GROUP);
 
        Node rootNode = ((SimpleApplication)getApplication()).getRootNode();       
        CursorEventControl.addListenersToSpatial(rootNode, focalPointListener);
    }
    
    @Override   
    protected void onDisable() {
        System.out.println(getClass().getName() + " Disabled");
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.deactivateGroup(GROUP);
        Node rootNode = ((SimpleApplication)getApplication()).getRootNode();       
        CursorEventControl.removeListenersFromSpatial(rootNode, focalPointListener);
    }
 
    @Override
    public void update( float tpf ) {
    }

    @Override
    public void postRender() { 
    }
 
    protected Vector3f getFocalPoint() {
        if( focalPoint != null ) {
            return focalPoint;
        }
        
        // Else we will calculate one from the current camera position
        // and the cursor position
        Vector2f cursor = getApplication().getInputManager().getCursorPosition();
        Vector3f near = cam.getWorldCoordinates(cursor, 0);
        Vector3f far = cam.getWorldCoordinates(cursor, 1);
        Vector3f dir = far.subtractLocal(near).normalizeLocal();
        
        calculatedFocalPoint.set(cam.getLocation());
        calculatedFocalPoint.addLocal(dir.mult(10));
        return calculatedFocalPoint;
    }
 
    private class FocalPointListener extends DefaultCursorListener {
    
        protected void setFocalPoint( CursorMotionEvent event ) {
            if( event.getCollision() != null ) {
                focalPoint = event.getCollision().getContactPoint(); 
            } else {
                focalPoint = null;
            }
        }
    
        @Override       
        public void cursorMoved( CursorMotionEvent event, Spatial target, Spatial capture ) {
            setFocalPoint(event);
        }
 
        @Override
        public void cursorExited( CursorMotionEvent event, Spatial target, Spatial capture ) {
            setFocalPoint(event);
        } 

        @Override
        public void cursorEntered( CursorMotionEvent event, Spatial target, Spatial capture ) {
            setFocalPoint(event);
        } 
 
        @Override       
        public void cursorButtonEvent( CursorButtonEvent event, Spatial target, Spatial capture ) {
            // Have to override this method or the base class will consume the
            // button event and we don't want that... because then input handler won't 
            // get it.
        }
    } 
    
    private class InputHandler implements AnalogFunctionListener, StateFunctionListener {
 
        private Vector3f fp;
        private Quaternion flip = new Quaternion().fromAngles(0, FastMath.PI, 0);
 
        public void valueChanged( FunctionId func, InputState value, double tpf ) {
            if( func == F_FAST ) {
                if( value == InputState.Positive ) {
                    speed = fastSpeed;
                } else {
                    speed = normalSpeed;
                }
            } else if( func == F_ORBIT_ON ) {
                System.out.println("state change:" + func + "   value:" + value);
                if( value == InputState.Positive ) {
                    fp = getFocalPoint();
                }
            }
        }        
 
        public void valueActive( FunctionId func, double value, double tpf ) {
            System.out.println("Function:" + func + "  value:" + value + "  tpf:" + tpf + "  speed:" + speed);
 
            //System.out.println("Focal point:" + focalPoint);
            
            float amount = (float)(speed * value * tpf); 
            
            Vector3f translate = new Vector3f();
            
            if( func == F_ORBIT_X || func == F_ORBIT_Y ) {
 
                // Just do straight relative rotation based on x or y
                Quaternion locDelta;
                Quaternion camDelta;
                if( func == F_ORBIT_X ) {
                    // yaw
                    locDelta = new Quaternion().fromAngles(0, amount, 0);
                    camDelta = new Quaternion().fromAngles(0, amount, 0);  
                } else {
                    // pitch
                    locDelta = new Quaternion().fromAngles(amount, 0, 0); 
                    camDelta = new Quaternion().fromAngles(amount, 0, 0);  
                }

                Quaternion cameraRot = cam.getRotation();

                // For rotation of the camera location, we need to be looking
                // in the opposite direction of the camera.  Then these deltas
                // are relative to that rotation
                Quaternion orbitRot = cameraRot.mult(flip);
                
                // We need to find what world space rotation we'd need to
                // rotate the location.  For example, if the camera is moving
                // pitch up and down but the camera is facing along the X axis
                // then we need to be rotating in x,y and not z,y (as would
                // be normal for local pitch).
                // This requires a change of basis.
                Quaternion worldLocDelta = cameraRot.mult(locDelta);                
                worldLocDelta = worldLocDelta.mult(cameraRot.inverse());
                                                
                Vector3f relative = cam.getLocation().subtract(fp);
                relative = worldLocDelta.mult(relative);
                cam.setLocation(fp.add(relative));
 
                cameraRot = cameraRot.mult(camDelta);
                cam.setRotation(cameraRot);
            } else if( func == F_MOVE ) {
                translate.addLocal(cam.getDirection().mult(amount));
            } else if( func == F_STRAFE ) {
                translate.addLocal(cam.getLeft().mult(-amount));
            } else if( func == F_ELEVATION ) {
                translate.addLocal(cam.getUp().mult(amount));
            }
            if( translate.lengthSquared() != 0 ) {
                cam.setLocation(cam.getLocation().addLocal(translate));
            }           
        }     
    }   
}
