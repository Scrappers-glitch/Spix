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
import com.jme3.bounding.*;
import com.jme3.light.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.util.SafeArrayList;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.*;
import spix.app.light.*;
import spix.app.utils.ShapeUtils;
import spix.core.*;

import java.beans.*;
import java.util.*;

import static com.jme3.light.Light.Type.Ambient;
import static com.jme3.light.Light.Type.Directional;
import static com.jme3.light.Light.Type.Spot;

//import javafx.scene.shape.Circle;


/**
 * Draws a translation widget over the currently selected object(s)
 * and allows dragging them using various manipulations of the widget.
 *
 * @author Paul Speed
 */
public class NodeWidgetState extends BaseAppState {


    public static final float NODE_INDENT = 0.1f;
    private Camera cam;
    private Node nodesNode;
    private SelectionModel selection;
    private SelectionObserver selectionObserver = new SelectionObserver();
    private LightSelectionDispatcher selectionDispatcher = new LightSelectionDispatcher();
    private Map<Node, Node> nodeMap = new HashMap<>();

    public NodeWidgetState() {
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
        nodesNode = new Node("Nodes Node");

        recurseAddNodes(rootNode, 1);
    }

    private void recurseAddNodes(Node n, int indent){
        if(nodesNode.getParent() != null) {
            addNode(n, indent);
        }

        for (Spatial s: n.getChildren()){
            if(s instanceof Node){
                recurseAddNodes((Node)s, indent + 1);
            }
        }
    }

    public void addNode(Node node) {
        Node parent = node.getParent();
        int indent = 1;
        while (parent != null){
            indent ++;
            parent = parent.getParent();
        }
        addNode(node, indent);
    }


    private void addNode(Node node, int indent) {
        Node widget = new Node(node.getName()+ " Widget");
        float indentVal = NODE_INDENT * indent;
        BoundingVolume bv =node.getWorldBound();
        float handleSize = 0.08f;
        if(bv instanceof BoundingBox){


            Box b = new Box(handleSize,handleSize,handleSize);
            Geometry g = new Geometry("geom",b);

            Material m = GuiGlobals.getInstance().createMaterial(false).getMaterial();
            ColorRGBA color = new ColorRGBA(0,0.8f,0,0.5f);
            m.setColor("Color", color);
            m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            g.setMaterial(m);
            g.setQueueBucket(RenderQueue.Bucket.Transparent);
            widget.attachChild(g);

            Geometry g2 = ShapeUtils.makeNodeHintShape2(node.getName() + "hint Geom", color,handleSize );
            Material m2 = GuiGlobals.getInstance().createMaterial(false).getMaterial();
            m2.setBoolean("VertexColor", true);
            m2.setColor("Color", new ColorRGBA(1,1,1,1));
            m2.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            m2.getAdditionalRenderState().setWireframe(true);
            g2.setMaterial(m2);

            widget.attachChild(g2);

            updateWidgetPosition(indentVal, (BoundingBox) bv, widget, node);

            nodesNode.attachChild(widget);

            nodeMap.put(node, widget);
        }

    }

    private void updateWidgetPosition(float indentVal, BoundingBox bv, Node widget, Node node) {
        //BoundingBox bb = bv;
//        Spatial g = widget.getChild(0);
//        Spatial g2 = widget.getChild(1);
        //g.setLocalTranslation(bb.getCenter().add(bb.getXExtent(),bb.getYExtent(),bb.getZExtent()).addLocal(indentVal,indentVal,indentVal));
//        g.setLocalTranslation(bb.getCenter().add(bb.getXExtent(),bb.getYExtent(),bb.getZExtent()).addLocal(indentVal,indentVal,indentVal));
//        g2.setLocalTranslation(g.getLocalTranslation());

        widget.setLocalTranslation(node.getWorldTranslation());
        widget.setLocalRotation(node.getWorldRotation());
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {
        getRoot().attachChild(nodesNode);
        this.selection = getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        selection.addPropertyChangeListener(selectionObserver);
        CursorEventControl.addListenersToSpatial(nodesNode, selectionDispatcher);
    }

    @Override
    protected void onDisable() {
        nodesNode.removeFromParent();
        selection.removePropertyChangeListener(selectionObserver);
        CursorEventControl.removeListenersFromSpatial(nodesNode, selectionDispatcher);
    }

    @Override
    public void update(float tpf) {
        Node rootNode = ((SimpleApplication) getApplication()).getRootNode();
        updateNodes(rootNode, 1);
    }

    public void updateNodes(Node node, int indent) {
        Node widget = nodeMap.get(node);
        if(node.getParent() != null) {
            if (widget == null) {
                addNode(node, indent);
            } else {
                BoundingVolume bv = node.getWorldBound();
                if (bv instanceof BoundingBox) {
                    updateWidgetPosition(indent * NODE_INDENT, (BoundingBox) bv, widget, node);
                }
            }
        }

        for (Spatial spatial : node.getChildren()) {
            if(spatial instanceof Node){
                updateNodes((Node)spatial, indent ++);
            }
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

                System.out.println("Setting selection to:" + selected +" " + capture +" " + target);
                if(selected == null){
                    return;
                }
                Spatial widget = findWidget(selected);
                getState(SpixState.class).getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class).setSingleSelection(getNode(widget));
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
            if(s.getParent() == nodesNode){
                return s;
            } else {
                return findWidget(s.getParent());
            }
        }

        private Node getNode(Spatial widget){
            for (Node node : nodeMap.keySet()) {
                if(nodeMap.get(node) == widget){
                    return node;
                }
            }
            return null;
        }
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
