package spix.swing.materialEditor.panels;

import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.shader.*;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import spix.app.material.MaterialService;
import spix.core.RequestCallback;
import spix.swing.SwingGui;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.utils.NoneSelectedButtonGroup;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Created by Nehon on 03/06/2016.
 */
public class ShaderCodePanel extends DockPanel {

    private ShaderCodeEditor editor;
    private SwingGui gui;
    private Map<Shader.ShaderType, JToggleButton> buttons = new HashMap<>();
    private Map<String, Shader> shaders;
    private String currentShaderVersion;
    private Shader.ShaderType currentShaderType;
    private NoneSelectedButtonGroup group = new NoneSelectedButtonGroup();
    private JToolBar toolbar;

    public ShaderCodePanel(Container container, SwingGui gui) {
        super(Slot.West, container);
        this.gui = gui;

        editor = new ShaderCodeEditor();

        JPanel panel = new JPanel(new BorderLayout());
        RTextScrollPane scrollPane = new RTextScrollPane(editor);
        panel.add(scrollPane, BorderLayout.CENTER);

        setComponent(panel);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                editor.fitContent();
            }
        });

        setTitle("Shader Code");
        setIcon(Icons.code);
        button.setIcon(Icons.shaderCode);
        button.setRolloverIcon(Icons.shaderCodeHover);

        toolbar = new JToolBar("Generated shader code");
        toolbar.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        toolbar.setFloatable(false);

        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
        JComboBox<String> cb = new JComboBox<>(comboBoxModel);
        comboBoxModel.addElement("GLSL100");
        comboBoxModel.addElement("GLSL150");
        currentShaderVersion = "GLSL100";

        cb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentShaderVersion = (String) cb.getModel().getSelectedItem();
                updateText();
            }
        });

        toolbar.addSeparator();
        JLabel versionLablel = new JLabel("Version");
        toolbar.add(versionLablel);
        toolbar.add(cb);

        panel.add(toolbar, BorderLayout.NORTH);

    }

    public void refreshCode(TechniqueDef def, MaterialDef matDef) {
        gui.getSpix().getService(MaterialService.class).requestCode(def, matDef, new RequestCallback<Map<String, Shader>>() {
            @Override
            public void done(Map<String, Shader> result) {
                if (result == null) {
                    return;
                }
                shaders = result;
                Shader s = shaders.get(currentShaderVersion);
                if (s == null) {
                    return;
                }
                for (JToggleButton b : buttons.values()) {
                    toolbar.remove(b);
                }

                for (Shader.ShaderSource shaderSource : s.getSources()) {
                    JToggleButton button = makeButton(shaderSource.getType());
                    toolbar.add(button, 0);
                    if (currentShaderType == null) {
                        currentShaderType = shaderSource.getType();
                    }

                    if (currentShaderType == shaderSource.getType()) {
                        button.setSelected(true);
                    }
                }

                updateText();
            }
        });

    }

    private JToggleButton makeButton(Shader.ShaderType type) {
        JToggleButton button = buttons.get(type);
        if (button == null) {
            button = new JToggleButton(type.name());
            buttons.put(type, button);
            group.add(button);
            switch (type) {
                case Vertex:
                    button.setIcon(Icons.vert);
                    break;
                case Fragment:
                    button.setIcon(Icons.frag);
                    break;
                default:
                    button.setIcon(Icons.attrib);
                    break;
            }
        }
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentShaderType = type;
                updateText();
            }
        });
        return button;
    }

    private void updateText() {
        Shader shader = shaders.get(currentShaderVersion);
        if (shader == null) {
            return;
        }
        for (Shader.ShaderSource shaderSource : shader.getSources()) {
            if (shaderSource.getType() == currentShaderType) {
                editor.setText(shaderSource.getSource());
            }
        }
    }

}
