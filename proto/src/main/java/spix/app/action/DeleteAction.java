package spix.app.action;

import com.google.common.base.*;
import com.jme3.scene.*;
import spix.app.DefaultConstants;
import spix.app.light.LightWrapper;
import spix.core.*;
import spix.ui.MessageRequester;
import spix.undo.UndoManager;
import spix.undo.edit.*;

import static spix.app.DefaultConstants.SCENE_ROOT;

/**
 * The delete action
 * Created by Nehon on 29/12/2016.
 */
public class DeleteAction extends AbstractAction {

    public DeleteAction(Spix spix) {
        super("Delete", "Delete", "pressed DELETE");
        //we want this menu available only for LightWrappers and spatials, and we don't want users to be able to delete the root of the scene.
        spix.getBlackboard().bind("main.selection",
            this, "enabled", new Predicate<Object>() {
                @Override
                public boolean apply(Object input) {
                    SelectionModel selection = (SelectionModel) input;
                    Object sceneRoot = spix.getBlackboard().get(SCENE_ROOT);
                    for (Object o : selection) {
                        if(o == sceneRoot){
                            return false;
                        }
                        if(o == null){
                            continue;
                        }

                        if(Spatial.class.isAssignableFrom(o.getClass()) ||
                            LightWrapper.class.isAssignableFrom(o.getClass())){
                            return true;
                        }
                    }
                    return false;
                }
            });
        setEnabled(false);
    }

    @Override
    public void performAction(Spix spix) {
        SelectionModel selection = spix.getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        spix.getService(MessageRequester.class).confirm("Delete Selection", "Do you want to delete the selected element" + (selection.size()>1?"s?":"?"), new RequestCallback<Boolean>() {
            @Override
            public void done(Boolean confirm) {
                if (confirm) {
                    UndoManager um = spix.getService(UndoManager.class);
                    for (Object o : selection) {
                        if (o instanceof Spatial) {
                            Spatial s = (Spatial) o;
                            Spatial parent = s.getParent();
                            s.removeFromParent();
                            um.addEdit(new SpatialRemoveEdit((Node) parent, s));
                            //select the parent element
                            selection.setSingleSelection(parent);
                        } else if (o instanceof LightWrapper) {
                            LightWrapper lw = (LightWrapper) o;
                            lw.getParent().removeLight(lw.getLight());
                            um.addEdit(new LightRemoveEdit(lw.getParent(), lw.getLight()));
                        }
                    }
                }
            }
        });
    }
}
