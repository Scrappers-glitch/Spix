package spix.app;

import com.jme3.asset.MaterialKey;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import spix.app.material.MaterialService;
import spix.core.*;
import spix.ui.FileRequester;
import spix.undo.SceneGraphStructureEdit;
import spix.undo.UndoManager;
import spix.undo.edit.MaterialSetEdit;

import java.io.File;
import java.nio.file.Path;

/**
 * Created by Nehon on 08/01/2017.
 */
public class FileLoadingService {

    private FileIoAppState fileState;
    private Spix spix;

    public FileLoadingService(Spix spix, FileIoAppState fileState) {
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
                                    Material oldMat = geom.getMaterial();
                                    geom.setMaterial(mat);
                                    spix.refresh(geom);
                                    UndoManager um = spix.getService(UndoManager.class);
                                    MaterialSetEdit edit = new MaterialSetEdit(geom, oldMat, mat);
                                    um.addEdit(edit);
                                    model.setSingleSelection(null);
                                    model.setSingleSelection(geom);
                                }
                            });
                        }
                    });
        }
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
                            Material oldMat = geom.getMaterial();
                            geom.setMaterial(mat);
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

}
