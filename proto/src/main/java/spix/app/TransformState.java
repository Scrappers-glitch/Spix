package spix.app;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
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
    private static final FunctionId F_NOOP = new FunctionId(GROUP, "NOOP");
    private float timer = -1;
    private float delay = 0.1f; //in seconds

    @Override
    protected void initialize(Application app) {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.map(F_GRAB, KeyInput.KEY_G);
        inputMapper.map(F_SCALE, KeyInput.KEY_S);
        inputMapper.map(F_ROTATE, KeyInput.KEY_R);

        //This is some hack to avoid the SCALE modifier to be triggered when hitting ctrl+S or meta+S to save a file.
        inputMapper.map(F_NOOP, KeyInput.KEY_LCONTROL);
        inputMapper.map(F_NOOP, KeyInput.KEY_LMETA);
        inputMapper.map(F_NOOP, KeyInput.KEY_RCONTROL);
        inputMapper.map(F_NOOP, KeyInput.KEY_RMETA);

        inputMapper.addStateListener(new StateFunctionListener() {
            @Override
            public void valueChanged(FunctionId func, InputState value, double tpf) {
                if (timer >= 0) {
                    return;
                }
                if(func == F_GRAB && value == InputState.Positive){
                    translate();
                } else if(func == F_SCALE && value == InputState.Positive){
                    scale();
                } else if(func == F_ROTATE && value == InputState.Positive){
                    rotate();
                }
            }
        }, F_GRAB, F_SCALE, F_ROTATE);

        inputMapper.addAnalogListener(new AnalogFunctionListener() {
            @Override
            public void valueActive(FunctionId func, double value, double tpf) {
                if (func == F_NOOP) {
                    timer = 0;
                    return;
                }
            }
        }, F_NOOP);

    }

    public void rotate() {
        getState(SpixState.class).getSpix().getBlackboard().set("transform.mode", "rotate");
        getState(RotationWidgetState.class).startKeyTransform();
    }

    public void scale() {
        getState(SpixState.class).getSpix().getBlackboard().set("transform.mode", "scale");
        getState(ScaleWidgetState.class).startKeyTransform();
    }

    public void translate() {
        getState(SpixState.class).getSpix().getBlackboard().set("transform.mode", "translate");
        getState(TranslationWidgetState.class).startKeyTransform();
    }

    @Override
    public void update(float tpf) {
        if (timer >= 0) {
            timer += tpf;
            if (timer > delay) {
                timer = -1;
            }
        }
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
