package spix.app.action;

import com.google.common.base.Predicate;
import com.jme3.scene.Spatial;
import spix.core.SelectionModel;

import java.util.Collection;

/**
 * Created by nehon on 01/08/17.
 */
public class ContainsOneOfType implements Predicate {

    private Class<?>[] types;

    public ContainsOneOfType(Class<?>... types) {
        this.types = types;
    }

    @Override
    public boolean apply(Object input) {
        if(!(input instanceof Collection)){
            return false;
        }
        Collection selection = (Collection)input;
        for (Object o : selection) {
            if(o == null){
                continue;
            }
            for (Class<?> type : types) {
                if(type.isAssignableFrom(o.getClass())){
                    return true;
                }
            }
        }
        return false;
    }
}
