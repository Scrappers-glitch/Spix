package spix.app.action;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.jme3.scene.Spatial;
import com.jme3.util.TangentBinormalGenerator;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import spix.app.Editor;
import spix.core.*;

/**
 * Created by nehon on 01/08/17.
 */
public class GenerateTangentsAction extends DefaultActionList{

    private enum GeneratorType{
        Classic,
        Mikkt
    }
    public GenerateTangentsAction(Spix spix) {
        super("Generate Tangents");

        add(new Editor.NopAction("Classic") {
            public void performAction(Spix spix) {
                genTangents(GeneratorType.Classic, spix);
            }
        });

        add(new Editor.NopAction("Mikkt space") {
            public void performAction(Spix spix) {
                genTangents(GeneratorType.Mikkt, spix);
            }
        });
        spix.getBlackboard().bind("main.selection",
                this, "enabled", new ContainsOneOfType(Spatial.class));
    }


    private void genTangents(GeneratorType genType, Spix spix) {
        SelectionModel selection = spix.getBlackboard().get("main.selection", SelectionModel.class);

        for (Object selected : selection) {
            if(Spatial.class.isAssignableFrom(selected.getClass())) {
                switch(genType){
                    case Mikkt:
                        MikktspaceTangentGenerator.generate((Spatial)selected);
                        break;
                    case Classic:
                        TangentBinormalGenerator.generate((Spatial)selected);
                        break;
                }
            }
        }

    }
}
