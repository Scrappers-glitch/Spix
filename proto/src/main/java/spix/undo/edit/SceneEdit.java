package spix.undo.edit;

import spix.undo.CompositeEdit;
import spix.undo.Edit;
import spix.undo.SceneGraphStructureEdit;

/**
 * Created by nehon on 30/07/17.
 */
public class SceneEdit extends CompositeEdit implements SceneGraphStructureEdit {

    public SceneEdit(Edit... edits) {
        super(edits);
    }
}
