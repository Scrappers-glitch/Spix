package spix.swing.materialEditor.nodes;

import spix.swing.materialEditor.Diagram;
import spix.swing.materialEditor.controller.MatDefEditorController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GroupPane extends JPanel {

    private Diagram diagram;
    private JLabel label;

    public GroupPane(MatDefEditorController controller, Diagram diagram) {
        super();
        this.diagram = diagram;
        setOpaque(false);
        setFocusable(true);

        setLayout(new BorderLayout());
        updateSize(diagram.getWidth(), diagram.getHeight());
        setBackground(new Color(100, 100, 120, 250));
        label = new JLabel();
        add(label, BorderLayout.NORTH);
        diagram.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSize(diagram.getWidth(), diagram.getHeight());
            }
        });
    }

    public void setGroupName(String name) {
        label.setText(name);
    }

    public void updateSize(int width, int height) {
        setMinimumSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        setBounds(0, 0, width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.85f));
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

    }

}
