/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.app;

import com.jme3.anim.*;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.MatParamOverride;
import com.jme3.math.*;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.*;
import com.jme3.scene.debug.custom.ArmatureDebugger;
import com.jme3.shader.VarType;
import spix.app.light.LightWrapper;
import spix.core.SelectionModel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * @author Nehon
 */
public class ArmatureSelectionState extends BaseAppState {

    public static final float CLICK_MAX_DELAY = 0.2f;
    private Node debugNode = new Node("debugNode");
    private Map<Armature, ArmatureDebugger> armatures = new HashMap<>();
    //private Map<Armature, Joint> selectedBones = new HashMap<>();
    private Application app;
    private boolean displayAllJoints = true;
    private float clickDelay = -1;
    private SelectionObserver selectionObserver = new SelectionObserver();
    private SceneGraphVisitorAdapter xRayVisitor;
    private boolean xRay = true;

    public ArmatureSelectionState() {
        xRayVisitor = new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                geom.getMaterial().getAdditionalRenderState().setDepthTest(!ArmatureSelectionState.this.xRay);
            }
        };
    }

    @Override
    protected void initialize(Application app) {
        this.app = app;
        for (ArmatureDebugger armatureDebugger : armatures.values()) {
            armatureDebugger.initialize(app.getAssetManager(), app.getCamera());
        }
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        Node root = getState(DecoratorViewPortState.class).getRoot();
        root.attachChild(debugNode);
        SelectionModel selection =  getState(SpixState.class).getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        selection.addPropertyChangeListener(selectionObserver);
    }

    @Override
    protected void onDisable() {
        debugNode.removeFromParent();
        SelectionModel selection =  getState(SpixState.class).getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        selection.removePropertyChangeListener(selectionObserver);
    }

    @Override
    public void update(float tpf) {
//        if (clickDelay > -1) {
//            clickDelay += tpf;
//        }
    }

    public ArmatureDebugger addArmatureFrom(SkinningControl skinningControl) {
        Armature armature = skinningControl.getArmature();
        Spatial forSpatial = skinningControl.getSpatial();
        return addArmatureFrom(armature, forSpatial);
    }

    public void setXRay(boolean xRay){
        this.xRay = xRay;
        for (ArmatureDebugger armatureDebugger : armatures.values()) {
            armatureDebugger.depthFirstTraversal(xRayVisitor);
        }
    }

    public ArmatureDebugger addArmatureFrom(Armature armature, Spatial forSpatial) {

        ArmatureDebugger ad = armatures.get(armature);
        if(ad != null){
            return ad;
        }

        JointInfoVisitor visitor = new JointInfoVisitor(armature);
        forSpatial.depthFirstTraversal(visitor);

        ad = new ArmatureDebugger(forSpatial.getName() + "_Armature", armature, visitor.deformingJoints);
        ad.setLocalTransform(forSpatial.getWorldTransform());
        if (forSpatial instanceof Node) {
            List<Geometry> geoms = new ArrayList<>();
            findGeoms((Node) forSpatial, geoms);
            if (geoms.size() == 1) {
                ad.setLocalTransform(geoms.get(0).getWorldTransform());
            }
        }
        armatures.put(armature, ad);
        debugNode.attachChild(ad);

        if (isInitialized()) {
            ad.initialize(app.getAssetManager(), app.getCamera());
        }
        ad.depthFirstTraversal(xRayVisitor);
        ad.displayNonDeformingJoint(displayAllJoints);
        return ad;
    }

    private void findGeoms(Node node, List<Geometry> geoms) {
        for (Spatial spatial : node.getChildren()) {
            if (spatial instanceof Geometry) {
                geoms.add((Geometry) spatial);
            } else if (spatial instanceof Node) {
                findGeoms((Node) spatial, geoms);
            }
        }
    }

    public boolean isxRay() {
        return xRay;
    }

    //    public Map<Skeleton, Bone> getSelectedBones() {
//        return selectedBones;
//    }

    private class JointInfoVisitor extends SceneGraphVisitorAdapter {

        List<Joint> deformingJoints = new ArrayList<>();
        Armature armature;

        public JointInfoVisitor(Armature armature) {
            this.armature = armature;
        }

        @Override
        public void visit(Geometry g) {
            for (Joint joint : armature.getJointList()) {
                if (g.getMesh().isAnimatedByJoint(armature.getJointIndex(joint))) {
                    deformingJoints.add(joint);
                }
            }
        }
    }

    private class SelectionObserver implements PropertyChangeListener {

        public void propertyChange( PropertyChangeEvent event ) {
            if(event.getNewValue() != event.getOldValue() ){
                if(event.getNewValue() instanceof SkinningControl) {
                    ArmatureDebugger ad = addArmatureFrom((SkinningControl)event.getNewValue());
                    debugNode.attachChild(ad);
                }
                if(event.getOldValue() instanceof SkinningControl) {
                    ArmatureDebugger ad = armatures.get(((SkinningControl)event.getOldValue()).getArmature());
                    if(ad != null){
                        ad.removeFromParent();
                    }
                }
            }

        }
    }
}
