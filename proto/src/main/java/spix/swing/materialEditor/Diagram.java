/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor;

import com.jme3.shader.*;
import spix.swing.materialEditor.controller.MatDefEditorController;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.nodes.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import static spix.swing.materialEditor.icons.Icons.node;

/**
 * @author Nehon
 */
public class Diagram extends JPanel {

    private final static Cursor defCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private final static Cursor mvCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    private final Point contextMenuPosition = new Point(0, 0);

    private final MyMenu contextMenu = new MyMenu("Add");
    private MatDefEditorController controller;
    private final Point pp = new Point();

    public Diagram(MatDefEditorController controller) {
        this.controller = controller;
        setLayout(null);
        setBackground(new Color(40, 40, 40));
        DiagramMouseListener mouseListener = new DiagramMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
        createPopupMenu();
    }

    private JMenuItem createMenuItem(String text, Icon icon) {
        JMenuItem item = new JMenuItem(text, icon);
        item.setFont(new Font("Tahoma", 1, 10)); // NOI18N
        return item;
    }

    private void createPopupMenu() {
        contextMenu.setFont(new Font("Tahoma", 1, 10)); // NOI18N
        contextMenu.setOpaque(true);
        Border titleUnderline = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK);
        TitledBorder labelBorder = BorderFactory.createTitledBorder(
                titleUnderline, contextMenu.getLabel(),
                TitledBorder.LEADING, TitledBorder.ABOVE_TOP, contextMenu.getFont(), Color.BLACK);

        contextMenu.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        contextMenu.setBorder(BorderFactory.createCompoundBorder(contextMenu.getBorder(),
                labelBorder));

        JMenuItem nodeItem = createMenuItem("Node", node);
        nodeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.displayAddNodeDialog(contextMenuPosition);
            }
        });

        contextMenu.add(nodeItem);
        contextMenu.add(createSeparator());
        JMenuItem matParamItem = createMenuItem("Material Parameter", Icons.mat);
        matParamItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.displayAddMatParamDialog(contextMenuPosition);
            }
        });
        contextMenu.add(matParamItem);
        JMenuItem worldParamItem = createMenuItem("World Parameter", Icons.world);
        worldParamItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.displayAddWorldParamDialog(contextMenuPosition);
            }
        });
        contextMenu.add(worldParamItem);
        JMenuItem attributeItem = createMenuItem("Attribute", Icons.attrib);
        attributeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.displayAddAttibuteDialog(contextMenuPosition);
            }
        });
        contextMenu.add(attributeItem);
        contextMenu.add(createSeparator());
        JMenuItem outputItem = createMenuItem("Output color", Icons.output);
        contextMenu.add(outputItem);
        outputItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.displayAddFragmentOutputDialog(contextMenuPosition);
            }
        });

        JMenuItem outputVItem = createMenuItem("Output position", Icons.outputV);
        contextMenu.add(outputVItem);
        outputVItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.addOutPanel(Shader.ShaderType.Vertex,new ShaderNodeVariable("vec4", "Global", "position"), contextMenuPosition);
            }
        });
    }

    private JSeparator createSeparator() {
        JSeparator jsep = new JSeparator(JSeparator.HORIZONTAL);
        jsep.setBackground(Color.BLACK);
        return jsep;
    }

    public void fitContent() {

        int maxWidth = getParent().getParent().getWidth() - 2;
        int maxHeight = getParent().getParent().getHeight() - 2;

        for (Component nodePanel : getComponents()) {
            if(nodePanel instanceof GroupPane){
                continue;
            }
            int w = nodePanel.getLocation().x + nodePanel.getWidth() + 150;
            if (w > maxWidth) {
                maxWidth = w;
            }
            int h = nodePanel.getLocation().y + nodePanel.getHeight();
            if (h > maxHeight) {
                maxHeight = h;
            }
        }
        setPreferredSize(new Dimension(maxWidth, maxHeight));
        revalidate();
    }

    private class DiagramMouseListener extends MouseAdapter {


        @Override
        public void mousePressed(MouseEvent e) {

            if (SwingUtilities.isLeftMouseButton(e)) {
                controller.findSelection(e, e.isShiftDown() || e.isControlDown());
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                startWheelDrag(e);
            }
        }


        @Override
        public void mouseReleased(MouseEvent e) {

            switch (e.getButton()) {
                case MouseEvent.BUTTON2:
                    stopWheelDrag();
                    break;
                case MouseEvent.BUTTON3:
                    popContextMenu(e);
                    break;
            }

        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (SwingUtilities.isMiddleMouseButton(e)) {
                JViewport vport = (JViewport) getParent();
                Point cp = e.getPoint();
                Point vp = vport.getViewPosition();
                vp.translate(pp.x - cp.x, pp.y - cp.y);
                scrollRectToVisible(new Rectangle(vp, vport.getSize()));

            }
        }

    }


    public void startWheelDrag(MouseEvent e) {
        setCursor(mvCursor);
        pp.setLocation(e.getPoint());
        ((JScrollPane) getParent().getParent()).setWheelScrollingEnabled(false);
    }

    public void stopWheelDrag() {
        setCursor(defCursor);
        ((JScrollPane) getParent().getParent()).setWheelScrollingEnabled(true);
    }

    public void popContextMenu(MouseEvent e) {
        contextMenu.show(Diagram.this, e.getX(), e.getY());
        contextMenuPosition.setLocation(e.getX(), e.getY());
    }


    private class MyMenu extends JPopupMenu {

        public MyMenu(String label) {
            super(label);
        }

    }
}
