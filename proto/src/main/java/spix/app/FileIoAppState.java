package spix.app;

import com.jme3.app.*;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.*;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.scene.*;
import spix.core.Blackboard;
import spix.core.Spix;
import spix.props.*;
import spix.type.Type;
import spix.undo.Edit;
import spix.undo.UndoManager;
import spix.undo.edit.SpatialAddEdit;

import java.beans.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static spix.app.DefaultConstants.*;


/**
 * Created by Nehon on 15/12/2016.
 */
public class FileIoAppState extends BaseAppState {

    private AssetManager assetManager;
    private Node rootNode;
    private Blackboard blackboard;
    private AssetLoadingListener assetListener = new AssetLoadingListener();

    public FileIoAppState() {

    }

    @Override
    protected void initialize(Application app) {
        this.assetManager = app.getAssetManager();
        this.rootNode = ((SimpleApplication) app).getRootNode();
        this.blackboard = getState(SpixState.class).getSpix().getBlackboard();
        blackboard.set(MAIN_ASSETS_FOLDER, "");
        blackboard.set(SCENE_ROOT, new Node("Default Scene"));
        blackboard.set(SCENE_FILE_NAME, "defaultScene.j3o");
    }

    private Spix getSpix(){
        return getState(SpixState.class).getSpix();
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        blackboard.bind(UndoManager.LAST_EDIT, this, "lastEdit");
    }

    public void setLastEdit(Edit edit){
        blackboard.set(SCENE_DIRTY, true);
    }

    @Override
    protected void onDisable() {
        blackboard.unbind(UndoManager.LAST_EDIT, this, "lastEdit");
    }

    public void newScene(File assetPath) {
        FilePath fp = getFilePath(assetPath);

        String oldPath = blackboard.get(MAIN_ASSETS_FOLDER, String.class);

        blackboard.set(MAIN_ASSETS_FOLDER, fp.assetRoot.toString());
        if (fp.modelPath == null) {
            fp.modelPath = "Scenes/newScene.j3o";
        }
        fp.modelPath = getUnusedName(fp.assetRoot.toString(), fp.modelPath);

        blackboard.set(SCENE_FILE_NAME, fp.modelPath);
        Node newScene = new Node("New Scene");
        rootNode.detachAllChildren();
        rootNode.attachChild(newScene);

        if (oldPath != null && oldPath.length() > 0) {
            assetManager.unregisterLocator(oldPath, FileLocator.class);
        }
        assetManager.registerLocator(fp.assetRoot.toString(), FileLocator.class);
        blackboard.set(SCENE_ROOT, newScene);
        save();
    }

    public void save() {
        saveAs(blackboard.get(MAIN_ASSETS_FOLDER, String.class) + File.separator + blackboard.get(SCENE_FILE_NAME));

        Spatial scene = (Spatial) blackboard.get(SCENE_ROOT);

        if(scene.getKey() != null) {
            //removing the scene so that it's really reloaded if we reload it during this session.
            assetManager.deleteFromCache(scene.getKey());
        }
    }

