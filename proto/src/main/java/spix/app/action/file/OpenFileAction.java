package spix.app.action.file;

import spix.app.FileIoAppState;
import spix.core.*;
import spix.ui.FileRequester;

import java.io.File;

/**
 * Created by nehon on 27/12/16.
 */
public class OpenFileAction extends AbstractAction {

    private FileIoAppState fileIoAppState;

    public OpenFileAction(FileIoAppState fileIoAppState) {
        super("openFile", "Open", "control O");
        this.fileIoAppState = fileIoAppState;
    }

    @Override
    public void performAction(Spix spix) {
        spix.getService(FileRequester.class).requestFile("Open Scene",
                "JME Object", "j3o, gltf, obj, mesh, zip", null, true, false, false,
                new RequestCallback<File>() {
                    public void done(File f) {
                        System.out.println("Need to load:" + f + "   Thread:" + Thread.currentThread());
                        fileIoAppState.loadFile(f);
                    }
                });
    }
}
