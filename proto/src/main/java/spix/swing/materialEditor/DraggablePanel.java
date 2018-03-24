/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor;

import spix.swing.materialEditor.controller.MatDefEditorController;

import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Nehon
 */
public class DraggablePanel extends JPanel {

    protected int svdx, svdy, svdex, svdey;
    protected MatDefEditorController controller;

    public DraggablePanel(MatDefEditorController controller) {
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

            @Override
            public void mouseMoved(MouseEvent e) {
                DraggablePanel.this.onMouseMoved(e);
            }
        });
    }

    public void onMousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON2) {
            svdex = e.getXOnScreen();
            svdey = e.getYOnScreen();
            saveLocation();
            controller.multiStartDrag(this);
            e.consume();
        }
    }

    public void onMouseDragged(MouseEvent e) {
        if (!SwingUtilities.isMiddleMouseButton(e)) {
            int xoffset = e.getLocationOnScreen().x - svdex;
            int yoffset = e.getLocationOnScreen().y - svdey;
            movePanel(xoffset, yoffset);
            controller.multiMove(this, xoffset, yoffset);
            e.consume();
        }
    }

    private int snap(int value, double step) {
        return (int) (Math.floor(((double) value) / step) * step);
    }

    public void onMouseReleased(MouseEvent e) {
    }

    public void onMouseMoved(MouseEvent e) {
    }

    public void saveLocation() {
        svdy = getLocation().y;
        svdx = getLocation().x;
    }

    public void movePanel(int xoffset, int yoffset) {
        setLocation(Math.max(0, snap(svdx + xoffset, 10)), Math.max(0, snap(svdy + yoffset, 10)));
    }

}
