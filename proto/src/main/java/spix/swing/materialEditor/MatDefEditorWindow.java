package spix.swing.materialEditor;

import com.jme3.material.MaterialDef;
import com.jme3.scene.Geometry;
import com.jme3.shader.*;
import spix.app.DefaultConstants;
import spix.core.*;
import spix.props.PropertySet;
import spix.swing.*;
import spix.swing.materialEditor.nodes.NodePanel;
import spix.swing.materialEditor.nodes.inOut.*;
import spix.swing.materialEditor.nodes.inputs.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
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
    private SwingGui gui;
    private SelectionModel sceneSelection;
    private SceneSelectionChangeListener sceneSelectionChangeListener = new SceneSelectionChangeListener();

    public MatDefEditorWindow(SwingGui gui) {
        super("Material definition editor");
        this.gui = gui;
        setDefaultCloseOperation(HIDE_ON_CLOSE);
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

        Diagram diagram = new Diagram(gui);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(diagram);
        getContentPane().add(scrollPane);
//        diagram.setPreferredSize(new Dimension(scrollPane.getWidth() - 2, scrollPane.getHeight() - 2));
//        diagram.revalidate();
        scrollPane.addComponentListener(diagram);
        //diagram.addOutBus(new OutBusPanel("Test", Shader.ShaderType.Fragment));

        diagram.addNode(new AttributePanel(new ShaderNodeVariable("vec2", "inTexCoord")));
        diagram.addNode(new WorldParamPanel(new ShaderNodeVariable("mat4", "WorldProjectionMatrix")));
        diagram.addNode(new MatParamPanel(new ShaderNodeVariable("vec4", "Color")));

        diagram.addNode(new VertexPositionPanel());
        diagram.addNode(new FragmentColorPanel());

        diagram.autoLayout();
        sceneSelection = gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        sceneSelection.addPropertyChangeListener(sceneSelectionChangeListener);

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
        sceneSelection.removePropertyChangeListener(sceneSelectionChangeListener);
        super.dispose();
    }

    public SwingGui getGui() {
        return gui;
    }

    private class SceneSelectionChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getNewValue() instanceof Geometry) {
                Geometry g = (Geometry) evt.getNewValue();
                PropertySet set = gui.getSpix().getPropertySetFactory(MaterialDef.class).createPropertySet(g.getMaterial().getMaterialDef(), gui.getSpix());
                SwingPropertySetWrapper wrapper = new SwingPropertySetWrapper(gui, set);
                setTitle((String) wrapper.getProperty("name").getValue());
            }
        }
    }
}
