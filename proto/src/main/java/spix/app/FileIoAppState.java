package spix.app;

import com.jme3.app.*;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.*;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.scene.*;
import spix.props.*;
import spix.type.Type;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static spix.app.DefaultConstants.*;


/**
 * Created by Nehon on 15/12/2016.
 */
public class FileIoAppState extends BaseAppState {

    private AssetManager assetManager;
    private Node rootNode;
    private Property mainAssetProp = new DefaultProperty(MAIN_ASSETS_FOLDER, new Type(String.class), "");
    private Property currentOpenedFile = new DefaultProperty(SCENE_FILE_NAME, new Type(String.class), "");
    private Property currentScene = new DefaultProperty(SCENE_ROOT, new Type(Spatial.class), new Node("default"));
    private AssetLoadingListener assetListener = new AssetLoadingListener();
    private List<Runnable> enabledCommands = new CopyOnWriteArrayList<>();

    @Override
    protected void initialize(Application app) {
        this.assetManager = app.getAssetManager();
        this.rootNode = ((SimpleApplication) app).getRootNode();
        getState(SpixState.class).getSpix().getBlackboard().set(MAIN_ASSETS_FOLDER, mainAssetProp);
        getState(SpixState.class).getSpix().getBlackboard().set(SCENE_ROOT, currentScene);
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        for (Runnable r : enabledCommands) {
            r.run();
        }
    }

    public void addEnabledCommand(Runnable runable) {
        enabledCommands.add(runable);
    }

    @Override
    protected void onDisable() {

    }

    public void newProject(String assetPath, String subPath) {
        String oldPath = (String) mainAssetProp.getValue();

        mainAssetProp.setValue(assetPath);
        if (subPath == null) {
            subPath = getUnusedName(assetPath, "Scenes/newScene.j3o");
        }
        currentOpenedFile.setValue(subPath);
        Node newScene = new Node("New Scene");
        rootNode.detachAllChildren();
        rootNode.attachChild(newScene);

        if (oldPath != null && oldPath.length() > 0) {
            assetManager.unregisterLocator(oldPath, FileLocator.class);
        }
        assetManager.registerLocator(assetPath, FileLocator.class);
        currentScene.setValue(newScene);
        save();
    }

    public void save() {
        saveAs(mainAssetProp.getValue() + File.separator + currentOpenedFile.getValue());
        Spatial scene = (Spatial) currentScene.getValue();

        if(scene.getKey() != null) {
            //removing the scene so that it's really reloaded if we reload it during this session.
            assetManager.deleteFromCache(scene.getKey());
        }
    }

    public void saveAs(String fileName) {
        //actually save

        BinaryExporter exporter = BinaryExporter.getInstance();
        File file = new File(fileName);
        Node scene = (Node) currentScene.getValue();
        try {
            exporter.save(scene, file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String getUnusedName(String basePath, String filePath) {
        Path path = Paths.get(basePath + File.separator + filePath);
        Path fileName = path.getFileName();
        Path folder = path.getParent();
        int count = 1;

        while (Files.exists(path)) {
            String file[] = fileName.toString().split("\\.");

            path = Paths.get(folder.toString() + File.separator + file[0] + "_" + count + "." + file[1]);
            count++;
        }

        return filePath.replaceAll(fileName.toString(), path.getFileName().toString());

    }

    public void loadFile(File f) {
        try {
            Spatial scene = loadScene(f, true);
            rootNode.detachAllChildren();
            rootNode.attachChild(scene);

            currentScene.setValue(scene);

            getState(NodeWidgetState.class).setScene(scene);
        } catch (AssetLoadException | AssetNotFoundException e) {
            e.printStackTrace();
            //TODO here we should report in an error log.
        }

    }

    public Spatial loadScene(File f, boolean changeAssetFolder) throws AssetLoadException, AssetNotFoundException {
        // JME doesn't really make this easy... so we cheat a little and make some
        // assumptions.
        File assetRoot = f.getParentFile();
        String modelPath = f.getName();
        while (assetRoot.getParentFile() != null && !"assets".equals(assetRoot.getName())) {
            modelPath = assetRoot.getName() + "/" + modelPath;
            assetRoot = assetRoot.getParentFile();
        }

        if(assetRoot.getParentFile() ==  null){
            //we went all the way up not finding an asset folder and we may have a broken path so just take the file's folder as the asset path
            assetRoot = f.getParentFile();
            modelPath = f.getName();
        }

        //System.out.println("Asset root:" + assetRoot + "   modelPath:" + modelPath);
        assetManager.registerLocator(assetRoot.toString(), FileLocator.class);

        if (changeAssetFolder) {
            mainAssetProp.setValue(assetRoot.toString());
            currentOpenedFile.setValue(modelPath);

        } else if (!mainAssetProp.getValue().equals(assetRoot.toString())) {
            //This asset is not in the main asset root, meaning that if we add it in the scene as is the resulting j3o will be broken and will miss some assets.
            //We have to relocate them in the main asset folder.

            assetListener.clear();
            assetManager.addAssetEventListener(assetListener);
            Spatial model = assetManager.loadModel(modelPath);

            for (String dependency : assetListener.getDependencies()) {
                Path source = Paths.get(assetRoot + File.separator + dependency);
                //check if the source file exists in the source folder (could be stock assets that doesn't need to be copied)
                if (Files.exists(source)) {
                    Path target = Paths.get(mainAssetProp.getValue() + File.separator + dependency);
                    try {
                        System.err.println("copying " + source + " to " + target);
                        if (!Files.exists(target)) {
                            //create the parent dir if needed.
                            if (!Files.exists(target.getParent())) {
                                Files.createDirectories(target.getParent());
                            }
                            //copy the file.
                            Files.copy(source, target);
                        }
                        //maybe if the file already exists in the target dir throw a warning...
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            assetManager.deleteFromCache(model.getKey());
            assetManager.unregisterLocator(assetRoot.toString(), FileLocator.class);
            assetManager.removeAssetEventListener(assetListener);
        }

        Spatial model = assetManager.loadModel(modelPath);

        return model;
    }


    public void AppendFile(File f) {
        try {
            Spatial scene = loadScene(f, false);

            // For now, find out where to put the scene so that it is next to whatever
            // is currently loaded
            BoundingBox currentBounds = (BoundingBox) rootNode.getWorldBound();
            BoundingBox modelBounds = (BoundingBox) scene.getWorldBound();

            float x = 0;
            float extent = 0;
            if (currentBounds != null) {
                x = currentBounds.getCenter().x;
                extent = currentBounds.getXExtent();
            }
            float worldRight = x + extent;
            float modelLeft = -modelBounds.getCenter().x + modelBounds.getXExtent();

            scene.setLocalTranslation(worldRight + modelLeft, 0, 0);
            Node rootScene = (Node) currentScene.getValue();
            rootScene.attachChild(scene);
        } catch (AssetLoadException | AssetNotFoundException e) {
            e.printStackTrace();
            //TODO here we should report in an error log.
        }
    }

    private class AssetLoadingListener implements AssetEventListener {
        Set<String> dependencies = new HashSet<>();

        @Override
        public void assetLoaded(AssetKey key) {

        }

        @Override
        public void assetRequested(AssetKey key) {
            dependencies.add(key.getName());
        }

        @Override
        public void assetDependencyNotFound(AssetKey parentKey, AssetKey dependentAssetKey) {

        }

        public void clear() {
            dependencies.clear();
        }

        public Set<String> getDependencies() {
            return dependencies;
        }
    }

}
