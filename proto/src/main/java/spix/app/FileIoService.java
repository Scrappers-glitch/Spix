package spix.app;

import com.jme3.asset.AssetKey;
import com.jme3.asset.MaterialKey;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.scene.*;
import com.jme3.texture.Texture;
import spix.app.material.MaterialService;
import spix.app.utils.MaterialUtils;
import spix.core.*;
import spix.ui.FileRequester;
import spix.undo.UndoManager;
import spix.undo.edit.MaterialSetEdit;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by Nehon on 08/01/2017.
 */
public class FileIoService {

    private FileIoAppState fileState;
    private Spix spix;

    public FileIoService(Spix spix, FileIoAppState fileState) {
        this.fileState = fileState;
        this.spix = spix;
    }


    public void requestTexture(RequestCallback<Texture> callback) {
        File assetRoot = new File(spix.getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
        spix.getService(FileRequester.class).requestFile("Select an image file", "Image file", ".jpg, .bmp, .gif, .png, .jpeg, .hdr, .pfm, .dds, .tga",
                assetRoot, true, true, true,
                new RequestCallback<File>() {
                    @Override
                    public void done(File result) {
                        checkRelocateAndDo(result, "Textures", new RequestCallback<String>() {
                            @Override
                            public void done(String relocateTo) {
                                fileState.loadTexture(result, relocateTo, new RequestCallback<Texture>() {
                                    @Override
                                    public void done(Texture result) {
                                        callback.done(result);
                                    }
                                });
                            }
                        });
                    }
                });
    }

    public void requestTexture(String description, String extensions, boolean flip, RequestCallback<Texture> callback) {
        File assetRoot = new File(spix.getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
        spix.getService(FileRequester.class).requestFile("Select an image file", description, extensions,
                assetRoot, true, true, true,
                new RequestCallback<File>() {
                    @Override
                    public void done(File result) {
                        checkRelocateAndDo(result, "Textures", new RequestCallback<String>() {
                            @Override
                            public void done(String relocateTo) {
                                fileState.loadTexture(result, relocateTo, flip, new RequestCallback<Texture>() {
                                    @Override
                                    public void done(Texture result) {
                                        callback.done(result);
                                    }
                                });
                            }
                        });
                    }
                });
    }

    public void loadJ3mForSelection() {
        SelectionModel model = spix.getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        if (model.getSingleSelection() != null && model.getSingleSelection() instanceof Geometry) {
            Geometry geom = (Geometry) model.getSingleSelection();
            File assetRoot = new File(spix.getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
            spix.getService(FileRequester.class).requestFile("Load a J3m file", "j3m material file", ".j3m",
                    assetRoot, true, false, false,
                    new RequestCallback<File>() {
                        @Override
                        public void done(File result) {
                            checkRelocateAndDo(result, "Materials", new RequestCallback<String>() {
                                @Override
                                public void done(String relocateTo) {
                                    Material mat = fileState.loadMaterial(result, relocateTo);
                                    changeMaterial(mat, geom, model);
                                }
                            });
                        }
                    });
        }
    }

    private void changeMaterial(Material mat, Geometry geom, SelectionModel model) {
        Material oldMat = geom.getMaterial();
        //copy params over new mat if relevant
        MaterialUtils.copyParams(oldMat, mat);
        geom.setMaterial(mat);
        spix.refresh(geom);
        UndoManager um = spix.getService(UndoManager.class);
        MaterialSetEdit edit = new MaterialSetEdit(geom, oldMat, mat);
        um.addEdit(edit);
        model.setSingleSelection(null);
        model.setSingleSelection(geom);
    }

    public void createJ3mForSelection() {
        SelectionModel model = spix.getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        if (model.getSingleSelection() != null && model.getSingleSelection() instanceof Geometry) {
            Geometry geom = (Geometry) model.getSingleSelection();
            File assetRoot = new File(spix.getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
            File defaultFile = new File(assetRoot.getPath() + File.separator + "Materials/material");
            spix.getService(FileRequester.class).requestFile("Create a J3m file", "j3m material file", "j3m",
                    defaultFile, false, false, false,
                    new RequestCallback<File>() {
                        @Override
                        public void done(File result) {
                            Path path = assetRoot.toPath().relativize(result.toPath());
                            String assetPath = path.toString().replaceAll("\\\\", "/");
                            MaterialService matService = spix.getService(MaterialService.class);
                            Material mat = geom.getMaterial();
                            Material oldMat = mat.clone();
                            if (mat.getKey() != null) {
                                matService.unregisterMaterialForSync(mat.getKey().getName(), mat);
                            }

                            MaterialKey key = new MaterialKey(assetPath);
                            mat.setKey(key);
                            spix.getService(MaterialService.class).registerMaterialForSync(assetPath, mat);
                            fileState.saveMaterial(key);

                            spix.refresh(geom);
                            UndoManager um = spix.getService(UndoManager.class);
                            MaterialSetEdit edit = new MaterialSetEdit(geom, oldMat, mat);
                            um.addEdit(edit);
                            model.setSingleSelection(null);
                            model.setSingleSelection(geom);

                        }
                    });
        }
    }


    public void loadJ3mdForSelection() {
        SelectionModel model = spix.getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        if (model.getSingleSelection() != null && model.getSingleSelection() instanceof Geometry) {
            Geometry geom = (Geometry) model.getSingleSelection();
            File assetRoot = new File(spix.getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
            spix.getService(FileRequester.class).requestFile("Load a J3md file", "material definition file", ".j3md",
                    assetRoot, true, false, false,
                    new RequestCallback<File>() {
                        @Override
                        public void done(File result) {
                            Material mat = fileState.makeMaterialFromMatDef(result);
                            changeMaterial(mat, geom, model);
                        }
                    });
        }
    }

    public void createJ3mdForSelection() {
        Geometry geom = getSelectedGeometry();
        if (geom == null) {
            return;
        }
        File assetRoot = getAssetRoot();
        File defaultFile = new File(assetRoot.getPath() + File.separator + "MatDefs/matdef");
        spix.getService(FileRequester.class).requestFile("Create a J3md file", "j3md material definition file", "j3md",
                defaultFile, false, false, false,
                new RequestCallback<File>() {
                    @Override
                    public void done(File result) {
                        MaterialService matService = spix.getService(MaterialService.class);

                        Material oldMat = geom.getMaterial();
                        if (oldMat.getKey() != null) {
                            matService.unregisterMaterialForSync(oldMat.getKey().getName(), oldMat);
                        }

                        Material mat = fileState.createMaterialFromDefault(result);
                        if (mat == null) {
                            return;
                        }
                        geom.setMaterial(mat);
                        spix.refresh(geom);

                        UndoManager um = spix.getService(UndoManager.class);
                        MaterialSetEdit edit = new MaterialSetEdit(geom, oldMat, mat);
                        um.addEdit(edit);
                        SelectionModel model = getSelectionModel();
                        model.setSingleSelection(null);
                        model.setSingleSelection(geom);

                    }
                });

    }

    private Geometry getSelectedGeometry() {
        SelectionModel model = getSelectionModel();
        if (model.getSingleSelection() != null && model.getSingleSelection() instanceof Geometry) {
            return (Geometry) model.getSingleSelection();
        }
        return null;
    }

    private SelectionModel getSelectionModel() {
        return spix.getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
    }

    private File getAssetRoot() {
        return new File(spix.getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
    }

    public void loadStockJ3mdForSelection(String matDefPath) {
        SelectionModel model = spix.getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        if (model.getSingleSelection() != null && model.getSingleSelection() instanceof Geometry) {
            Geometry geom = (Geometry) model.getSingleSelection();
            spix.enqueueTask(new Runnable() {
                @Override
                public void run() {
                    Material mat = fileState.makeMaterialFromStockMatDef(matDefPath);
                    changeMaterial(mat, geom, model);

                }
            });
        }
    }

    public void saveMaterialDef(MaterialDef matDef) {
        fileState.saveMaterialdef(matDef);
        spix.enqueueTask(new Runnable() {
            @Override
            public void run() {
                MaterialDef newDef = fileState.loadMaterialDef(matDef.getAssetName());
                Node rootNode = (Node) spix.getBlackboard().get(DefaultConstants.SCENE_ROOT, Spatial.class);
                spix.getService(MaterialService.class).replaceMatDef(rootNode, newDef);
                SelectionModel model = spix.getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
                if (model.getSingleSelection() instanceof Geometry) {
                    Geometry geom = (Geometry) model.getSingleSelection();
                    spix.refresh(geom);
                    model.setSingleSelection(null);
                    model.setSingleSelection(geom);
                }

            }
        });
    }

    private void checkRelocateAndDo(final File result, String defaultFolder, RequestCallback<String> callback) {
        FileIoAppState.FilePath fp = fileState.getFilePath(result);
        File assetRoot = new File(spix.getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class));
        if (!fp.assetRoot.equals(assetRoot)) {
            File j3mPath = new File(assetRoot, defaultFolder);
            if (!j3mPath.exists()) {
                j3mPath = assetRoot;
            }
            spix.getService(FileRequester.class).requestDirectory("Choose where to import " + fp.fileName + " in the asset folder", "folder",
                    j3mPath, true,
                    new RequestCallback<File>() {
                        @Override
                        public void done(File relocatePath) {
                            Path path = assetRoot.toPath().relativize(relocatePath.toPath());
                            callback.done(path.toString());
                        }
                    });
        } else {
            callback.done("");
        }
    }

    public void loadFileAsText(String path, RequestCallback<String> callback) {
        String content = fileState.loadFileAsText(path);
        callback.done(content);
    }

    public void loadAsset(AssetKey key, RequestCallback<Object> callback) {
        Object content = fileState.loadAsset(key);
        callback.done(content);
    }

    public List<String> findFilesWithExtension(String extension) {
        return fileState.findFilesWithExtension(extension);
    }
}
