package spix.swing.materialEditor;

import spix.swing.SwingGui;
import spix.swing.materialEditor.controller.MatDefEditorController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.Preferences;

/**
 * Created by Nehon on 11/05/2016.
 */
public class MatDefEditorWindow extends JFrame {

    public static final String MAT_DEF_EDITOR_WIDTH = "MatDefEditor.width";
    public static final String MAT_DEF_EDITOR_HEIGHT = "MatDefEditor.height";
    public static final String MAT_DEF_EDITOR_X = "MatDefEditor.x";
    public static final String MAT_DEF_EDITOR_Y = "MatDefEditor.y";
    private Preferences prefs = Preferences.userNodeForPackage(MatDefEditorWindow.class);
    private MatDefEditorController controller;


    public MatDefEditorWindow(SwingGui gui) {
        super("Material definition editor");
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        controller = new MatDefEditorController(gui, this);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                prefs.putInt(MAT_DEF_EDITOR_WIDTH, e.getComponent().getWidth());
                prefs.putInt(MAT_DEF_EDITOR_HEIGHT, e.getComponent().getHeight());
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                prefs.putInt(MAT_DEF_EDITOR_X, e.getComponent().getX());
                prefs.putInt(MAT_DEF_EDITOR_Y, e.getComponent().getY());
            }
        });


    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            int width = prefs.getInt(MAT_DEF_EDITOR_WIDTH, 300);
            int height = prefs.getInt(MAT_DEF_EDITOR_HEIGHT, 150);

            int x = prefs.getInt(MAT_DEF_EDITOR_X, 300);
            int y = prefs.getInt(MAT_DEF_EDITOR_Y, 150);

            setSize(new Dimension(width, height));
            setLocation(x, y);
        }
        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        controller.cleanup();
        super.dispose();
    }


}
