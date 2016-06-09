package spix.app.material.hack;

import com.jme3.material.MaterialDef;

import java.lang.reflect.Field;

/**
 * Created by Nehon on 06/06/2016.
 * This class is a hack over MaterialDef to be able to use the missing accessors in the class.
 * This can be removed once we use 3.2
 */
public class MatDefWrapper {


    private MaterialDef materialDef;

    public MatDefWrapper(MaterialDef materialDef) {
        this.materialDef = materialDef;
    }

    public MaterialDef getMaterialDef() {
        return materialDef;
    }

    public String getName(){
        return materialDef.getName();
    }

    public void setName(String name){
        //here is the first hack... we use reflection to set the value.
        try {
            Field nameField =  materialDef.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(materialDef, name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
