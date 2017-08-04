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

        SelectionModel selection = getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        for (Object selected : selection) {
            if(!(selected instanceof Spatial)){
                continue;
            }
            Spatial spatial = (Spatial)selected;
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
            selections.add(clone);
        }
        updateSelection();
        getState(TransformState.class).translate();
    }

    private void updateSelection(){
        if(selections.isEmpty()){
            return;
        }
        SelectionModel selection = getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        selection.clear();
        selection.addAllSelection(selections);
       /* for (Spatial spatial : selections) {
            selection.addSelection(spatial);
        }*/
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
