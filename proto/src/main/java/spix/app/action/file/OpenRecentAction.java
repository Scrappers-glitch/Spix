package spix.app.action.file;

import spix.app.DefaultConstants;
import spix.app.FileIoAppState;
import spix.core.AbstractAction;
import spix.core.DefaultActionList;
import spix.core.RequestCallback;
import spix.core.Spix;
import spix.props.Property;
import spix.ui.FileRequester;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static spix.app.DefaultConstants.SCENE_FILE_NAME;

/**
 * Created by nehon on 27/12/16.
 */
public class OpenRecentAction extends DefaultActionList {

    private static final int MAX_RECENT_FILES = 5;
    private List<String> recentFiles = new ArrayList<>(MAX_RECENT_FILES);

    private FileIoAppState fileIoAppState;
    private Preferences prefs = Preferences.userNodeForPackage(OpenRecentAction.class);
    private Spix spix;

    public OpenRecentAction(Spix spix, FileIoAppState fileIoAppState) {
        super("Open Recent");
        this.fileIoAppState = fileIoAppState;
        this.spix = spix;
        spix.getBlackboard().bind(SCENE_FILE_NAME, this, "currentFile");

        loadRecentFiles();

        for (String s : recentFiles) {
            add(new OpenRecentFileAction(s,s));
        }
    }

    private void addRecentFile(String file) {
        if (recentFiles.size() == MAX_RECENT_FILES){
            recentFiles.remove(MAX_RECENT_FILES -1);
            removeLast();
        }
        recentFiles.add(0, file);
        add(0, new OpenRecentFileAction(file, file));
        for (int i = 0; i < recentFiles.size(); i++) {
            prefs.put("recent.file." + i, recentFiles.get(i));
        }
    }
    private void loadRecentFiles() {
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            String s = prefs.get("recent.file." + i, null);
            if (s!=null) {
                recentFiles.add(s);
            }
        }
    }
    public List<String> getRecentFiles() {
        return recentFiles;
    }

    private class OpenRecentFileAction extends AbstractAction {

        String path;

        protected OpenRecentFileAction(String id, String path) {
            super(id);
            this.path = path;
        }

        @Override
        public void performAction(Spix spix) {
            fileIoAppState.loadFile(new File(path));
        }
    }

    public void setCurrentFile(Property currentFileProp) {
        String assetRoot = (String)((Property)spix.getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER)).getValue();
        addRecentFile(assetRoot + File.separator + currentFileProp.getValue());

    }

}
