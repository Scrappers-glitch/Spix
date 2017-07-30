package spix.app;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Spatial;
import spix.core.SelectionModel;
import spix.core.Spix;
import spix.undo.UndoManager;
import spix.undo.edit.SpatialAddEdit;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nehon on 27/07/17.
 */
public class DuplicateSelectionAppState extends BaseAppState{


    List<Spatial> selections = new ArrayList<>();

    @Override
    protected void initialize(Application app) {

    }

    public Spix getSpix(){
        return getState(SpixState.class).getSpix();
    }

    public void duplicate(boolean cloneMaterial, boolean deepClone){
        List<Spatial> spatials = getSelection();
        for (Spatial spatial : spatials) {
            Spatial clone = null;
            if(deepClone){
                clone = spatial.deepClone();
            } else {
                clone = spatial.clone(cloneMaterial);
            }
            if(spatial.getParent()!=null){

                spatial.getParent().attachChild(clone);
                SpatialAddEdit edit = new SpatialAddEdit(spatial.getParent(), clone);
                getSpix().getService(UndoManager.class).addEdit(edit);
            }
            updateSelection(clone);
            getState(TransformState.class).translate();
        }
    }

    private List<Spatial> getSelection() {
        selections.clear();
        SelectionModel selection = getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        Object obj = selection.getSingleSelection();
        if( obj instanceof Spatial){
            selections.add((Spatial)obj);
        }
        return selections;
    }
    private void updateSelection(Spatial s){
        SelectionModel selection = getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        selection.setSingleSelection(s);
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
