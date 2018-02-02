package spix.app;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.CursorButtonEvent;
import com.simsilica.lemur.event.CursorEventControl;
import com.simsilica.lemur.event.CursorListener;
import com.simsilica.lemur.event.CursorMotionEvent;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.input.InputState;
import com.simsilica.lemur.input.StateFunctionListener;
import spix.core.SelectionModel;

/**
 * Created by nehon on 30/07/17.
 */
public class SelectionAppState extends BaseAppState {

    private static final String GROUP = "SELECTION";
    private static final FunctionId F_CONTROL = new FunctionId(GROUP, "CTRL_MODIFIER");
    private boolean controlModifier = false;
    private boolean cancelNext = false;

    @Override
    protected void initialize(Application app) {
        Node rootNode = ((SimpleApplication)app).getRootNode();
        // Setup for some scene picking.
        CursorEventControl.addListenersToSpatial(rootNode, new CursorListener() {

            private CursorMotionEvent lastMotion;

            public void cursorButtonEvent(CursorButtonEvent event, Spatial target, Spatial capture) {
                //System.out.println("cursorButtonEvent(" + event + ", " + target + ", " + capture + ")");
                if (!event.isPressed() && event.getButtonIndex() != 2 && lastMotion != null && !cancelNext) {
                    // Set the selection
                    Geometry selected = null;
                    Vector3f contactPoint = null;
                    if (lastMotion.getCollision() != null) {
                        selected = lastMotion.getCollision().getGeometry();
                        contactPoint = lastMotion.getCollision().getContactPoint();
                    }
                    //System.out.println("Setting selection to:" + selected);
                    if (controlModifier) {
                        //multi select
                        getState(SpixState.class).getSpix().getBlackboard().get("main.selection", SelectionModel.class).addSelection(selected);
                    }else {
                        //single select
                        getState(SpixState.class).getSpix().getBlackboard().get("main.selection", SelectionModel.class).setSingleSelection(selected);
                    }
                    getState(SpixState.class).getSpix().getBlackboard().set("main.selection.contactpoint", contactPoint);
                }
                cancelNext = false;
            }

            public void cursorEntered(CursorMotionEvent event, Spatial target, Spatial capture) {
                // System.out.println("cursorEntered(" + event + ", " + target + ", " + capture + ")");
            }

            public void cursorExited(CursorMotionEvent event, Spatial target, Spatial capture) {
                //System.out.println("cursorExited(" + event + ", " + target + ", " + capture + ")");
            }

            public void cursorMoved(CursorMotionEvent event, Spatial target, Spatial capture) {
                //System.out.println("cursorMoved(" + event + ", " + target + ", " + capture + ")");
                this.lastMotion = event;
            }

        });

        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.map(F_CONTROL, KeyInput.KEY_LCONTROL);
        inputMapper.map(F_CONTROL, KeyInput.KEY_RCONTROL);
        inputMapper.addStateListener(new StateFunctionListener() {
            @Override
            public void valueChanged(FunctionId func, InputState value, double tpf) {
                controlModifier = value == InputState.Positive;
            }
        }, F_CONTROL);

        inputMapper.activateGroup(GROUP);
    }

    public void cancelNextEvent() {
        this.cancelNext = true;
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
