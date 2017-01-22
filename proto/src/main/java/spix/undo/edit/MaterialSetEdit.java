package spix.undo.edit;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import spix.app.DefaultConstants;
import spix.core.SelectionModel;
import spix.core.Spix;
import spix.undo.SceneGraphStructureEdit;

/**
 * Created by Nehon on 22/01/2017.
 */
public class MaterialSetEdit implements SceneGraphStructureEdit {

    private Geometry geom;
    private Material oldMat;
    private Material newMat;

    public MaterialSetEdit(Geometry geom, Material oldMat, Material newMat) {
        this.geom = geom;
        this.oldMat = oldMat;
        this.newMat = newMat;
    }

    @Override
    public void undo(Spix spix) {
        geom.setMaterial(oldMat);
        refreshSelection(spix);
    }

    @Override
    public void redo(Spix spix) {
        geom.setMaterial(newMat);
        refreshSelection(spix);
    }

    public void refreshSelection(Spix spix) {
        spix.refresh(geom);
        SelectionModel model = spix.getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        Object selection = model.getSingleSelection();
        if (selection == geom) {
            model.setSingleSelection(null);
            model.setSingleSelection(geom);
        }
    }
}
