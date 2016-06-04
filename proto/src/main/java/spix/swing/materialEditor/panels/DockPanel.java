package spix.swing.materialEditor.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by Nehon on 03/06/2016.
 */
public class DockPanel extends JPanel {

    public static final int MARGIN = 5;

    public enum Slot{
        North,
        East,
        West,
        South
    }

    private String layoutSlot;
    protected JToggleButton button;
    private Container container;
    private Component component;

    public DockPanel(Slot slot, Container container){
        super(new BorderLayout());

        if(!(container.getLayout() instanceof BorderLayout)){
            throw new IllegalArgumentException("Container must have a BorderLayout");
        }
        this.container = container;

        setVisible(false);
        switch (slot){
            case North:
                setBorder(BorderFactory.createEmptyBorder(0,0, MARGIN,0));
                setCursor( Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                layoutSlot = BorderLayout.NORTH;
                break;
            case South:
                setBorder(BorderFactory.createEmptyBorder(MARGIN,0,0,0));
                setCursor( Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                layoutSlot = BorderLayout.SOUTH;
                break;
            case West:
                setBorder(BorderFactory.createEmptyBorder(0,0,0, MARGIN));
                setCursor( Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                layoutSlot = BorderLayout.WEST;
                break;
            case East:
                setBorder(BorderFactory.createEmptyBorder(0, MARGIN,0,0));
                setCursor( Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                layoutSlot = BorderLayout.EAST;
                break;
        }

        button = new JToggleButton();
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(isVisible()){
                    dock();
                } else {
                    Component c = ((BorderLayout) container.getLayout()).getLayoutComponent(layoutSlot);
                    if (c != null) {
                        if (c instanceof DockPanel) {
                            ((DockPanel) c).dock();
                        } else {
                            container.remove(c);
                        }
                        setPreferredSize(c.getPreferredSize());
                    }
                    container.add(DockPanel.this, layoutSlot);
                    setVisible(true);
                }
                container.revalidate();
                container.repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                MouseEvent ev = SwingUtilities.convertMouseEvent(DockPanel.this ,e, getParent());
                switch (slot){
                    case North:
                        setPreferredSize(new Dimension(10,ev.getY()));
                        break;
                    case South:
                        setPreferredSize(new Dimension(10, getParent().getHeight() - ev.getY()));
                        break;
                    case West:
                        setPreferredSize(new Dimension(ev.getX(),10));
                        break;
                    case East:
                        setPreferredSize(new Dimension(getParent().getWidth() - ev.getX(), 10));
                        break;
                }
                revalidate();
                repaint();
            }
        });
    }


    public void dock() {
        container.remove(DockPanel.this);
        setVisible(false);
    }

    public JToggleButton getButton() {
        return button;
    }

    public void setComponent(Component comp){
        super.add(comp, BorderLayout.CENTER);
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        this.component = comp;
    }

    @Override
    public Component add(Component comp) {
        setComponent(comp);
        return comp;
    }

}
