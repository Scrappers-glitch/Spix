package spix.swing.materialEditor;

import com.jme3.scene.Geometry;
import spix.app.DefaultConstants;
import spix.core.SelectionModel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.controller.MatDefEditorController;
import spix.swing.materialEditor.icons.Icons;
import spix.undo.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.prefs.Preferences;

/**
 * Created by Nehon on 11/05/2016.
 */
public class MatDefEditorWindow extends JFrame {

    public static final String MAT_DEF_EDITOR_WIDTH = "MatDefEditor.width";
    public static final String MAT_DEF_EDITOR_HEIGHT = "MatDefEditor.height";
    public static final String MAT_DEF_EDITOR_X = "MatDefEditor.x";
    public static final String MAT_DEF_EDITOR_Y = "MatDefEditor.y";
    private SwingGui gui;
    private Preferences prefs = Preferences.userNodeForPackage(MatDefEditorWindow.class);
    private MatDefEditorController controller;


    public MatDefEditorWindow(SwingGui gui) {
        super("Material definition editor");
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        controller = new MatDefEditorController(gui, this);
        this.gui = gui;
        setIconImages(Arrays.asList(new Image[]{Icons.logo16.getImage(), Icons.logo32.getImage(), Icons.logo64.getImage(), Icons.logo128.getImage()}));

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

            @Override
            public void componentShown(ComponentEvent e) {
                controller.initialize();
            }
        });

        //register blackboard bindings
        gui.getSpix().getBlackboard().bind(UndoManager.LAST_EDIT, this, "lastEdit");

    }

    public void setLastEdit(Edit edit) {
        if (!isVisible()) {
            return;
        }

        SelectionModel m = gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        if (!(m.getSingleSelection() instanceof Geometry)) {
            return;
        }

        PropertyEdit pe = null;
        if (edit instanceof CompositeEdit) {
            pe = ((CompositeEdit) edit).getEditWithType(PropertyEdit.class);
        } else if (edit instanceof PropertyEdit) {
            pe = (PropertyEdit) edit;
        }
        if (pe == null) {
            return;
        }

        controller.onSelectionPropertyChange((Geometry) m.getSingleSelection());


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
