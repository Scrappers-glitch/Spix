package spix.app;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
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
                                Texture tex = fileState.loadTexture(result, relocateTo);
                                callback.done(tex);
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

    public void checkRelocateAndDo(final File result, String defaultFolder, RequestCallback<String> callback) {
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
