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
    private JPanel dummyTool;
    private Map<String, JPanel> toolPanels = new HashMap<>();

    public ToolsManager(JPanel scenePane, SwingGui gui) {
        this.scenePane = scenePane;
        tools = new JToolBar("Tools");
        tools.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        tools.setOrientation(JToolBar.HORIZONTAL);
        this.scenePane.add(tools, BorderLayout.SOUTH);
        makeDummyTool();
        currentTool = dummyTool;
        refresh(scenePane);

        toolPanels.put("VertexPainting", new VertexPaintingTool(gui));
        toolPanels.put("LightProbe", new LightProbeTool(gui));

        gui.getSpix().getBlackboard().addListener("tools.active", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String tool = (String)evt.getNewValue();
                if(currentTool != null) {
                    tools.remove(currentTool);
                }
                currentTool = toolPanels.get(tool);
                if(currentTool == null) {
                    currentTool = dummyTool;
                }
                refresh(scenePane);
            }
        });
    }

    public void refresh(JPanel scenePane) {
        tools.add(currentTool);
        tools.revalidate();
        tools.repaint();
        scenePane.revalidate();
        scenePane.repaint();
    }

    private void makeDummyTool(){
        dummyTool = new JPanel();
        Dimension size = new Dimension(25, 25);
        dummyTool.setMinimumSize(size);
        dummyTool.setPreferredSize(size);
        dummyTool.setMaximumSize(size);
    }

}
