package spix.app;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.*;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.*;
import com.jme3.material.plugin.export.material.J3MExporter;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderNodeDefinition;
import com.jme3.texture.Texture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spix.app.material.MaterialService;
import spix.core.*;
import spix.ui.MessageRequester;
import spix.undo.Edit;
import spix.undo.UndoManager;
import spix.undo.edit.SpatialAddEdit;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
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
    private Logger log = LoggerFactory.getLogger(FileIoAppState.class.getName());
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);

    public FileIoAppState() {

    }

    @Override
    protected void initialize(Application app) {
        this.assetManager = app.getAssetManager();
        this.rootNode = ((SimpleApplication) app).getRootNode();
        this.blackboard = getState(SpixState.class).getSpix().getBlackboard();
        blackboard.set(MAIN_ASSETS_FOLDER, "");
        Node n = new Node("Default Scene");
        rootNode.attachChild(n);
        blackboard.set(SCENE_ROOT, n);
        blackboard.set(SCENE_FILE_NAME, "defaultScene.j3o");
    }

    private Spix getSpix(){
        return getState(SpixState.class).getSpix();
    }

    @Override
    protected void cleanup(Application app) {
        executor.shutdownNow();
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
        //Select the new scene
        blackboard.get(SELECTION_PROPERTY, SelectionModel.class).setSingleSelection(newScene);
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
        try {
            exporter.save(scene, file);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        //dependencies
        Set<AssetKey> files = blackboard.get(MODIFIED_DEPENDENCIES, Set.class);
        if (files != null) {
            String message = "Some dependencies have been modified, do you want to save tham too?\n";
            for (AssetKey s : files) {
                message += s.getName() + "\n";
            }

            getSpix().getService(MessageRequester.class).confirm("Save dependencies", message, new RequestCallback<Boolean>() {
                @Override
                public void done(Boolean save) {
                    if (save) {
                        for (AssetKey key : files) {
                            if (key instanceof MaterialKey) {
                                saveMaterial(key);
                            }
                        }
                        //clear unsaved dependencies
                        blackboard.set(MODIFIED_DEPENDENCIES, null);
                        blackboard.set(SCENE_DIRTY, false);
                    }
                }
            });

        } else {
            blackboard.set(SCENE_DIRTY, false);
        }
        FilePath fp = getFilePath(file);
        blackboard.set(MAIN_ASSETS_FOLDER, fp.assetRoot.toString());
        blackboard.set(SCENE_FILE_NAME, fp.modelPath);
    }

    public void saveMaterial(AssetKey key) {
        Material mat = getSpix().getService(MaterialService.class).getMaterialForPath(key.getName());
        String path = key.getName();
        if (log.isDebugEnabled()) {
            log.debug("Savind material: " + path);
        }
        J3MExporter ex = new J3MExporter();

        try {
            ex.save(mat, new File(blackboard.get(MAIN_ASSETS_FOLDER) + File.separator + path));
            if (mat.getKey() != null) {
                assetManager.deleteFromCache(mat.getKey());
            } else {
                mat.setKey(new MaterialKey(path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Material createMaterialFromDefault(File file) {
        Path filePath = file.toPath();
        try {

            String fileName = filePath.getFileName().toString();
            filePath = filePath.getParent();
            fileName = getUnusedName(filePath.toString(), fileName);
            filePath = filePath.resolve(fileName);
            Files.copy(getClass().getResourceAsStream("/templates/default.j3md"), filePath);
            Path root = Paths.get(blackboard.get(MAIN_ASSETS_FOLDER, String.class));
            filePath = root.relativize(filePath);
            Material mat = new Material(assetManager, filePath.toString());
            return mat;
        } catch (IOException e) {
            getSpix().getService(MessageRequester.class).showMessage("Error creating File " + filePath.toString(), e.getMessage(), MessageRequester.Type.Error);
            e.printStackTrace();
        }

        return null;
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

        loadScene(f, true, new RequestCallback<Spatial>() {
            @Override
            public void done(Spatial scene) {
                rootNode.detachAllChildren();
                rootNode.attachChild(scene);

                blackboard.set(SCENE_ROOT, scene);
                //Select the new scene
                blackboard.get(SELECTION_PROPERTY, SelectionModel.class).setSingleSelection(scene);
                //clear unsaved dependencies
                blackboard.set(MODIFIED_DEPENDENCIES, null);
            }
        });
    }

    public void loadScene(File f, boolean changeAssetFolder, RequestCallback<Spatial> callback) throws AssetLoadException, AssetNotFoundException {
        String id = getSpix().getService(MessageRequester.class).displayLoading("Loading " + f.getName() + "...");
        Runnable task = new Runnable() {
            @Override
            public void run() {
                FilePath fp = getFilePath(f);
                try {


                    if (changeAssetFolder) {
                        assetManager.unregisterLocator(blackboard.get(MAIN_ASSETS_FOLDER, String.class), FileLocator.class);
                        assetManager.registerLocator(fp.assetRoot.toString(), FileLocator.class);
                        blackboard.set(MAIN_ASSETS_FOLDER, fp.assetRoot.toString());
                        blackboard.set(SCENE_FILE_NAME, fp.modelPath);

                    } else if (!blackboard.get(MAIN_ASSETS_FOLDER).equals(fp.assetRoot.toString())) {
                        //This asset is not in the main asset root, meaning that if we add it in the scene as is the resulting j3o will be broken and will miss some assets.
                        //We have to relocate them in the main asset folder.
                        relocateAsset(fp, new ModelKey(fp.modelPath));
                    }

                    Spatial model = assetManager.loadModel(fp.modelPath);

                    getSpix().getService(MaterialService.class).gatherMaterialsForSync(model);
                    getApplication().enqueue(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(model);
                            getSpix().getService(MessageRequester.class).hideLoading(id);
                        }
                    });
                } catch (AssetLoadException | AssetNotFoundException e) {
                    e.printStackTrace();
                    getSpix().getService(MessageRequester.class).hideLoading(id);
                    getSpix().getService(MessageRequester.class).showMessage("Error Loading File " + f.getName(), e.getMessage(), MessageRequester.Type.Error);
                }

            }
        };
        executor.execute(task);

    }

    private void relocateAsset(FilePath fp, AssetKey key) {
        assetManager.registerLocator(fp.assetRoot.toString(), FileLocator.class);
        assetListener.clear();
        assetManager.addAssetEventListener(assetListener);

        try {
            assetManager.loadAsset(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assetManager.removeAssetEventListener(assetListener);
        relocateDependencies(fp);
        assetManager.deleteFromCache(key);
        assetManager.unregisterLocator(fp.assetRoot.toString(), FileLocator.class);
    }

    private void relocateDependencies(FilePath fp) {
        List<String> dependencies = new ArrayList<>();
        dependencies.addAll(assetListener.getDependencies());
        List<String> deps = new ArrayList<>();
        for (String dependency : dependencies) {
            //j3sn and j3md needs to be loaded so that associated shader files are copied over
            if (dependency.endsWith(".j3sn")) {
                ShaderNodeDefinitionKey sndKey = new ShaderNodeDefinitionKey(dependency);
                List<ShaderNodeDefinition> defs = (List<ShaderNodeDefinition>) assetManager.loadAsset(sndKey);
                for (ShaderNodeDefinition def : defs) {
                    for (String path : def.getShadersPath()) {
                        addShaderDependencies(deps, path);
                    }
                }
            }
            if (dependency.endsWith(".j3md")) {
                AssetKey<MaterialDef> defKey = new AssetKey<>(dependency);
                MaterialDef def = assetManager.loadAsset(defKey);
                for (String techName : def.getTechniqueDefsNames()) {
                    List<TechniqueDef> tds = def.getTechniqueDefs(techName);
                    for (TechniqueDef td : tds) {
                        EnumMap<Shader.ShaderType, String> shaders = td.getShaderProgramNames();
                        for (String path : shaders.values()) {
                            addShaderDependencies(deps, path);
                        }
                    }
                }
            }
            deps.add(dependency);
        }
        final StringBuilder message = new StringBuilder("The following files have been imported in the asset folder: \n");
        for (String dependency : deps) {
            Path source = Paths.get(fp.assetRoot + File.separator + dependency);
            //check if the source file exists in the source folder (could be stock assets that doesn't need to be copied)
            if (Files.exists(source)) {
                Path target = Paths.get(blackboard.get(MAIN_ASSETS_FOLDER) + File.separator + dependency);
                try {
                    message.append(dependency).append("\n");
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
        getApplication().enqueue(new Runnable() {
            @Override
            public void run() {
                getSpix().getService(MessageRequester.class).showMessage("Asset Imported successfully", message.toString(), MessageRequester.Type.Information);
            }
        });
    }

    private void addShaderDependencies(List<String> deps, String path) {
        assetListener.clear();
        assetManager.addAssetEventListener(assetListener);
        assetManager.loadAsset(path);
        assetManager.removeAssetEventListener(assetListener);
        deps.addAll(assetListener.getDependencies());
    }

    public void loadTexture(File file, String relocateIn, RequestCallback<Texture> done) {
        loadTexture(file, relocateIn, false, done);
    }

    public void loadTexture(File file, String relocateIn, boolean flip, RequestCallback<Texture> done) {
        String id = getSpix().getService(MessageRequester.class).displayLoading("Loading " + file.getName() + "...");
        Runnable task = new Runnable() {
            @Override
            public void run() {
                FilePath fp = getFilePath(file);

                String assetRoot = blackboard.get(MAIN_ASSETS_FOLDER, String.class);
                if (!fp.assetRoot.getPath().equals(assetRoot)) {
                    //we need to relocate the file
                    fp = relocateFile(fp, relocateIn);
                }
                TextureKey key = new TextureKey(fp.modelPath, flip);
                Texture tex = getApplication().getAssetManager().loadTexture(key);
                tex.setKey(key);
                getApplication().enqueue(new Runnable() {
                    @Override
                    public void run() {
                        done.done(tex);
                        getSpix().getService(MessageRequester.class).hideLoading(id);
                    }
                });

            }
        };
        executor.execute(task);
    }

    public void loadTexture(TextureKey key, RequestCallback<Texture> done) {
        String id = getSpix().getService(MessageRequester.class).displayLoading("Loading " + key.getName() + "...");
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Texture tex = getApplication().getAssetManager().loadTexture(key);
                tex.setKey(key);
                getApplication().enqueue(new Runnable() {
                    @Override
                    public void run() {
                        done.done(tex);
                        getSpix().getService(MessageRequester.class).hideLoading(id);
                    }
                });

            }
        };
        executor.execute(task);
    }

    public void loadTexture(String texturePath, RequestCallback<Texture> done) {
        Path path = Paths.get(texturePath);
        Path parent = path.getParent();
        getApplication().getAssetManager().registerLocator(parent.toString(), FileLocator.class);
        TextureKey key = new TextureKey(path.getFileName().toString(), false);
        loadTexture(key, new RequestCallback<Texture>() {
            @Override
            public void done(Texture result) {
                done.done(result);
                getApplication().getAssetManager().unregisterLocator(parent.toString(), FileLocator.class);
            }
        });
    }

    public Material loadMaterial(File file, String relocateIn) {
        FilePath fp = getFilePath(file);

        String assetRoot = blackboard.get(MAIN_ASSETS_FOLDER, String.class);
        if (!fp.assetRoot.getPath().equals(assetRoot)) {
            //we need to relocate the file
            relocateAsset(fp, new MaterialKey(fp.modelPath));
        }

        Material mat = getApplication().getAssetManager().loadMaterial(fp.modelPath);
        getSpix().getService(MaterialService.class).registerMaterialForSync(fp.modelPath, mat);
        return mat;
    }

    public Material makeMaterialFromMatDef(File file) {
        FilePath fp = getFilePath(file);

        String assetRoot = blackboard.get(MAIN_ASSETS_FOLDER, String.class);
        if (!fp.assetRoot.getPath().equals(assetRoot)) {
            //we need to relocate the file
            relocateAsset(fp, new AssetKey(fp.modelPath));
        }
        Material mat = new Material(getApplication().getAssetManager(), fp.modelPath);
        return mat;
    }

    public Material makeMaterialFromStockMatDef(String matDefPath) {
        Material mat = new Material(getApplication().getAssetManager(), matDefPath);
        return mat;
    }

    private FilePath relocateFile(FilePath fp, String localPath) {
        Path source = Paths.get(fp.assetRoot + File.separator + fp.modelPath);
        //check if the source file exists in the source folder (could be stock assets that doesn't need to be copied)
        if (Files.exists(source)) {
            Path target = Paths.get(blackboard.get(MAIN_ASSETS_FOLDER) + File.separator + localPath + File.separator + fp.fileName);
            try {
                //Te file exists in the target dir, let's find a suitable new name.
                while (Files.exists(target)) {
                    String newName = getUnusedName(target.getParent().toString(), target.getFileName().toString());
                    target = Paths.get(blackboard.get(MAIN_ASSETS_FOLDER) + File.separator + localPath + File.separator + newName);
                }

                //create the parent dir if needed.
                if (!Files.exists(target.getParent())) {
                    Files.createDirectories(target.getParent());
                }
                //copy the file.
                System.err.println("copying " + source + " to " + target);
                Files.copy(source, target);
                return getFilePath(target.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public FilePath getFilePath(File f) {
        // JME doesn't really make this easy... so we cheat a little and make some
        // assumptions.
        FilePath fp = new FilePath();
        fp.assetRoot = f.getParentFile();
        fp.modelPath = f.getName();
        fp.fileName = f.getName();
        while (fp.assetRoot.getParentFile() != null && !("assets".equals(fp.assetRoot.getName()) || "resources".equals(fp.assetRoot.getName()))) {
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

        loadScene(f, false, new RequestCallback<Spatial>() {
            @Override
            public void done(Spatial scene) {
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
                if (rootScene instanceof Node) {
                    ((Node) rootScene).attachChild(scene);

                    UndoManager um = getSpix().getService(UndoManager.class);
                    um.addEdit(new SpatialAddEdit((Node) rootScene, scene));
                    //Select the imported scene
                    blackboard.get(SELECTION_PROPERTY, SelectionModel.class).setSingleSelection(scene);
                }
            }
        });
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

    public static class FilePath {
        File assetRoot;
        String modelPath;
        String fileName;
    }
}
