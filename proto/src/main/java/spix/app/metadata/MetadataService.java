package spix.app.metadata;

import com.jme3.material.MaterialDef;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import spix.app.FileIoAppState;
import spix.core.Blackboard;
import spix.core.Spix;

import java.util.*;

/**
 * Created by Nehon on 10/06/2017.
 */
public class MetadataService {

    private Blackboard blackboard;
    private FileIoAppState fileIoAppState;
    private Yaml yaml;
    private final static List<String> stockMatDefs = new ArrayList<>();

    static {
        stockMatDefs.add("Common/MatDefs/Light/Lighting.j3md");
        stockMatDefs.add("Common/MatDefs/Misc/Unshaded.j3md");
        stockMatDefs.add("Common/MatDefs/Misc/UnshadedNodes.j3md");
    }

    public MetadataService(Spix spix, FileIoAppState fileIoAppState) {
        this.blackboard = spix.getBlackboard();
        this.fileIoAppState = fileIoAppState;
        DumperOptions opt = new DumperOptions();
        opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(opt);
    }

    public Map<String, Object> getMetadata(MaterialDef matDef) {
        String key = "material.metadata." + matDef.getAssetName();
        Map<String, Object> metadata = (Map<String, Object>) blackboard.get(key);
        if (metadata != null) {
            return metadata;
        }
        metadata = loadMetadata(matDef);
        if (metadata != null) {
            blackboard.set(key, metadata);
            return metadata;
        }
        metadata = new LinkedHashMap<>();
        blackboard.set(key, metadata);
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata, MaterialDef matDef) {
        String key = "material.metadata." + matDef.getAssetName();
        blackboard.set(key, metadata);
        if (stockMatDefs.contains(matDef.getAssetName())) {
            System.err.println(yaml.dump(metadata));
            return;
        }
        fileIoAppState.saveMaterialDefMetadata(metadata, matDef.getAssetName());
    }

    private Map<String, Object> loadMetadata(MaterialDef matDef) {
        if (stockMatDefs.contains(matDef.getAssetName())) {
            String name = matDef.getAssetName().substring(matDef.getAssetName().lastIndexOf("/")) + ".mtdt";
            return (Map<String, Object>) yaml.load(this.getClass().getResourceAsStream(name));
        }
        return (Map<String, Object>) fileIoAppState.loadMaterialDefMetadata(matDef);
    }

}
