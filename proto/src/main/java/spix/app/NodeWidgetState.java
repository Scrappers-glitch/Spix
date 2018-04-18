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
import com.jme3.material.*;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.*;
import spix.app.utils.ShapeUtils;
import spix.core.*;
import spix.undo.*;
import spix.undo.edit.*;

import java.util.*;

/**
 * Draws a translation widget over the currently selected object(s)
 * and allows dragging them using various manipulations of the widget.
 *
 * @author Paul Speed
 */
public class NodeWidgetState extends BaseAppState {



    private Camera cam;
    private Node nodesNode;
    private SelectionModel selection;
    private NodeSelectionDispatcher selectionDispatcher = new NodeSelectionDispatcher();
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

        recurseAddNodes(rootNode);
    }

    public void setScene(Spatial n) {
        clear();
        recurseAddNodes(n);
    }

    private void clear() {
        nodesNode.detachAllChildren();
        nodeMap.clear();
    }

    private void recurseAddNodes(Spatial spatial) {
        if (spatial instanceof Node) {
            Node n = (Node) spatial;
            addNode(n);

            for (Spatial s : n.getChildren()) {
                if (s instanceof Node) {
                    recurseAddNodes(s);
                }
            }
        }
    }

    private void addNode(Node node) {
        Node widget = new Node(node.getName() + " Widget");
        float handleSize = 0.08f;

        Box b = new Box(handleSize, handleSize, handleSize);
        Geometry g = new Geometry("geom", b);

        Material m = GuiGlobals.getInstance().createMaterial(false).getMaterial();
        ColorRGBA color = new ColorRGBA(0, 0.8f, 0, 0.5f);
        m.setColor("Color", color);
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        g.setMaterial(m);
        g.setQueueBucket(RenderQueue.Bucket.Transparent);
        widget.attachChild(g);

        Geometry g2 = ShapeUtils.makeNodeHintShape2(node.getName() + "hint Geom", color, handleSize);
        Material m2 = GuiGlobals.getInstance().createMaterial(false).getMaterial();
        m2.setBoolean("VertexColor", true);
        m2.setColor("Color", new ColorRGBA(1, 1, 1, 1));
        m2.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        m2.getAdditionalRenderState().setWireframe(true);
        g2.setMaterial(m2);

        widget.attachChild(g2);

        updateWidgetPosition(widget, node);

        nodesNode.attachChild(widget);
        nodeMap.put(node, widget);
    }

    private void recurseRemoveNode(Node n) {
        removeNode(n);
        for (Spatial spatial : n.getChildren()) {
            if (spatial instanceof Node) {
                Node child = (Node) spatial;
                recurseRemoveNode(child);
            }
        }
    }

    private void removeNode(Node node ){
        Spatial widget = nodeMap.remove(node);
        if(widget != null){
            nodesNode.detachChild(widget);
        }
    }

    private void updateWidgetPosition( Node widget, Node node) {
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
        CursorEventControl.addListenersToSpatial(nodesNode, selectionDispatcher);
        getSpix().getBlackboard().bind(DefaultConstants.SCENE_ROOT, this, "scene");
        getSpix().getBlackboard().bind(UndoManager.LAST_EDIT, this, "lastEdit");
    }

    @Override
    protected void onDisable() {
        nodesNode.removeFromParent();
        CursorEventControl.removeListenersFromSpatial(nodesNode, selectionDispatcher);
        getSpix().getBlackboard().unbind(DefaultConstants.SCENE_ROOT, this, "scene");
        getSpix().getBlackboard().bind(UndoManager.LAST_EDIT, this, "lastEdit");
    }

    public void setLastEdit(Edit edit){
        if(edit instanceof CompositeEdit){
            CompositeEdit ce = (CompositeEdit)edit;
            SpatialAddEdit nodeEdit = ce.getEditWithType(SpatialAddEdit.class);
            if( nodeEdit!= null && nodeEdit.getChild() instanceof Node){
                Node node = (Node) nodeEdit.getChild();
                if(nodeEdit instanceof SpatialRemoveEdit) {
                    if(nodeEdit.isDone()) {
                        recurseRemoveNode(node);
                    } else {
                        addNode(node);
                    }
                } else {
                    if(nodeEdit.isDone()) {
                        addNode(node);
                    } else {
                        recurseRemoveNode(node);
                    }
                }
            }
        }
    }


    @Override
    public void update(float tpf) {
        Node rootNode = ((SimpleApplication) getApplication()).getRootNode();
        updateNodes(rootNode);
    }

    public void updateNodes(Node node) {
        Node widget = nodeMap.get(node);
        if(widget != null) {
           updateWidgetPosition(widget, node);
        }

        for (Spatial spatial : node.getChildren()) {
            if(spatial instanceof Node){
                updateNodes((Node)spatial);
            }
        }

    }

    @Override
    public void render(RenderManager renderManager) {
    }

    private class NodeSelectionDispatcher implements CursorListener {

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

}
