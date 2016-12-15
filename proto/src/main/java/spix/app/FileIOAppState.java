package spix.app;

import com.jme3.app.*;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.scene.*;
import spix.props.*;
import spix.type.Type;

import java.io.File;

/**
 * Created by Nehon on 15/12/2016.
 */
public class FileIOAppState extends BaseAppState {

    private static String MAIN_ASSETS_FOLDER = "scene.mainAssetsFolder";
    private AssetManager assetManager;
    private Node rootNode;
    private Property mainAssetProp = new DefaultProperty(MAIN_ASSETS_FOLDER, new Type(String.class), "");


    @Override
    protected void initialize(Application app) {
        this.assetManager = app.getAssetManager();
        this.rootNode = ((SimpleApplication) app).getRootNode();
        getState(SpixState.class).getSpix().getBlackboard().set(MAIN_ASSETS_FOLDER, mainAssetProp);
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

    public void loadFile(File f) {
        Spatial scene = loadScene(f, true);

        rootNode.detachAllChildren();
        rootNode.attachChild(scene);

        getState(SpixState.class).getSpix().getBlackboard().set("scene.root", scene);
        getState(NodeWidgetState.class).setScene(scene);
    }

    public Spatial loadScene(File f, boolean changeAssetFolder) {
        // JME doesn't really make this easy... so we cheat a little and make some
        // assumptions.
        File assetRoot = f.getParentFile();
        String modelPath = f.getName();
        while (assetRoot.getParentFile() != null && !"assets".equals(assetRoot.getName())) {
            modelPath = assetRoot.getName() + "/" + modelPath;
            assetRoot = assetRoot.getParentFile();
        }
        //System.out.println("Asset root:" + assetRoot + "   modelPath:" + modelPath);

        if (changeAssetFolder) {
            mainAssetProp.setValue(assetRoot.toString());

        } else if (!mainAssetProp.getValue().equals(assetRoot.toString())) {
            //here we should copy the loaded resource in the mainAssetsFolder and load this one.
            //it's a bit tricky because we have to catch assetLoading exceptions and copy the need file and try again...
            // could be nice to be able to know all assets dependencies of a model...
        }

        assetManager.registerLocator(assetRoot.toString(), FileLocator.class);

        return assetManager.loadModel(modelPath);
    }

    public void AppendFile(File f) {
        Spatial scene = loadScene(f, false);

        // For now, find out where to put the scene so that it is next to whatever
        // is currently loaded
        BoundingBox currentBounds = (BoundingBox) rootNode.getWorldBound();
        BoundingBox modelBounds = (BoundingBox) scene.getWorldBound();

        float worldRight = currentBounds.getCenter().x + currentBounds.getXExtent();
        float modelLeft = -modelBounds.getCenter().x + modelBounds.getXExtent();

        scene.setLocalTranslation(worldRight + modelLeft, 0, 0);
        rootNode.attachChild(scene);
    }

    private class MainFolderProperty extends AbstractProperty {

        private Type<String> type = new Type<String>(String.class);
        private String value = "";

        public MainFolderProperty(String id) {
            super(id);
        }

        public MainFolderProperty(String id, String value) {
            this(id);
            this.value = value;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public void setValue(Object value) {
            this.value = (String) value;
        }

        @Override
        public Object getValue() {
            return value;
        }
    }

}
