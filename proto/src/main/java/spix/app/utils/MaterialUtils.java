package spix.app.utils;

import com.jme3.material.MatParam;
import com.jme3.material.Material;

import java.lang.reflect.Field;

/**
 * Created by Nehon on 23/05/2017.
 */
public class MaterialUtils {

    public static void copyParamsAndRenderStates(Material from, Material to) {
        for (MatParam matParam : from.getParams()) {
            if (to.getMaterialDef().getMaterialParam(matParam.getName()) != null) {
                to.setParam(matParam.getName(), matParam.getVarType(), matParam.getValue());
            }
        }

        to.getAdditionalRenderState().set(from.getAdditionalRenderState());

    }
}
