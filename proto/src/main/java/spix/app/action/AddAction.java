package spix.app.action;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import spix.app.SpixState;
import spix.core.AbstractAction;
import spix.core.SelectionModel;
import spix.core.Spix;

import static spix.app.DefaultConstants.SCENE_ROOT;
import static spix.app.DefaultConstants.SELECTION_PROPERTY;

/**
 * Created by nehon on 28/12/16.
 */
public abstract class AddAction extends AbstractAction{

    public AddAction(String id){
        super(id);
    }

    protected Node getAncestorNode(Spix spix){
        Spatial selected = getSelectedSpatial(spix);
        Spatial anchor;
        Spatial spatial = spix.getBlackboard().get(SCENE_ROOT, Spatial.class);
        if (selected != null) {
            anchor = selected;
            while (anchor != spatial && !(anchor instanceof Node)) {
                anchor = anchor.getParent();
            }
        } else {
            anchor = spatial;
        }
        return (Node)anchor;
    }

    protected Spatial getSelectedSpatial(Spix spix){
        SelectionModel model = spix.getBlackboard().get(SELECTION_PROPERTY, SelectionModel.class);
        Object obj = model.getSingleSelection();
        if (obj != null && obj instanceof Spatial) {
            return (Spatial) obj;
        }
        return null;
    }
}
