package spix.swing.materialEditor.panels;

import com.jme3.shader.Shader;
import com.jme3.shader.ShaderNode;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;
import spix.app.FileIoService;
import spix.core.RequestCallback;
import spix.swing.SwingGui;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.utils.NoneSelectedButtonGroup;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nehon on 03/06/2016.
 */
public class ShaderNodeCodePanel extends DockPanel {

    private ShaderCodeEditor editor;
    private SwingGui gui;
    private JToolBar toolbar;

    private java.util.List<String> fileNames = new ArrayList<>();
    private Map<String, Document> fileContents = new HashMap<>();
    private java.util.List<JToggleButton> tbButtons = new ArrayList<>();
    private int lastButtonIndex = 0;
    private ButtonGroup group = new ButtonGroup();
    private Document currentDocument;
    private RTextScrollPane scrollPane;

    public ShaderNodeCodePanel(Container container, SwingGui gui) {
        super(Slot.West, container);
        this.gui = gui;

        editor = new ShaderCodeEditor();
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (editor.isEditable()) {
                    currentDocument.modified = true;
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        scrollPane = new RTextScrollPane(editor);
        scrollPane.setIconRowHeaderEnabled(true);
        panel.add(scrollPane, BorderLayout.CENTER);

        setComponent(panel);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                editor.fitContent();
            }
        });

        setTitle("Shader Node");
        setIcon(Icons.node);
        button.setIcon(Icons.shaderNode);
        button.setRolloverIcon(Icons.shaderNodeHover);

        toolbar = new JToolBar("Generated shader code");
        toolbar.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        toolbar.setFloatable(false);

        panel.add(toolbar, BorderLayout.NORTH);

    }

    public void setError(String documentName, IOException e) {
        Document doc = fileContents.get(documentName);
        if (!e.getMessage().contains(":")) {
            doc.errorMessage = e.getMessage();
            doc.errorLine = 1;
        } else {
            Pattern pattern = Pattern.compile("Error On line ([0-9]+) :");
            Matcher m = pattern.matcher(e.getMessage());
            m.find();
            try {
                doc.errorLine = Integer.parseInt(m.group(1));
            } catch (IllegalStateException | NumberFormatException e1) {
                doc.errorLine = 1;
            }
            doc.errorMessage = e.getMessage().substring(e.getMessage().indexOf(":") + 1, e.getMessage().length());
        }
    }

    public void clearError(String documentName) {
        Document doc = fileContents.get(documentName);
        doc.errorLine = 0;
        doc.errorMessage = null;
    }

    public void refreshErrors() {
        for (String key : fileContents.keySet()) {
            Document doc = fileContents.get(key);
            for (JToggleButton tbButton : tbButtons) {
                if (tbButton.getActionCommand().equals(key)) {
                    if (doc.errorLine > 0) {
                        tbButton.setIcon(Icons.errorSmall);
                    } else {
                        tbButton.setIcon(null);
                    }
                }
            }
        }

        refreshCurrentDocError();
    }

    public void refreshCurrentDocError() {
        removeCurrentDocError();
        if (currentDocument.errorLine > 0) {

            try {
                currentDocument.gutterInfo = scrollPane.getGutter().addLineTrackingIcon(currentDocument.errorLine - 1, Icons.errorSmall, currentDocument.errorMessage);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

    }

    public void removeCurrentDocError() {
        if (currentDocument.gutterInfo != null) {
            scrollPane.getGutter().removeTrackingIcon(currentDocument.gutterInfo);
            currentDocument.gutterInfo = null;
        }
    }

    public void setSelectedNode(Object item) {
        if (!(item instanceof ShaderNode)) {
            return;
        }
        fileNames.clear();
        fileContents.clear();
        toolbar.removeAll();
        lastButtonIndex = 0;
        ShaderNode node = (ShaderNode) item;
        fileNames.add(node.getDefinition().getPath());
        fileNames.addAll(node.getDefinition().getShadersPath());

        for (String fileName : fileNames) {

            gui.getSpix().getService(FileIoService.class).loadFileAsText(fileName, new RequestCallback<String>() {
                @Override
                public void done(String result) {
                    if (result == null) {
                        result = "";
                    }
                    fileContents.put(fileName, new Document(fileName, result, node));
                }
            });

            JToggleButton b = null;
            if (lastButtonIndex < tbButtons.size()) {
                b = tbButtons.get(lastButtonIndex);
            }
            if (b == null) {
                b = new JToggleButton();
                tbButtons.add(b);
                b.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        removeCurrentDocError();
                        String name = ((JToggleButton) e.getSource()).getActionCommand();
                        updateEditorText(name);
                        refreshCurrentDocError();
                    }
                });
                group.add(b);
            }
            toolbar.add(b);
            lastButtonIndex++;
            b.setText(fileName.substring(fileName.lastIndexOf('/') + 1));
            b.setToolTipText(fileName);
            b.setActionCommand(fileName);
        }
        tbButtons.get(0).setSelected(true);
        updateEditorText(fileNames.get(0));
    }

    public void updateEditorText(final String name) {
        gui.getService(FileIoService.class).isFileWritable(name, new RequestCallback<Boolean>() {
            @Override
            public void done(Boolean result) {
                if (currentDocument != null && currentDocument.modified) {
                    currentDocument.content = editor.getText();
                }
                currentDocument = fileContents.get(name);
                editor.setText(currentDocument.content);
                editor.setEditable(result);
            }
        });
    }

    public Collection<Document> getDocuments() {
        if (currentDocument != null && currentDocument.modified) {
            currentDocument.content = editor.getText();
        }
        return fileContents.values();
    }

    public static class Document {
        private String name;
        private String content;
        private boolean modified;
        private ShaderNode associatedNode;
        private int errorLine = 0;
        private String errorMessage = null;
        private GutterIconInfo gutterInfo;

        public Document(String name, String content, ShaderNode associatedNode) {
            this.name = name;
            this.content = content;
            this.associatedNode = associatedNode;
        }

        public String getName() {
            return name;
        }

        public String getContent() {
            return content;
        }

        public boolean isModified() {
            return modified;
        }

        public ShaderNode getAssociatedNode() {
            return associatedNode;
        }

        public void setModified(boolean modified) {
            this.modified = modified;
        }
    }
}
