package spix.swing.sceneexplorer;

import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import spix.app.DefaultConstants;
import spix.app.light.LightWrapper;
import spix.core.SelectionModel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.panels.DockPanel;
import spix.undo.CompositeEdit;
import spix.undo.Edit;
import spix.undo.SceneGraphStructureEdit;
import spix.undo.UndoManager;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Enumeration;

import static spix.app.DefaultConstants.SCENE_ROOT;
import static spix.app.DefaultConstants.SELECTION_PROPERTY;

/**
 * Created by bouquet on 30/09/16.
 */
public class SceneExplorerPanel extends DockPanel {

    private JTree sceneTree;
    private SwingGui gui;

    public SceneExplorerPanel(Slot slot, Container container, SwingGui gui) {
        super(slot, container);

        this.gui = gui;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scene");

        sceneTree = new JTree(root);
        sceneTree.setShowsRootHandles(true);
        sceneTree.setRootVisible(false);
        sceneTree.setCellRenderer(new ItemRenderer());
        sceneTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        sceneTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        sceneTree.getLastSelectedPathComponent();

                if (node == null) {
                    return;
                }

                Object o = node.getUserObject();
                if (o instanceof Spatial || o instanceof Light) {

                    gui.runOnRender(new Runnable() {
                        @Override
                        public void run() {
                            SelectionModel sm = (SelectionModel) gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY);
                            if(sm.getSingleSelection() != o) {
                                sm.setSingleSelection(o);
                            }
                        }
                    });

                }
            }
        });


        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(sceneTree), BorderLayout.CENTER);
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
                refresh(gui.getSpix().getBlackboard().get(SCENE_ROOT, Spatial.class));
            }
        });
        tb.add(b);

        getButton().setIcon(Icons.shaderCode);
        setIcon(Icons.output);
        setTitle("Scene Explorer");

        //register blackboard bindings
        gui.getSpix().getBlackboard().bind(UndoManager.LAST_EDIT, this, "lastEdit");
        gui.getSpix().getBlackboard().bind(SCENE_ROOT, this, "sceneRoot");

        //Listening to selection to reflect the selection in the tree.
        SelectionModel selection = gui.getSpix().getBlackboard().get(SELECTION_PROPERTY, SelectionModel.class);
        selection.addPropertyChangeListener(new SelectionObserver());

    }

    public void setSceneRoot(Spatial root){
        refresh(root);
    }

    /**
     * bound to the blackboard
     * @param lastEdit
     */
    public void setLastEdit(Edit lastEdit){
        if (lastEdit instanceof CompositeEdit) {
            CompositeEdit edit = (CompositeEdit) lastEdit;
            if (edit.hasTypes(SceneGraphStructureEdit.class)) {
                refresh(gui.getSpix().getBlackboard().get(SCENE_ROOT, Spatial.class));
            }
        }

    }

    private void buildTree(Spatial s, DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(s);
        parent.add(item);


        if (s instanceof Geometry) {
            Geometry g = (Geometry) s;
            item.add(new DefaultMutableTreeNode(g.getMesh()));
            item.add(new DefaultMutableTreeNode(g.getMaterial()));
        }

        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial child : n.getChildren()) {
                buildTree(child, item);
            }

            if (n.getLocalLightList().size() != 0) {
                DefaultMutableTreeNode lights = new DefaultMutableTreeNode("Lights");
                item.add(lights);
                for (Light light : n.getLocalLightList()) {
                    DefaultMutableTreeNode l = new DefaultMutableTreeNode(light);
                    lights.add(l);
                }
            }
        }
        if (s.getNumControls() > 0) {
            DefaultMutableTreeNode controls = new DefaultMutableTreeNode("Controls");
            item.add(controls);
            for (int i = 0; i < s.getNumControls(); i++) {
                Control control = s.getControl(i);
                DefaultMutableTreeNode c = new DefaultMutableTreeNode(control);
                controls.add(c);
            }
        }
    }

    public void refresh(Spatial sceneRoot) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scene");

        buildTree(sceneRoot, root);
        gui.runOnSwing(new Runnable() {
            @Override
            public void run() {
                //TODO this is brute force...maybe it could be more clever and just update the tree instead of recreating one...
                java.util.List<String> expandedPaths = new ArrayList<>();
                for (int i = 0; i < sceneTree.getRowCount(); i++) {
                    if (sceneTree.isExpanded(i)) {
                        expandedPaths.add(sceneTree.getPathForRow(i).toString());
                    }
                }

                sceneTree.setModel(new DefaultTreeModel(root, true));

                //re expand expanded path
                for (int i = 0; i < sceneTree.getRowCount(); i++) {
                    if (expandedPaths.contains(sceneTree.getPathForRow(i).toString())) {
                        sceneTree.expandRow(i);
                    }
                }


                if (!sceneTree.isExpanded(0)) {
                    //always expand at least the first level
                    sceneTree.expandRow(0);
                }

            }
        });
    }

    private class ItemRenderer implements TreeCellRenderer {
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
            } else if (o instanceof Light) {
                Light l = (Light) o;
                label.setIcon(Icons.lightBulb);
                if (l.getName() == null) {
                    label.setText(l.getType().name());
                } else {
                    label.setText(l.getName());
                }
            } else if (o instanceof String) {
                String text = (String) o;
                label.setText((String) o);
                switch (text) {
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

            if (hasFocus || selected) {
                label.setBackground(Color.DARK_GRAY);
            } else {
                label.setBackground(null);
            }
            return label;
        }
    }

    private class SelectionObserver implements PropertyChangeListener {

        public void propertyChange( PropertyChangeEvent event ) {
            if(event.getNewValue() != event.getOldValue() ){
                gui.runOnSwing(new Runnable() {
                    @Override
                    public void run() {
                        Object o = event.getNewValue();
                        if (o instanceof Spatial || o instanceof LightWrapper) {
                            if (o instanceof LightWrapper) {
                                LightWrapper lw = (LightWrapper) o;
                                o = lw.getLight();
                            }
                            DefaultMutableTreeNode node = searchNode(o);
                            if(sceneTree.getLastSelectedPathComponent() != node) {
                                sceneTree.getSelectionModel().setSelectionPath(new TreePath(node.getPath()));
                            }
                        }
                    }
                });

            }

        }
    }

    public DefaultMutableTreeNode searchNode(Object userObject) {
        DefaultMutableTreeNode node = null;
        Enumeration e = ((DefaultMutableTreeNode)sceneTree.getModel().getRoot()).breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            node = (DefaultMutableTreeNode) e.nextElement();
            if (userObject.equals(node.getUserObject())) {
                return node;
            }
        }
        return null;
    }

}
