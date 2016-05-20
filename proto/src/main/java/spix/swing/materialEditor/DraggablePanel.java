/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor;

import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Nehon
 */
public class DraggablePanel extends JPanel {

    protected int svdx, svdy, svdex, svdey;
    protected Diagram diagram;

    public DraggablePanel() {
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
            diagram.multiStartDrag(this);
            e.consume();
        }
    }

    public void onMouseDragged(MouseEvent e) {
        if (!SwingUtilities.isMiddleMouseButton(e)) {
            int xoffset = e.getLocationOnScreen().x - svdex;
            int yoffset = e.getLocationOnScreen().y - svdey;
            movePanel(xoffset, yoffset);
            diagram.multiMove(this, xoffset, yoffset);
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

    public Diagram getDiagram() {
        return diagram;
    }

    public void setDiagram(Diagram diagram) {
        this.diagram = diagram;
    }
}
