package spix.app.action.file;

import spix.app.FileIoAppState;
import spix.app.SpixState;
import spix.core.AbstractAction;
import spix.core.RequestCallback;
import spix.core.Spix;
import spix.props.Property;
import spix.ui.FileRequester;

import java.io.File;

import static spix.app.DefaultConstants.*;

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
                "JME Object", "j3o", new File(fileIoAppState.getUnusedName(mainAssetRoot, fileName)), false,
                new RequestCallback<File>() {
                    public void done(File f) {
                        System.out.println("Save as :" + f + "   Thread:" + Thread.currentThread());
                        fileIoAppState.saveAs(f.getPath());
                    }
                });
    }
}
