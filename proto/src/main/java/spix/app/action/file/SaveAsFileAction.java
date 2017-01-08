package spix.app.action.file;

import spix.app.FileIoAppState;
import spix.core.*;
import spix.ui.FileRequester;

import java.io.File;

import static spix.app.DefaultConstants.MAIN_ASSETS_FOLDER;
import static spix.app.DefaultConstants.SCENE_FILE_NAME;

/**
 * Created by nehon on 27/12/16.
 */
public class SaveAsFileAction extends AbstractAction {

    private FileIoAppState fileIoAppState;

    public SaveAsFileAction(FileIoAppState fileIoAppState) {
        super("saveAsFile", "Save As...");
        this.fileIoAppState = fileIoAppState;
    }

    @Override
    public void performAction(Spix spix) {
        String mainAssetRoot = spix.getBlackboard().get(MAIN_ASSETS_FOLDER, String.class);
        String fileName = spix.getBlackboard().get(SCENE_FILE_NAME, String.class);
        spix.getService(FileRequester.class).requestFile("Save As",
                "JME Object", "j3o", new File(fileIoAppState.getUnusedName(mainAssetRoot, fileName)), false, false, false,
                new RequestCallback<File>() {
                    public void done(File f) {
                        System.out.println("Save as :" + f + "   Thread:" + Thread.currentThread());
                        fileIoAppState.saveAs(f.getPath());
                    }
                });
    }
}
