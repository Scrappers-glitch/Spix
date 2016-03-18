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
import com.jme3.input.KeyInput;
import com.jme3.math.*;
import com.jme3.renderer.Camera;

import com.simsilica.lemur.GuiGlobals;
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
    public static final FunctionId F_MOVE = new FunctionId(GROUP, "Move"); 
    public static final FunctionId F_STRAFE = new FunctionId(GROUP, "Strafe"); 
    public static final FunctionId F_ELEVATION = new FunctionId(GROUP, "Elevation"); 
    public static final FunctionId F_FAST = new FunctionId(GROUP, "Fast"); 

    private Camera cam;
    private InputHandler inputHandler = new InputHandler();
    private float normalSpeed = 5;
    private float fastSpeed = 20;
    private float speed = normalSpeed; 
    
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
        inputMapper.addStateListener(inputHandler, F_FAST);
        
        // See if there are already mappings for these functions.
        if( !inputMapper.hasMappings(F_ORBIT_X) ) {
            System.out.println("Initializing default mappings for:" + F_ORBIT_X);
            inputMapper.map(F_ORBIT_X, Axis.MOUSE_X, Button.MOUSE_BUTTON2); 
        }
        if( !inputMapper.hasMappings(F_ORBIT_Y) ) {
            System.out.println("Initializing default mappings for:" + F_ORBIT_Y);
            inputMapper.map(F_ORBIT_Y, Axis.MOUSE_Y, Button.MOUSE_BUTTON2); 
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
            inputMapper.map(F_STRAFE, KeyInput.KEY_D); 
            inputMapper.map(F_STRAFE, InputState.Negative, KeyInput.KEY_A); 
        }
        if( !inputMapper.hasMappings(F_FAST) ) {
            System.out.println("Initializing default mappings for:" + F_FAST);
            inputMapper.map(F_FAST, KeyInput.KEY_LSHIFT);
            inputMapper.map(F_FAST, KeyInput.KEY_RSHIFT);
        }
        
        System.out.println("Input functions:" + inputMapper.getFunctionIds());
        
        System.out.println("Show mappings:" + inputMapper.getMappings(F_ORBIT_X));
    }
    
    @Override   
    protected void cleanup( Application app ) {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.removeAnalogListener(inputHandler, F_ORBIT_X, F_ORBIT_Y, F_MOVE, F_STRAFE, F_ELEVATION);
        inputMapper.removeStateListener(inputHandler, F_FAST);
    }
    
    @Override   
    protected void onEnable() {
        System.out.println(getClass().getName() + " Enabled");
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.activateGroup(GROUP);
    }
    
    @Override   
    protected void onDisable() {
        System.out.println(getClass().getName() + " Disabled");
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.deactivateGroup(GROUP);
    }
    
    private class InputHandler implements AnalogFunctionListener, StateFunctionListener {
 
        public void valueChanged( FunctionId func, InputState value, double tpf ) {
            if( func == F_FAST ) {
                if( value == InputState.Positive ) {
                    speed = fastSpeed;
                } else {
                    speed = normalSpeed;
                }
            }
        }        
    
        public void valueActive( FunctionId func, double value, double tpf ) {
            System.out.println("Function:" + func + "  value:" + value + "  tpf:" + tpf + "  speed:" + speed);
            
            float amount = (float)(speed * value * tpf); 
            
            Vector3f translate = new Vector3f();
            if( func == F_MOVE ) {
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
