package spix.util;

import javax.swing.*;
import javax.swing.event.MenuDragMouseEvent;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by Nehon on 21/05/2016.
 */
public class SwingUtils {

    /**
     * Workaround for swing utilities removing mouse button when converting events.
     * @param e
     * @param targetComponent
     * @return
     */
    public static MouseEvent convertEvent( Component sourceComponent, MouseEvent e,Component targetComponent) {
        MouseEvent me = null;

        if (e instanceof MouseWheelEvent || e instanceof MenuDragMouseEvent) {
            SwingUtilities.convertMouseEvent(sourceComponent, e, targetComponent);
        } else {
            Point p = SwingUtilities.convertPoint(sourceComponent, new Point(e.getX(),
                            e.getY()),
                    targetComponent);

            me = new MouseEvent(targetComponent,
                    e.getID(),
                    e.getWhen(),
                    e.getModifiers()
                            | e.getModifiersEx(),
                    p.x, p.y,
                    e.getXOnScreen(),
                    e.getYOnScreen(),
                    e.getClickCount(),
                    e.isPopupTrigger(),
                    e.getButton());
        }
        return me;
    }
}
