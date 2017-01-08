package spix.app;

import com.jme3.texture.Texture;
import spix.core.RequestCallback;
import spix.core.Spix;
import spix.ui.FileRequester;

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
                        FileIoAppState.FilePath fp = fileState.getFilePath(result);
                        if (!fp.assetRoot.equals(assetRoot)) {
                            File texturePath = new File(assetRoot, "Textures");
                            if (!texturePath.exists()) {
                                texturePath = assetRoot;
                            }
                            spix.getService(FileRequester.class).requestDirectory("Choose where to import " + fp.fileName + " in the asset folder", "folder",
                                    texturePath, true,
                                    new RequestCallback<File>() {
                                        @Override
                                        public void done(File relocatePath) {
                                            Path path = assetRoot.toPath().relativize(relocatePath.toPath());
                                            Texture tex = fileState.loadTexture(result, path.toString());
                                            callback.done(tex);
                                        }
                                    });
                        } else {
                            Texture tex = fileState.loadTexture(result, "");
                            callback.done(tex);
                        }

                    }
                });
    }
//

}
