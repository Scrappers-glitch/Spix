/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor;

import spix.swing.materialEditor.controller.MaterialDefController;

import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Nehon
 */
public class DraggablePanel extends JPanel {

    protected int svdx, svdy, svdex, svdey;
    protected MaterialDefController controller;

    public DraggablePanel(MaterialDefController controller) {
        this.controller = controller;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                DraggablePanel.this.onMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                DraggablePanel.this.onMouseReleased(e);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                DraggablePanel.this.onMouseDragged(e);
            }
        });
    }

    public void onMousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON2) {
            svdex = e.getXOnScreen();
            svdey = e.getYOnScreen();
            saveLocation();
            controller.getEditor().getDiagram().multiStartDrag(this);
            e.consume();
        }
    }

    public void onMouseDragged(MouseEvent e) {
        if (!SwingUtilities.isMiddleMouseButton(e)) {
            int xoffset = e.getLocationOnScreen().x - svdex;
            int yoffset = e.getLocationOnScreen().y - svdey;
            movePanel(xoffset, yoffset);
            controller.getEditor().getDiagram().multiMove(this, xoffset, yoffset);
            e.consume();
        }
    }

    public void onMouseReleased(MouseEvent e) {
    }

    protected void saveLocation() {
        svdy = getLocation().y;
        svdx = getLocation().x;
    }

    protected void movePanel(int xoffset, int yoffset) {
        setLocation(Math.max(0, svdx + xoffset), Math.max(0, svdy + yoffset));
    }

}
