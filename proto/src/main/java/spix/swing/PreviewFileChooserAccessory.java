package spix.swing;

import spix.app.material.MaterialService;
import spix.core.RequestCallback;
import spix.swing.materialEditor.preview.MaterialPreviewRenderer;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

/**
 * Created by Nehon on 07/01/2017.
 */
public class PreviewFileChooserAccessory extends FileChooserAccessory {

    private SwingGui gui;
    private JFileChooser chooser;
    private JLabel img;
    private static int MAX_HEIGHT = 128;
    private static int MAX_WIDTH = 225;

    public PreviewFileChooserAccessory(SwingGui gui) {
        super();
        this.gui = gui;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        img = new JLabel();
        img.setPreferredSize(new Dimension(128, 128));
        add(img);
    }

    @Override
    void setFileChooser(JFileChooser fileChooser) {
        chooser = fileChooser;

        chooser.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    if (chooser.getSelectedFile() != null && isImageType(chooser.getSelectedFile().getCanonicalPath())) {
                        gui.getSpix().getService(MaterialService.class).requestTexturePreview(chooser.getSelectedFile().getPath(), new RequestCallback<MaterialService.PreviewResult>() {
                            @Override
                            public void done(MaterialService.PreviewResult result) {
                                ImageIcon icon = new ImageIcon(MaterialPreviewRenderer.convert(result.imageData, result.width, result.height));
                                float ratio = (float) result.originalWidth / (float) result.originalHeight;
                                int height = MAX_HEIGHT;
                                int width = (int) (ratio * (float) height);
                                if (width > MAX_WIDTH) {
                                    width = MAX_WIDTH;
                                    height = (int) ((float) width / ratio);
                                }

                                Image i = icon.getImage().getScaledInstance(width, height, Image.SCALE_FAST);
                                img.setIcon(new ImageIcon(i));
                                img.setPreferredSize(new Dimension(width, height));
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });


    }

    private boolean isImageType(String path) {
        String[] exts = new String[]{".jpg", ".bmp", ".gif", ".png", ".jpeg", ".hdr", ".pfm", ".dds", ".tga"};
        for (String ext : exts) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
