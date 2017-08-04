package spix.app.action;

import com.google.common.base.Predicates;
import com.jme3.scene.Spatial;
import org.parboiled.transform.BaseAction;
import spix.app.DuplicateSelectionAppState;
import spix.app.Editor;
import spix.core.DefaultActionList;
import spix.core.Spix;

/**
 * Created by nehon on 01/08/17.
 */
public class DuplicateAction extends DefaultActionList {

    public DuplicateAction(Spix spix, DuplicateSelectionAppState duplicateState){
        super("Duplicate");

        add(new Editor.NopAction("Share material", "shift D", null) {
            public void performAction(Spix spix) {
                duplicateState.duplicate(false, false);
            }
        });

        add(new Editor.NopAction("Duplicate material", "shift control D", null) {
            public void performAction(Spix spix) {
                duplicateState.duplicate(true, false);
            }
        });

        add(new Editor.NopAction("Deep clone", "shift control alt D", null) {
            public void performAction(Spix spix) {
                duplicateState.duplicate(false, true);
            }
        });
        spix.getBlackboard().bind("main.selection",
                this, "enabled", new ContainsOneOfType(Spatial.class));
    }
}
