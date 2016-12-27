package spix.app.action.file;

import spix.app.FileIoAppState;
import spix.core.AbstractAction;
import spix.core.RequestCallback;
import spix.core.Spix;
import spix.ui.FileRequester;

import java.io.File;

/**
 * Created by nehon on 27/12/16.
 */
public class ImportFileAction extends AbstractAction {

    private FileIoAppState fileIoAppState;

    public ImportFileAction(FileIoAppState fileIoAppState) {
        super("importFile", "Import Asset", "control I");
        this.fileIoAppState = fileIoAppState;
    }

    @Override
    public void performAction(Spix spix) {
        spix.getService(FileRequester.class).requestFile("Open asset",
                "JME Object", "j3o", null, true,
                new RequestCallback<File>() {
                    public void done(File f) {
                        System.out.println("Need to load:" + f + "   Thread:" + Thread.currentThread());
                        fileIoAppState.AppendFile(f);
                    }
                });
    }
}