    public void saveAs(String fileName) {
        //actually save
        if (!fileName.endsWith(".j3o")){
            fileName += ".j3o";
        }

        BinaryExporter exporter = BinaryExporter.getInstance();
        File file = new File(fileName);
        Spatial scene = (Spatial) blackboard.get(SCENE_ROOT);
        getState(SpixState.class).getSpix().getBlackboard().set(SCENE_DIRTY, false);
        try {
            exporter.save(scene, file);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        FilePath fp = getFilePath(file);
        blackboard.set(MAIN_ASSETS_FOLDER, fp.assetRoot.toString());
        blackboard.set(SCENE_FILE_NAME, fp.modelPath);
    }

    public String getUnusedName(String basePath, String filePath) {
        Path path = Paths.get(basePath + File.separator + filePath);
        Path fileName = path.getFileName();
        Path folder = path.getParent();
        int count = 1;
        String origFileName = fileName.toString();
        //extract previous _number at the end of the file
        Pattern p = Pattern.compile("(.*)_(\\d*)(\\.j3o)");
        Matcher m = p.matcher(origFileName);
        if(m.matches()){
            count = Integer.parseInt(m.group(2)) + 1;
            path = Paths.get(folder.toString() + File.separator + m.replaceFirst("$1$3"));
            fileName = path.getFileName();
        }

        while (Files.exists(path)) {
            String file[] = fileName.toString().split("\\.");

            path = Paths.get(folder.toString() + File.separator + file[0] + "_" + count + "." + file[1]);
            count++;
        }

        return filePath.replaceAll(origFileName, path.getFileName().toString());

    }

    public void loadFile(File f) {
        try {
            Spatial scene = loadScene(f, true);
            rootNode.detachAllChildren();
            rootNode.attachChild(scene);

            blackboard.set(SCENE_ROOT, scene);
        } catch (AssetLoadException | AssetNotFoundException e) {
            e.printStackTrace();
            //TODO here we should report in an error log.
        }

    }

    public Spatial loadScene(File f, boolean changeAssetFolder) throws AssetLoadException, AssetNotFoundException {

        FilePath fp = getFilePath(f);

        //System.out.println("Asset root:" + assetRoot + "   modelPath:" + modelPath);
        assetManager.registerLocator(fp.assetRoot.toString(), FileLocator.class);

        if (changeAssetFolder) {
            blackboard.set(MAIN_ASSETS_FOLDER, fp.assetRoot.toString());
            blackboard.set(SCENE_FILE_NAME, fp.modelPath);

        } else if (!blackboard.get(MAIN_ASSETS_FOLDER).equals(fp.assetRoot.toString())) {
            //This asset is not in the main asset root, meaning that if we add it in the scene as is the resulting j3o will be broken and will miss some assets.
            //We have to relocate them in the main asset folder.

            assetListener.clear();
            assetManager.addAssetEventListener(assetListener);
            Spatial model = assetManager.loadModel(fp.modelPath);

            for (String dependency : assetListener.getDependencies()) {
                Path source = Paths.get(fp.assetRoot + File.separator + dependency);
                //check if the source file exists in the source folder (could be stock assets that doesn't need to be copied)
                if (Files.exists(source)) {
                    Path target = Paths.get(blackboard.get(MAIN_ASSETS_FOLDER) + File.separator + dependency);
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
            assetManager.unregisterLocator(fp.assetRoot.toString(), FileLocator.class);
            assetManager.removeAssetEventListener(assetListener);
        }

        Spatial model = assetManager.loadModel(fp.modelPath);

        return model;
    }

    private FilePath getFilePath(File f) {
        // JME doesn't really make this easy... so we cheat a little and make some
        // assumptions.
        FilePath fp = new FilePath();
        fp.assetRoot = f.getParentFile();
        fp.modelPath = f.getName();
        while (fp.assetRoot.getParentFile() != null && !"assets".equals(fp.assetRoot.getName())) {
            fp.modelPath = fp.assetRoot.getName() + "/" + fp.modelPath;
            fp.assetRoot = fp.assetRoot.getParentFile();
        }

        if(fp.assetRoot.getParentFile() ==  null){
            //we went all the way up not finding an asset folder and we may have a broken path so just take the file's folder as the asset path
            fp.assetRoot = f.getParentFile();
            fp.modelPath = f.getName();
        }
        return fp;
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
            Spatial rootScene = (Spatial) blackboard.get(SCENE_ROOT);
            if (rootScene instanceof Node){
                ((Node)rootScene).attachChild(scene);

                UndoManager um = getSpix().getService(UndoManager.class);
                um.addEdit(new SpatialAddEdit((Node)rootScene, scene));
            }

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

    private class FilePath{
        File assetRoot;
        String modelPath;
    }
}
