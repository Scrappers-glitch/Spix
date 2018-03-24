package spix.swing.materialEditor.controller;

import spix.swing.materialEditor.*;
import spix.swing.materialEditor.nodes.NodePanel;
import spix.swing.materialEditor.nodes.ShaderNodePanel;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by Nehon on 22/05/2016.
 */
public class SelectionHandler {

    protected List<Selectable> selectedItems = new ArrayList<>();

    public void removeSelected(MatDefEditorController controller) {

        for (Selectable selectedItem : selectedItems) {
            if (selectedItem instanceof NodePanel) {
                controller.removeNode(selectedItem.getKey());
            }
            if (selectedItem instanceof Connection) {
                controller.removeConnection((Connection) selectedItem);
            }
        }
        selectedItems.clear();
    }

    public List<NodePanel> getSelectedNodePanels() {
        List<NodePanel> list = new ArrayList<>();

        for (Selectable selectedItem : selectedItems) {
            if (selectedItem instanceof NodePanel) {
                NodePanel panel = ((NodePanel)selectedItem);
                list.add(panel);
            }
        }
        clearSelection();
        return list;
    }

    public List<Selectable> getSelectedItems() {
        return selectedItems;
    }

    /**
     * selection from the editor. Select the item and notify the topComponent
     *
     * @param selectable
     */
    public void select(Selectable selectable, boolean multi) {
        doSelect(selectable, multi);
    }

    public void multiMove(DraggablePanel movedPanel, int xOffset, int yOffset) {

        for (Selectable selectedItem : selectedItems) {
            if (selectedItem != movedPanel) {
                if (selectedItem instanceof DraggablePanel) {
                    ((DraggablePanel) selectedItem).movePanel(xOffset, yOffset);
                }
            }
        }
    }

    public void multiStartDrag(DraggablePanel movedPanel) {
        for (Selectable selectedItem : selectedItems) {
            if (selectedItem != movedPanel) {
                if (selectedItem instanceof DraggablePanel) {
                    ((DraggablePanel) selectedItem).saveLocation();
                }
            }
        }
    }

    public void multiStopDrag(MatDefEditorController controller) {
        for (Selectable selectedItem : selectedItems) {
            if (selectedItem instanceof NodePanel) {
                controller.onNodeMoved(((NodePanel) selectedItem));
            }
        }
    }

    /**
     * do select the item and repaint the diagram
     *
     * @param selectable
     * @return
     */
    private Selectable doSelect(Selectable selectable, boolean multi) {

        if (!multi && !selectedItems.contains(selectable)) {
            clearSelection();
        }

        if (selectable != null) {
            selectedItems.add(selectable);
            selectable.setSelected(true);
        }

        if (selectable instanceof Component) {
            ((Component) selectable).requestFocusInWindow();
        }


        return selectable;
    }

    void clearSelection() {
        for (Selectable selectedItem : selectedItems) {
            selectedItem.setSelected(false);
        }
        selectedItems.clear();
    }
}
