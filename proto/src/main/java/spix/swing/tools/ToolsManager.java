package spix.swing.tools;

import spix.swing.SwingGui;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

public class ToolsManager {

    private JPanel scenePane;
    private SwingGui gui;
    private JToolBar tools;
    private JPanel currentTool;
    private Map<String, JPanel> toolPanels = new HashMap<>();

    public ToolsManager(JPanel scenePane, SwingGui gui) {
        this.scenePane = scenePane;
        tools = new JToolBar("Tools");
        tools.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        tools.setOrientation(JToolBar.VERTICAL);
        this.scenePane.add(tools, BorderLayout.WEST);
        toolPanels.put("VertexPainting", new VertexPaintingTool(gui));

        gui.getSpix().getBlackboard().addListener("tools.active", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String tool = (String)evt.getNewValue();
                if(currentTool != null) {
                    tools.remove(currentTool);
                }
                currentTool = toolPanels.get(tool);
                if(currentTool != null) {
                    tools.add(currentTool);
                }

                tools.revalidate();
                tools.repaint();
                scenePane.revalidate();
                scenePane.repaint();
            }
        });
    }


}
