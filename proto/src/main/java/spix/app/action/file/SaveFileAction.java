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
public class SaveFileAction extends AbstractAction {

    private FileIoAppState fileIoAppState;

    public SaveFileAction(FileIoAppState fileIoAppState) {
        super("saveFile", "Save", "control S");
        this.fileIoAppState = fileIoAppState;
    }

    @Override
    public void performAction(Spix spix) {
        fileIoAppState.save();
    }
}
