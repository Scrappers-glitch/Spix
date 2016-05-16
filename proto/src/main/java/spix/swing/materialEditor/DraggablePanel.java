/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Nehon
 */
public class DraggablePanel extends JPanel implements MouseListener, MouseMotionListener {

    protected int svdx, svdy, svdex, svdey;
    protected Diagram diagram;

    public DraggablePanel() {
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON2) {
            svdex = e.getXOnScreen();
            svdey = e.getYOnScreen();
            saveLocation();
            diagram.multiStartDrag(this);
            e.consume();
        }
    }

    protected void saveLocation() {
        svdy = getLocation().y;
        svdx = getLocation().x;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!SwingUtilities.isMiddleMouseButton(e)) {
            int xoffset = e.getLocationOnScreen().x - svdex;
            int yoffset = e.getLocationOnScreen().y - svdey;
            movePanel(xoffset, yoffset);
            diagram.multiMove(this, xoffset, yoffset);
            e.consume();
        }
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
