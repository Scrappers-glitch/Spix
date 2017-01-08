package spix.swing;

import javax.swing.*;

/**
 * Created by Nehon on 07/01/2017.
 */
public abstract class FileChooserAccessory extends JPanel {

    public FileChooserAccessory() {
        super();
    }

    abstract void setFileChooser(JFileChooser fileChooser);

}
