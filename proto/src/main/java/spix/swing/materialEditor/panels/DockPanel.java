package spix.swing.materialEditor.panels;

import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created by Nehon on 03/06/2016.
 */
public class DockPanel extends JPanel {

    public static final int MARGIN = 5;

    public enum Slot {
        North,
        East,
        West,
        South
    }

    private String layoutSlot;
    protected JToggleButton button;
    private Container container;
    private Component component;
    private JLabel header;

    public DockPanel(Slot slot, Container container) {
        super(new BorderLayout());

        if (!(container.getLayout() instanceof BorderLayout)) {
            throw new IllegalArgumentException("Container must have a BorderLayout");
        }
        this.container = container;


        JPanel p = new JPanel(new GridLayout(1,2,0,0));

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT,2,2));
        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT,2,2));
        p.add(p1);
        p.add(p2);
        header = new JLabel();
        p1.add(header);
        add(p, BorderLayout.NORTH);
        p.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
            }
        });
        DockButton close = new DockButton();
        p2.add(close);


        setVisible(false);
        switch (slot) {
            case North:
                setBorder(BorderFactory.createEmptyBorder(0, 0, MARGIN, 0));
                setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                layoutSlot = BorderLayout.NORTH;
                break;
            case South:
                setBorder(BorderFactory.createEmptyBorder(MARGIN, 0, 0, 0));
                setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                layoutSlot = BorderLayout.SOUTH;
                close.setIcon(Icons.dockDown);
                close.setRolloverIcon(Icons.dockDownHover);
                break;
            case West:
                setBorder(BorderFactory.createEmptyBorder(0, 0, 0, MARGIN));
                setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                layoutSlot = BorderLayout.WEST;
                close.setIcon(Icons.dockLeft);
                close.setRolloverIcon(Icons.dockLeftHover);
                break;
            case East:
                setBorder(BorderFactory.createEmptyBorder(0, MARGIN, 0, 0));
                setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                layoutSlot = BorderLayout.EAST;
                close.setIcon(Icons.dockRight);
                close.setRolloverIcon(Icons.dockRightHover);
                break;
        }

        button = new JToggleButton();
        button.setPreferredSize(new Dimension(20, 20));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isVisible()) {
                    dock();
                } else {
                    unDock();
                }

            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                MouseEvent ev = SwingUtilities.convertMouseEvent(DockPanel.this, e, getParent());
                switch (slot) {
                    case North:
                        setPreferredSize(new Dimension(10, ev.getY()));
                        break;
                    case South:
                        setPreferredSize(new Dimension(10, getParent().getHeight() - ev.getY()));
                        break;
                    case West:
                        setPreferredSize(new Dimension(ev.getX(), 10));
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

    public void setTitle(String title) {
        header.setText(title);
    }

    public void setIcon(Icon icon) {
        header.setIcon(icon);
    }

    public void dock() {
        button.setSelected(false);
        container.remove(DockPanel.this);
        setVisible(false);
        container.revalidate();
        container.repaint();
    }

    public void unDock() {
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
        container.revalidate();
        container.repaint();

    }

    public JToggleButton getButton() {
        return button;
    }

    public void setComponent(Component comp) {
        super.add(comp, BorderLayout.CENTER);
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        this.component = comp;
    }

    @Override
    public Component add(Component comp) {
        setComponent(comp);
        return comp;
    }

    class DockButton extends JLabel {

        private Icon rollOverIcon;
        private Icon savedIcon;

        public void setRolloverIcon(Icon rollOverIcon) {
            this.rollOverIcon = rollOverIcon;
        }

        public DockButton() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    dock();
                    container.revalidate();
                    container.repaint();
                    setIcon(savedIcon);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (rollOverIcon != null) {
                        savedIcon = getIcon();
                        setIcon(rollOverIcon);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (savedIcon != null) {
                        setIcon(savedIcon);
                        savedIcon = null;
                    }
                }
            });
        }


    }

}
