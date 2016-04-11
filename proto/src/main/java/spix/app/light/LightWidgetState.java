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

package spix.app.light;

import com.jme3.app.*;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.scene.*;
import com.jme3.util.*;
//import javafx.scene.shape.Circle;
import com.simsilica.lemur.event.*;
import spix.app.*;
import spix.core.*;

import java.beans.*;


/**
 * Draws a translation widget over the currently selected object(s)
 * and allows dragging them using various manipulations of the widget.
 *
 * @author Paul Speed
 */
public class LightWidgetState extends BaseAppState {


    private Camera cam;
    private Node lightNode;
    private SafeArrayList<LightWrapper> wrappers = new SafeArrayList<>(LightWrapper.class);
    private SelectionModel selection;
    private SelectionObserver selectionObserver = new SelectionObserver();
    private LightSelectionDispatcher selectionDispatcher = new LightSelectionDispatcher();

    public LightWidgetState() {
    }

    protected Spix getSpix() {
        return getState(SpixState.class).getSpix();
    }

    protected Node getRoot() {
        return getState(DecoratorViewPortState.class).getRoot();
    }

    @Override
    protected void initialize(Application app) {
        Node rootNode = ((SimpleApplication) app).getRootNode();
        cam = app.getCamera();
        lightNode = new Node("Light Node");

        recurseAddLights(rootNode);
    }

    private void recurseAddLights(Node n){
        for (Light light : n.getLocalLightList()) {
            addLight(n, light);
        }
        for (Spatial s: n.getChildren()){
            if(s instanceof Node){
                recurseAddLights((Node)s);
            }
        }
    }

    public void addLight(Spatial parent, Light light) {
        Node widget = new Node(light.toString()+ " Widget");

        switch (light.getType()){
            case Directional:
                wrappers.add(new DirectionalLightWrapper(widget,(DirectionalLight)light, parent, getApplication().getAssetManager()));
                break;
            case Ambient:
                wrappers.add(new AmbientLightWrapper(widget,(AmbientLight) light, parent, getApplication().getAssetManager()));
                break;
            case Point:
                wrappers.add(new PointLightWrapper(widget,(PointLight) light, parent, getApplication().getAssetManager()));
                break;
            case Spot:
                wrappers.add(new SpotLightWrapper(widget,(SpotLight)light, parent, getApplication().getAssetManager()));
                break;
            default:
                widget.setLocalTranslation(Vector3f.ZERO);
                break;
        }

        lightNode.attachChild(widget);
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {
        getRoot().attachChild(lightNode);
        this.selection = getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        selection.addPropertyChangeListener(selectionObserver);
        CursorEventControl.addListenersToSpatial(lightNode, selectionDispatcher);
    }

    @Override
    protected void onDisable() {
        lightNode.removeFromParent();
        selection.removePropertyChangeListener(selectionObserver);
        CursorEventControl.removeListenersFromSpatial(lightNode, selectionDispatcher);
    }

    @Override
    public void update(float tpf) {
        for (LightWrapper wrapper: wrappers.getArray()){
            wrapper.update(tpf, cam);
        }
    }

    @Override
    public void render(RenderManager renderManager) {
    }

    private class LightSelectionDispatcher implements CursorListener{

        private CursorMotionEvent lastMotion;

        public void cursorButtonEvent( CursorButtonEvent event, Spatial target, Spatial capture ) {

            //It's important here that the event isPressed is false (on key up)
            //else there are some strange things with event orders
            if( !event.isPressed() && lastMotion != null ) {
                // Set the selection
                Geometry selected = null;
                if( lastMotion.getCollision() != null ) {
                    selected = lastMotion.getCollision().getGeometry();
                }
                //System.out.println("Setting selection to:" + selected +" " + capture +" " + target);
                if(selected == null){
                    return;
                }
                Spatial widget = findWidget(selected);
                getState(SpixState.class).getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class).setSingleSelection(getWrapper(widget));
            }
        }

        public void cursorEntered( CursorMotionEvent event, Spatial target, Spatial capture ) {
        }

        public void cursorExited( CursorMotionEvent event, Spatial target, Spatial capture ) {
        }

        public void cursorMoved( CursorMotionEvent event, Spatial target, Spatial capture ) {
            this.lastMotion = event;
        }

        private Spatial findWidget(Spatial s){
            if(s == null){
                return null;
            }
            if(s.getParent() == lightNode){
                return s;
            } else {
                return findWidget(s.getParent());
            }
        }
    }

    private LightWrapper getWrapper(Spatial widget){
        for(LightWrapper wrapper : wrappers.getArray()){
            if(wrapper.getWidget() == widget){
                return wrapper;
            }
        }
        return null;
    }

    //Light widget selection Highlighting is handled by listening to the selection change event.
    //The SelectionHighlightState only handles Spatials and I felt hackish to manage an exception for Light wrappers
    private class SelectionObserver implements PropertyChangeListener {

        public void propertyChange( PropertyChangeEvent event ) {
            if(event.getNewValue() != event.getOldValue() ){

                if(event.getNewValue() instanceof LightWrapper) {
                    Node widget = ((LightWrapper) event.getNewValue()).getWidget();
                    //TODO Perfect candidate for MPO, let's do it when we are on alpha 5
                    //for now we recurse and set the color on every material.
                    setColorRecurse(ColorRGBA.Orange, widget);
                }
                if(event.getOldValue() instanceof LightWrapper) {
                    Node oldWidget = ((LightWrapper) event.getOldValue()).getWidget();
                    setColorRecurse(ColorRGBA.Black, oldWidget);
                }
            }

        }
    }

    private void setColorRecurse(ColorRGBA color, Node node){
        for(Spatial s:node.getChildren()){
            if(s instanceof Geometry){
                Material m = ((Geometry)s).getMaterial();
                if (m.getMaterialDef().getMaterialParam("Color") != null) {
                    m.setColor("Color", color);
                }
            } else if(s instanceof  Node){
                setColorRecurse(color,(Node)s);
            }
        }
    }
}
