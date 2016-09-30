package spix.swing.sceneexplorer;

import com.jme3.light.Light;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import spix.core.SelectionModel;
import spix.core.Spix;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.panels.DockPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by bouquet on 30/09/16.
 */
public class SceneExplorerPanel extends DockPanel {

    private JTree sceneTree;
    private Spix spix;

    public SceneExplorerPanel(Slot slot, Container container, Spix spix) {
        super(slot, container);

        this.spix = spix;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scene");

        sceneTree = new JTree(root);
        sceneTree.setShowsRootHandles(true);
        sceneTree.setRootVisible(false);
        sceneTree.setCellRenderer(new ItemRenderer());


        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(sceneTree),BorderLayout.CENTER);
        setComponent(p);

        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setOrientation(JToolBar.VERTICAL);
        p.add(tb, BorderLayout.EAST);

        JButton b = new JButton();
        b.setIcon(Icons.reload);
        b.setToolTipText("Reload");
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        tb.add(b);

        getButton().setIcon(Icons.shaderCode);
        setIcon(Icons.output);
        setTitle("Scene Explorer");

    }

    private void buildTree(Spatial s, DefaultMutableTreeNode parent){
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(s);
        parent.add(item);
        if(s instanceof Node){
            Node n = (Node)s;
            for (Spatial child : n.getChildren()) {
                buildTree(child,item);
            }

            for (Light light : n.getLocalLightList()) {
                DefaultMutableTreeNode l = new DefaultMutableTreeNode(light);
                item.add(l);
            }
        }
    }

    public void refresh(){
        Node n = spix.getBlackboard().get("scene.root", Node.class);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scene");

        buildTree(n, root);
        sceneTree.setModel(new DefaultTreeModel(root,true));

        for (int i = 0; i < sceneTree.getRowCount(); i++) {
            sceneTree.expandRow(i);
        }

    }

//    DefaultTreeCellRenderer

    private class ItemRenderer implements TreeCellRenderer{
        private JLabel label;

        ItemRenderer() {
            label = new JLabel();
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            Object o = ((DefaultMutableTreeNode) value).getUserObject();
            if (o instanceof Node) {
                Node node = (Node) o;
                label.setIcon(Icons.jmeNode);
                label.setText(node.getName());
            } else if (o instanceof Geometry) {
                Geometry g = (Geometry) o;
                label.setIcon(Icons.model);
                label.setText(g.getName());
            } else if (o instanceof Light) {
                Light l = (Light) o;
                label.setIcon(Icons.lightBulb);
                if(l.getName() == null){
                    label.setText(l.getType().name());
                } else {
                    label.setText(l.getName());
                }
            }else if(o instanceof String) {
                label.setText((String)o);
            }
            if (hasFocus){
               label.setBackground(Color.DARK_GRAY);
            } else {
                label.setBackground(null);
            }
            return label;
        }
    }


}
