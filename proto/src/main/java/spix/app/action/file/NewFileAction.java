package spix.app.action.file;

import spix.app.FileIoAppState;
import spix.core.*;
import spix.ui.FileRequester;

import java.io.File;

/**
 * Created by nehon on 27/12/16.
 */
public class NewFileAction extends AbstractAction {

    private FileIoAppState fileIoAppState;

    public NewFileAction(FileIoAppState fileIoAppState) {
        super("newFile", "New", "control N");
        this.fileIoAppState = fileIoAppState;
    }

    @Override
    public void performAction(Spix spix) {
        spix.getService(FileRequester.class).requestFile("New Scene",
                "JME Object", "j3o", new File(System.getProperty("user.home") + File.separator + "newScene.j3o"), true, false, true,
                new RequestCallback<File>() {
                    public void done(File f) {
                        System.out.println("New scene in " + f + "   Thread:" + Thread.currentThread());
                        fileIoAppState.newScene(f);
                    }
                });
    }
}
