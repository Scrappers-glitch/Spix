package spix.app;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.*;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.*;
import com.jme3.material.plugin.export.material.J3MExporter;
import com.jme3.material.plugin.export.materialdef.J3mdExporter;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderNodeDefinition;
import com.jme3.texture.Texture;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import spix.app.material.MaterialService;
import spix.core.*;
import spix.ui.MessageRequester;
import spix.undo.Edit;
import spix.undo.UndoManager;
import spix.undo.edit.SpatialAddEdit;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
    private Yaml yaml;
    private String currentZipLocator = null;

    private static final String[] supportedModelFormats = new String[]{".gltf", " .j3o", ".mesh", ".obj"};


    public FileIoAppState() {
        DumperOptions opt = new DumperOptions();
        opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(opt);
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
            String message = "Some dependencies have been modified, do you want to save them too?\n";
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

    public void saveFile(String fileName, String fileContent) {
        AssetKey key = new AssetKey(fileName);
        assetManager.deleteFromCache(key);
        String root = getSpix().getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class);
        try {
            Files.write(Paths.get(root, fileName), fileContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            getSpix().getService(MessageRequester.class).showMessage("Error Saving File " + fileName, e.getMessage(), MessageRequester.Type.Error);
        }

    }

    public void saveMaterialdef(MaterialDef def) {
        J3mdExporter exporter = new J3mdExporter();
        String root = getSpix().getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class);
        try (FileOutputStream stream = new FileOutputStream(new File(root + File.separator + def.getAssetName()))) {
            exporter.save(def, stream);
        } catch (Exception e) {
            e.printStackTrace();
            getSpix().getService(MessageRequester.class).showMessage("Error Saving Material Def " + def.getAssetName(), e.getMessage(), MessageRequester.Type.Error);
        }
    }

    public MaterialDef loadMaterialDef(String path) {
        AssetKey key = new AssetKey<>(path);
        assetManager.deleteFromCache(key);
        return (MaterialDef) loadAsset(key);
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

    private boolean isSupportedModelFormat(String path) {
        for (String format : supportedModelFormats) {
            if (path.endsWith(format)) {
                return true;
            }
        }
        return false;
    }

    public void loadFile(File f) {

        boolean setMainAssetPath = true;

        FilePath fp = getFilePath(f);

        if (f.getName().endsWith(".zip")) {
            setMainAssetPath = false;
            if (currentZipLocator != null) {
                assetManager.unregisterLocator(currentZipLocator, ZipLocator.class);
            }
            currentZipLocator = f.getPath();
            assetManager.registerLocator(currentZipLocator, ZipLocator.class);
            try (ZipFile zip = new ZipFile(f)) {
                String file = null;
                Enumeration entries = zip.entries();
                while (entries.hasMoreElements() && file == null) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    if (isSupportedModelFormat(entry.getName())) {
                        file = entry.getName();
                    }
                }
                fp.fileName = file;
                fp.modelPath = file;
                fp.assetRoot = new File(blackboard.get(MAIN_ASSETS_FOLDER, String.class));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        loadScene(fp, setMainAssetPath, new RequestCallback<Spatial>() {
            @Override
            public void done(Spatial scene) {
                rootNode.detachAllChildren();
                Spatial rootScene = scene;
                if (!(rootScene instanceof Node)) {
                    Node nodeScene = new Node(scene.getName() + "-node");
                    nodeScene.attachChild(rootScene);
                    rootScene = nodeScene;
                }

                rootNode.attachChild(rootScene);

                blackboard.set(SCENE_ROOT, rootScene);
                //Select the new scene
                blackboard.get(SELECTION_PROPERTY, SelectionModel.class).setSingleSelection(rootScene);
                //clear unsaved dependencies
                blackboard.set(MODIFIED_DEPENDENCIES, null);
            }
        });
    }

    public String loadFileAsText(String path) {
        String fullPath = blackboard.get(MAIN_ASSETS_FOLDER, String.class) + File.separator + path;
        File f = new File(fullPath);
        if (f.exists()) {
            try {
                return new String(Files.readAllBytes(f.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //try to look in stock resources
            InputStream input = this.getClass().getResourceAsStream("/" + path);
            if (input != null) {
                Scanner s = new Scanner(input).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";
                return result;
            }
        }
        return null;
    }

    public void loadScene(FilePath fp, boolean changeAssetFolder, RequestCallback<Spatial> callback) throws AssetLoadException, AssetNotFoundException {
        String id = getSpix().getService(MessageRequester.class).displayLoading("Loading " + fp.fileName + "...");
        Runnable task = new Runnable() {
            @Override
            public void run() {
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
                    ModelKey key = new ModelKey(fp.modelPath);
                    assetManager.deleteFromCache(key);
                    Spatial model = assetManager.loadModel(fp.modelPath);

                    getSpix().getService(MaterialService.class).gatherMaterialsForSync(model);
                    getApplication().enqueue(new Runnable() {
                        @Override
                        public void run() {
                            getState(SceneValidatorState.class).validate(model);
                            callback.done(model);
                            getSpix().getService(MessageRequester.class).hideLoading(id);
                        }
                    });
                } catch (AssetLoadException | AssetNotFoundException e) {
                    e.printStackTrace();
                    getSpix().getService(MessageRequester.class).hideLoading(id);
                    getSpix().getService(MessageRequester.class).showMessage("Error Loading File " + fp.fileName, e.getMessage(), MessageRequester.Type.Error);
                }

            }
        };
        executor.execute(task);

    }

    private void relocateAsset(FilePath fp, AssetKey key) throws AssetNotFoundException {
        assetManager.registerLocator(fp.assetRoot.toString(), FileLocator.class);
        assetListener.clear();
        assetManager.addAssetEventListener(assetListener);

        loadAsset(key);
        assetManager.removeAssetEventListener(assetListener);
        relocateDependencies(fp);
        try {
            assetManager.deleteFromCache(key);
        } catch (IllegalArgumentException e) {
            //some asset key doesn't specify any cache strategy
            log.info(e.getMessage());
        }

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
                List<ShaderNodeDefinition> defs = (List<ShaderNodeDefinition>) loadAsset(sndKey);
                for (ShaderNodeDefinition def : defs) {
                    for (String path : def.getShadersPath()) {
                        addShaderDependencies(deps, path);
                    }
                }
            }
            if (dependency.endsWith(".j3md")) {
                AssetKey<MaterialDef> defKey = new AssetKey<>(dependency);
                MaterialDef def = (MaterialDef) loadAsset(defKey);
                for (String techName : def.getTechniqueDefsNames()) {
                    List<TechniqueDef> tds = def.getTechniqueDefs(techName);
                    for (TechniqueDef td : tds) {
                        if (td.isUsingShaderNodes()) {
                            continue;
                        }
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
        loadAsset(new AssetKey(path));
        assetManager.removeAssetEventListener(assetListener);
        deps.addAll(assetListener.getDependencies());
    }

    public Object loadAsset(AssetKey key) {
        try {
            return assetManager.loadAsset(key);
        } catch (Exception e) {
            getSpix().getService(MessageRequester.class).showMessage("Error Loading File " + key.getName(), e.getMessage(), MessageRequester.Type.Error);
        }
        return null;
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


    public Object importAsset(File file, Class<? extends AssetKey> keyClass) {
        FilePath fp = getFilePath(file);
        AssetKey key = null;
        try {
            key = keyClass.getConstructor(String.class).newInstance(fp.modelPath);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            getSpix().getService(MessageRequester.class).showMessage("Error creating key " + fp.modelPath, e.getMessage(), MessageRequester.Type.Error);
            return null;
        }
        String assetRoot = blackboard.get(MAIN_ASSETS_FOLDER, String.class);
        if (!fp.assetRoot.getPath().equals(assetRoot)) {
            //we need to relocate the file
            relocateAsset(fp, key);
        }

        Object asset = getApplication().getAssetManager().loadAsset(key);
        return asset;
    }

    public List<ShaderNodeDefinition> makeJ3sn(File file) {
        try {
            file.createNewFile();
            JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/default.j3sn");
            String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));

            Path root = Paths.get(blackboard.get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
            String path = root.relativize(file.toPath()).toString().replaceAll("\\\\", "/");
            String noExtPath = path.substring(0, path.lastIndexOf("."));
            JtwigModel model = JtwigModel.newModel()
                    .with("name", fileName.substring(0, 1).toUpperCase() + fileName.substring(1))
                    .with("filePath", noExtPath);

            String content = template.render(model);
            Files.write(file.toPath(), content.getBytes());

            ShaderNodeDefinitionKey key = new ShaderNodeDefinitionKey(path.toString());
            List<ShaderNodeDefinition> defs = assetManager.loadAsset(key);
            for (ShaderNodeDefinition def : defs) {
                for (String p : def.getShadersPath()) {
                    checkAndCreateShaderFile(p);
                }
            }
            return defs;
        } catch (IOException e) {
            getSpix().getService(MessageRequester.class).showMessage("Error creating file " + file.getPath(), e.getMessage(), MessageRequester.Type.Error);
            e.printStackTrace();
        }
        return null;
    }

    public void checkAndCreateShaderFile(String path) throws IOException {
        Path root = Paths.get(blackboard.get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
        Path filePath = Paths.get(root.toString(), path);
        if (!Files.exists(filePath)) {
            Files.write(filePath, "void main(){\n}\n".getBytes(), StandardOpenOption.CREATE_NEW);
        }
    }

    public void renameFile(String oldPath, String newPath) throws IOException {
        Path root = Paths.get(blackboard.get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
        Path oldFilePath = Paths.get(root.toString(), oldPath);
        Path newFilePath = Paths.get(root.toString(), newPath);
        if (Files.exists(newFilePath)) {
            throw new IOException("File " + newFilePath.toString() + " already exists");
        }
        oldFilePath.toFile().renameTo(newFilePath.toFile());
    }

    public boolean isFileWritable(String filePath) {
        String assetRoot = blackboard.get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class);
        Path p = Paths.get(assetRoot, filePath);
        return Files.exists(p);
    }

    public Material loadMaterial(File file) {
        Material mat = (Material) importAsset(file, MaterialKey.class);
        getSpix().getService(MaterialService.class).registerMaterialForSync(mat.getAssetName(), mat);
        return mat;
    }

    public Material makeMaterialFromMatDef(File file) {
        FilePath fp = getFilePath(file);

        String assetRoot = blackboard.get(MAIN_ASSETS_FOLDER, String.class);
        if (!fp.assetRoot.getPath().equals(assetRoot)) {
            //we need to relocate the file
            relocateAsset(fp, new AssetKey(fp.modelPath));
        }
        try {
            Material mat = new Material(getApplication().getAssetManager(), fp.modelPath);
            return mat;
        } catch (AssetLoadException | AssetNotFoundException e) {
            getSpix().getService(MessageRequester.class).showMessage("Error loading material definition " + fp.modelPath, e.getMessage(), MessageRequester.Type.Error);
            e.printStackTrace();
        }
        return null;
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

        FilePath fp = getFilePath(f);
        loadScene(fp, false, new RequestCallback<Spatial>() {
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

    public Object loadMaterialDefMetadata(MaterialDef matDef) {

        String path = blackboard.get(MAIN_ASSETS_FOLDER, String.class) + File.separator + matDef.getAssetName() + ".mtdt";

        try {
            InputStream input = new FileInputStream(new File(path));
            return yaml.load(input);
        } catch (FileNotFoundException e) {
            return null;
        }

    }

    public void saveMaterialDefMetadata(Object metadata, String path) {

        String filePath = blackboard.get(MAIN_ASSETS_FOLDER, String.class) + File.separator + path + ".mtdt";
        File f = new File(filePath);
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            yaml.dump(metadata, new FileWriter(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> findFilesWithExtension(String extension) {
        List<String> files = new ArrayList<>();
        listFiles(blackboard.get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class), files, extension);
        return files;
    }

    private void listFiles(String directoryName, List<String> files, String extension) {
        File directory = new File(directoryName);
        Path root = Paths.get(blackboard.get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                if (com.google.common.io.Files.getFileExtension(file.getName()).equals(extension)) {
                    files.add(root.relativize(file.toPath()).toString().replaceAll("\\\\", "/"));
                }
            } else if (file.isDirectory()) {
                listFiles(file.getAbsolutePath(), files, extension);
            }
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

    public static class FilePath {
        File assetRoot;
        String modelPath;
        String fileName;
    }
}
