package spix.swing.sceneexplorer;

import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.scene.*;
import com.jme3.scene.control.Control;
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

        if(s instanceof Geometry){
            Geometry g = (Geometry)s;
            item.add( new DefaultMutableTreeNode(g.getMesh()));
            item.add( new DefaultMutableTreeNode(g.getMaterial()));
        }

        if(s instanceof Node){
            Node n = (Node)s;
            for (Spatial child : n.getChildren()) {
                buildTree(child,item);
            }

            if(n.getLocalLightList().size() != 0) {
                DefaultMutableTreeNode lights = new DefaultMutableTreeNode("Lights");
                item.add(lights);
                for (Light light : n.getLocalLightList()) {
                    DefaultMutableTreeNode l = new DefaultMutableTreeNode(light);
                    lights.add(l);
                }
            }
        }
        if(s.getNumControls() > 0){
            DefaultMutableTreeNode controls = new DefaultMutableTreeNode("Controls");
            item.add(controls);
            for (int i = 0; i < s.getNumControls(); i++) {
                Control control = s.getControl(i);
                DefaultMutableTreeNode c = new DefaultMutableTreeNode(control);
                controls.add(c);
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
            label.setIcon(Icons.errorGray);
            label.setText(o.toString());

            if (o instanceof Node) {
                Node node = (Node) o;
                label.setIcon(Icons.jmeNode);
                label.setText(node.getName());
            } else if (o instanceof Geometry) {
                Geometry g = (Geometry) o;
                label.setIcon(Icons.model);
                label.setText(g.getName());
            } else if (o instanceof Material) {
                Material m = (Material) o;
                label.setIcon(Icons.mat);
                label.setText("Material: " + m.getMaterialDef().getName());
            } else if (o instanceof Mesh) {
                Mesh m = (Mesh) o;
                label.setIcon(Icons.mesh);
                label.setText("Mesh: " + m.getClass().getSimpleName());
            } else if (o instanceof Control) {
                Control c = (Control) o;
                label.setIcon(Icons.tech);
                label.setText(c.getClass().getSimpleName());
            }else if (o instanceof Light) {
                Light l = (Light) o;
                label.setIcon(Icons.lightBulb);
                if(l.getName() == null){
                    label.setText(l.getType().name());
                } else {
                    label.setText(l.getName());
                }
            }else if(o instanceof String) {
                String text = (String)o;
                label.setText((String)o);
                switch (text){
                    case "Lights":
                        label.setIcon(Icons.lightBulb);
                        break;
                    case "Controls":
                        label.setIcon(Icons.tech);
                        break;
                    default:
                        label.setIcon(Icons.errorGray);
                }
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
