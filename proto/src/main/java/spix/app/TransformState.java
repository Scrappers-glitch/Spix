package spix.app;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.math.Vector2f;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.*;

/**
 * Created by Nehon on 13/04/2016.
 */
public class TransformState extends BaseAppState {

    private static final String GROUP = "Transform State";

    private static final FunctionId F_GRAB = new FunctionId(GROUP, "Grab");
    private static final FunctionId F_SCALE = new FunctionId(GROUP, "Scale");
    private static final FunctionId F_ROTATE = new FunctionId(GROUP, "Rotate");

    @Override
    protected void initialize(Application app) {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.map(F_GRAB, KeyInput.KEY_G);
        inputMapper.map(F_SCALE, KeyInput.KEY_S);
        inputMapper.map(F_ROTATE, KeyInput.KEY_R);

        inputMapper.addStateListener(new StateFunctionListener() {
            @Override
            public void valueChanged(FunctionId func, InputState value, double tpf) {
                if(func == F_GRAB && value == InputState.Positive){
                    getState(SpixState.class).getSpix().getBlackboard().set("transform.mode", "translate");
                    getState(TranslationWidgetState.class).startKeyTransform();
                } else if(func == F_SCALE && value == InputState.Positive){
                    getState(SpixState.class).getSpix().getBlackboard().set("transform.mode", "scale");
                    getState(ScaleWidgetState.class).startKeyTransform();
                } else if(func == F_ROTATE && value == InputState.Positive){
                    //Doesn't exist yet
//                    getState(SpixState.class).getSpix().getBlackboard().set("transform.mode", "rotate");
//                    getState(RotationWidgetState.class);
                }
            }
        },F_GRAB, F_SCALE, F_ROTATE);

    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.activateGroup(GROUP);
    }

    @Override
    protected void onDisable() {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.deactivateGroup(GROUP);
    }
}
