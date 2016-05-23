/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor;

import com.jme3.shader.Shader;
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
    //private final BackdropPanel backDrop = new BackdropPanel();
    private MatDefEditorController controller;

    public Diagram(MatDefEditorController controller) {
        this.controller = controller;
        setLayout(null);
        setBackground(new Color(40, 40, 40));
        DiagramMouseListener mouseListener = new DiagramMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
        createPopupMenu();
    }

//    public void refreshPreviews(Material mat, String technique) {
//        for (OutBusPanel outBusPanel : outBuses) {
//            outBusPanel.updatePreview(mat, technique);
//        }
//        if (backDrop.isVisible()) {
//            backDrop.showMaterial(mat, technique);
//        }
//    }

//    public void displayBackdrop() {
//        if (backDrop.getParent() == null) {
//            add(backDrop);
//            ((JViewport) getParent()).addChangeListener(backDrop);
//        }
//
//        backDrop.setVisible(true);
//        backDrop.update(((JViewport) getParent()));
//    }


//    public MatDefEditorWindow getEditorParent() {
//        return parent;
//    }


    public void showEdit(NodePanel node) {
        //  parent.showShaderEditor(node.getName(), node.getType(), node.filePaths);
    }



    //
//    public void addNodesFromDefs(List<ShaderNodeDefinition> defList, String path, Point clickPosition) {
//        int i = 0;
//        for (ShaderNodeDefinition def : defList) {
//            ShaderNodeBlock sn = new ShaderNodeBlock(def, path);
//            sn.setName(fixNodeName(sn.getName()));
//
//            NodePanel np = new NodePanel(sn, def);
//            addShaderNode(np);
//            np.setLocation(clickPosition.x + i * 150, clickPosition.y);
//            sn.setSpatialOrder(np.getLocation().x);
//            i++;
//            np.revalidate();
//            getEditorParent().notifyAddNode(sn, def);
//        }
//        repaint();
//    }
//
//    public void addMatParam(String type, String name, Point point) {
//        String fixedType = type;
//        if (type.equals("Color")) {
//            fixedType = "Vector4";
//        }
//        ShaderNodeVariable param = new ShaderNodeVariable(VarType.valueOf(fixedType).getGlslType(), name);
//        NodePanel np = new NodePanel(param, NodePanel.NodeType.MatParam);
//        addShaderNode(np);
//        np.setLocation(point.x, point.y);
//        np.revalidate();
//        repaint();
//        getEditorParent().notifyAddMapParam(type, name);
//    }
//
//    public void addWorldParam(UniformBinding binding, Point point) {
//
//        ShaderNodeVariable param = new ShaderNodeVariable(binding.getGlslType(), binding.name());
//        NodePanel np = new NodePanel(param, NodePanel.NodeType.WorldParam);
//        addShaderNode(np);
//        np.setLocation(point.x, point.y);
//        np.revalidate();
//        repaint();
//        getEditorParent().notifyAddWorldParam(binding.name());
//    }
//
//    public void addAttribute(String name, String type, Point point) {
//        ShaderNodeVariable param = new ShaderNodeVariable(type, "Attr", name);
//        NodePanel np = new NodePanel(param, NodePanel.NodeType.Attribute);
//        addShaderNode(np);
//        np.setLocation(point.x, point.y);
//        np.revalidate();
//        repaint();
//    }
//


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
//                AddNodeDialog d = new AddNodeDialog(null, true, parent.obj.getLookup().lookup(ProjectAssetManager.class), Diagram.this, contextMenuPosition);
//                d.setLocationRelativeTo(null);
//                d.setVisible(true);
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
//                AddWorldParameterDialog d = new AddWorldParameterDialog(null, true, Diagram.this, contextMenuPosition);
//                d.setLocationRelativeTo(null);
//                d.setVisible(true);
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
//                OutBusPanel p2 = new OutBusPanel("color" + (outBuses.size() - 1), Shader.ShaderType.Fragment);
//                p2.setBounds(0, 350 + 50 * (outBuses.size() - 1), p2.getWidth(), p2.getHeight());
//
//                addOutBus(p2);

            }
        });
    }

    private JSeparator createSeparator() {
        JSeparator jsep = new JSeparator(JSeparator.HORIZONTAL);
        jsep.setBackground(Color.BLACK);
        return jsep;
    }

    // TODO: 21/05/2016 Should be completely reworked as it sucks big time
    public void autoLayout() {

        int offset = 0;
        String keys = "";
        for (Component node : getComponents()) {

            if (node instanceof ShaderNodePanel) {
                node.setLocation(offset + 200, getNodeTop(node));
                //  getEditorParent().savePositionToMetaData(node.getKey(), node.getLocation().x, node.getLocation().y);
                int pad = getNodeTop(node);
                for (Component comp : getComponents()) {
                    if(comp instanceof Connection) {
                        Connection connection = (Connection) comp;
                        if (connection.getEnd().getNode() == node) {
                            if (connection.getStart().getNode() instanceof NodePanel) {
                                NodePanel startP = (NodePanel) connection.getStart().getNode();
                                if (!(startP instanceof ShaderNodePanel)) {
                                    startP.setLocation(offset + 30, pad);
                                    //       getEditorParent().savePositionToMetaData(startP.getKey(), startP.getLocation().x, startP.getLocation().y);
                                    keys += startP.getKey() + "|";
                                    pad += 50;
                                }
                            }
                        }
                        connection.revalidate();
                    }
                }
                offset += 320;
            }

        }
        for (Component node : getComponents()) {
            if (node instanceof InOutPanel) {
                InOutPanel out = (InOutPanel) node;
                Dot input = out.getInputConnectPoint();
                if (input.isConnected()) {
                    for (Dot dot : input.getConnectedDots()) {
                        if (dot.getNode() instanceof NodePanel) {
                            out.setLocation(dot.getNode().getX() + 180, dot.getNode().getY());
                            //       getEditorParent().savePositionToMetaData(startP.getKey(), startP.getLocation().x, startP.getLocation().y);
                            keys += out.getKey() + "|";
                        }
                    }
                }

            }
        }
        offset = 0;
        for (Component node : getComponents()) {
            if(node instanceof Selectable) {
                String key= ((Selectable)node).getKey();
                if (!(node instanceof ShaderNodePanel) && !(keys.contains(key))) {
                    node.setLocation(offset + 10, 0);
                    //    getEditorParent().savePositionToMetaData(node.getKey(), node.getLocation().x, node.getLocation().y);
                    offset += 130;
                }
            }
        }
    }

    private int getNodeTop(Component node) {
        if (node instanceof ShaderNodePanel) {
            ShaderNodePanel panel = (ShaderNodePanel) node;
            if (panel.getShaderType() == Shader.ShaderType.Vertex) {
                return 50;
            }
            if (panel.getShaderType() == Shader.ShaderType.Fragment) {
                return 300;
            }
        }
        return 0;

    }


    public void fitContent() {

        int maxWidth = getParent().getParent().getWidth() - 2;
        int maxHeight = getParent().getParent().getHeight() - 2;

        for (Component nodePanel : getComponents()) {
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
        private final Point pp = new Point();


        @Override
        public void mousePressed(MouseEvent e) {

            if (SwingUtilities.isLeftMouseButton(e)) {
                controller.findSelection(e, e.isShiftDown() || e.isControlDown());
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                setCursor(mvCursor);
                pp.setLocation(e.getPoint());
                ((JScrollPane) getParent().getParent()).setWheelScrollingEnabled(false);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {

            switch (e.getButton()) {
                case MouseEvent.BUTTON2:
                    setCursor(defCursor);
                    ((JScrollPane) getParent().getParent()).setWheelScrollingEnabled(true);
                    break;
                case MouseEvent.BUTTON3:
                    contextMenu.show(Diagram.this, e.getX(), e.getY());
                    contextMenuPosition.setLocation(e.getX(), e.getY());
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



    private class MyMenu extends JPopupMenu {

        public MyMenu(String label) {
            super(label);
        }

    }
}
