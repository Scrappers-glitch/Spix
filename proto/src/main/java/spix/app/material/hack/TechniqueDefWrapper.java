package spix.app.material.hack;

import com.jme3.material.TechniqueDef;
import com.jme3.shader.ShaderNode;

import java.lang.reflect.Field;

/**
 * Created by Nehon on 06/06/2016.
 * This class is a hack over TechniqueDef to be able to use the missing accessors in the class.
 * This can be removed once we use 3.2
 */
public class TechniqueDefWrapper {

    private TechniqueDef techDef;

    public TechniqueDefWrapper(TechniqueDef techDef) {
        this.techDef = techDef;
    }

    public void setTechDef(TechniqueDef techDef) {
        this.techDef = techDef;
    }

    public TechniqueDef getTechDef() {
        return techDef;
    }

    public String getName(){
        return techDef.getName();
    }

    public void setName(String name){
        //here is the first hack... we use reflection to set the value.
        try {
            Field nameField =  techDef.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(techDef, name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public boolean isNoRender(){
        return techDef.isNoRender();
    }

    public void setNoRender(boolean noRender){
        techDef.setNoRender(noRender);
    }

    public TechniqueDef.LightMode getLightMode(){
        return techDef.getLightMode();
    }

    public void setLightMode(TechniqueDef.LightMode mode){
        techDef.setLightMode(mode);
    }

    public TechniqueDef.ShadowMode getShadowMode(){
        return techDef.getShadowMode();
    }

    public void setShadowMode(TechniqueDef.ShadowMode mode){
        techDef.setShadowMode(mode);
    }

}
