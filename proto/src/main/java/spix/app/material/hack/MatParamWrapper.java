package spix.app.material.hack;

import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.shader.VarType;
import com.jme3.texture.image.ColorSpace;

import java.lang.reflect.Field;

/**
 * Created by Nehon on 09/07/2017.
 */
public class MatParamWrapper {

    private MatParam matParam;

    public MatParamWrapper(MatParam matParam) {
        this.matParam = matParam;
    }

    public MatParam getMatParam() {
        return matParam;
    }

    public String getName() {
        return matParam.getName();
    }

    public void setName(String name) {
        //here is the first hack... we use reflection to set the value.
        try {
            Field nameField = matParam.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(matParam, name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public VarType getVarType() {
        return matParam.getVarType();
    }

    public void setVarType(VarType type) {
        try {
            Field typeField = matParam.getClass().getDeclaredField("type");
            typeField.setAccessible(true);
            typeField.set(matParam, type);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public ColorSpace getColorSpace() {
        if (matParam instanceof MatParamTexture) {
            return ((MatParamTexture) matParam).getColorSpace();
        }
        return null;
    }

    public void setColorSpace(ColorSpace colorSpace) {
        if (matParam instanceof MatParamTexture) {
            ((MatParamTexture) matParam).setColorSpace(colorSpace);
        }
    }

}
