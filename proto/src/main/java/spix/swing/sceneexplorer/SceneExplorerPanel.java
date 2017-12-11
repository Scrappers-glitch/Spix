package spix.swing.sceneexplorer;

import com.jme3.animation.*;
import com.jme3.audio.AudioNode;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.scene.*;
import com.jme3.scene.control.Control;
import spix.app.DefaultConstants;
import spix.app.scene.SceneService;
import spix.core.SelectionModel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.panels.DockPanel;
import spix.undo.*;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
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
    private static String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
            ";class=\"" +
            Node.class.getName() +
            "\"";
    private static DataFlavor nodeFlavor;
    private boolean stopPropagation;
    private java.util.List removeFromSelection = new ArrayList();
    private java.util.List addToSelection = new ArrayList();
    private java.util.List selectedObjects = new ArrayList();

    public SceneExplorerPanel(Slot slot, Container container, SwingGui gui) {
        super(slot, container);

        try {
            nodeFlavor = new DataFlavor(mimeType);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        this.gui = gui;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scene");

        sceneTree = new JTree(root);
        sceneTree.setShowsRootHandles(true);
        sceneTree.setRootVisible(false);
        sceneTree.setCellRenderer(new ItemRenderer());
        sceneTree.setDragEnabled(true);
        sceneTree.setTransferHandler(new ExplorerTransferHandler());
        sceneTree.setDropMode(DropMode.ON);
        sceneTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        sceneTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if(stopPropagation){
                    return;
                }

                TreePath paths[] = sceneTree.getSelectionPaths();
                if (paths == null) {
                    return;
                }
                removeFromSelection.clear();
                addToSelection.clear();
                selectedObjects.clear();
                SelectionModel sm = (SelectionModel) gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY);

                for (TreePath path : paths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

                    if (node == null) {
                        return;
                    }

                    Object o = node.getUserObject();
                    selectedObjects.add(o);
                    if (o instanceof Bone) {
                        dumpBone((Bone) o);
                    }

                }
                for (Object o : selectedObjects) {
                    if (!sm.contains(o) && (o instanceof Spatial || o instanceof Light)) {
                        addToSelection.add(o);
                    }
                }

                for (Object o : sm) {
                    if (!selectedObjects.contains(o)) {
                        removeFromSelection.add(o);
                    }
                }


                gui.runOnRender(new Runnable() {
                    @Override
                    public void run() {
                        SelectionModel sm = (SelectionModel) gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY);
                        if (!removeFromSelection.isEmpty()) {
                            sm.removeAll(removeFromSelection);
                        }
                        if (!addToSelection.isEmpty()) {
                            sm.addAll(addToSelection);
                        }
                    }
                });

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

        getButton().setIcon(Icons.sceneExplorer);
        getButton().setRolloverIcon(Icons.sceneExplorerHover);
        getButton().setRolloverSelectedIcon(Icons.sceneExplorerHover);
        setIcon(Icons.sceneGraph);
        setTitle("Scene Explorer");

        //register blackboard bindings
        gui.getSpix().getBlackboard().bind(UndoManager.LAST_EDIT, this, "lastEdit");
        gui.getSpix().getBlackboard().bind(SCENE_ROOT, this, "sceneRoot");

        //Listening to selection to reflect the selection in the tree.
        SelectionModel selection = gui.getSpix().getBlackboard().get(SELECTION_PROPERTY, SelectionModel.class);
        selection.addPropertyChangeListener(new SelectionObserver());

    }

    public void dumpBone(Bone o) {
        Bone selectedBone = o;
        System.err.println("-----------------------");
        System.err.println("Selected Bone : " + selectedBone.getName() + " in skeleton ");
        System.err.println("Root Bone : " + (selectedBone.getParent() == null));
        System.err.println("-----------------------");
        System.err.println("Bind translation: " + selectedBone.getBindPosition());
        System.err.println("Bind rotation: " + selectedBone.getBindRotation());
        System.err.println("Bind scale: " + selectedBone.getBindScale());
        System.err.println("---");
        System.err.println("Local translation: " + selectedBone.getLocalPosition());
        System.err.println("Local rotation: " + selectedBone.getLocalRotation());
        System.err.println("Local scale: " + selectedBone.getLocalScale());
        System.err.println("---");
        System.err.println("Model translation: " + selectedBone.getModelSpacePosition());
        System.err.println("Model rotation: " + selectedBone.getModelSpaceRotation());
        System.err.println("Model scale: " + selectedBone.getModelSpaceScale());
        System.err.println("---");
        System.err.println("Bind inverse Transform: ");
        System.err.println(selectedBone.getBindInverseTransform());
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

    private void buildTree(Object s, DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(s);
        parent.add(item);
        if (s instanceof Bone) {
            Bone b = (Bone) s;
            for (Bone bone : b.getChildren()) {
                buildTree(bone, item);
            }
            return;
        }


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
        }

        if (!(s instanceof Spatial)) {
            return;
        }

        Spatial spat = (Spatial) s;

        if (spat.getLocalLightList().size() != 0) {
            DefaultMutableTreeNode lights = new DefaultMutableTreeNode("Lights");
            item.add(lights);
            for (Light light : spat.getLocalLightList()) {
                DefaultMutableTreeNode l = new DefaultMutableTreeNode(light);
                lights.add(l);
            }
        }
        if (spat.getNumControls() > 0) {
            DefaultMutableTreeNode controls = new DefaultMutableTreeNode("Controls");
            item.add(controls);
            for (int i = 0; i < spat.getNumControls(); i++) {
                Control control = spat.getControl(i);
                DefaultMutableTreeNode c = new DefaultMutableTreeNode(control);
                controls.add(c);
                if (control instanceof SkeletonControl) {
                    Skeleton sk = ((SkeletonControl) control).getSkeleton();
                    DefaultMutableTreeNode st = new DefaultMutableTreeNode(sk);
                    c.add(st);
                    for (Bone bone : sk.getRoots()) {
                        buildTree(bone, st);
                    }
                }
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

                updateSelection();

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

            if (o instanceof AudioNode) {
                AudioNode node = (AudioNode) o;
                label.setIcon(Icons.audio);
                label.setText(node.getName());
            } else if (o instanceof Node) {
                Node node = (Node) o;
                label.setIcon(Icons.jmeNode);
                label.setText(node.getName());
            } else if (o instanceof Geometry) {
                Geometry g = (Geometry) o;
                label.setIcon(Icons.model);
                label.setText(g.getName());
            } else if (o instanceof Material) {
                Material m = (Material) o;
                label.setIcon(Icons.material);
                label.setText("Material: " + m.getMaterialDef().getName());
            } else if (o instanceof Mesh) {
                Mesh m = (Mesh) o;
                label.setIcon(Icons.mesh);
                label.setText("Mesh: " + m.getClass().getSimpleName());
            } else if (o instanceof Skeleton) {
                Skeleton sk = (Skeleton) o;
                label.setIcon(Icons.vert);
                label.setText("Skeleton (" + sk.getBoneCount() + "bones)");
            } else if (o instanceof Bone) {
                Bone b = (Bone) o;
                label.setIcon(Icons.scale);
                label.setText(b.getName());
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
            JTree.DropLocation dropLocation = tree.getDropLocation();
            boolean isDrop = dropLocation != null
                    && dropLocation.getChildIndex() == -1
                    && tree.getRowForPath(dropLocation.getPath()) == row;

            label.setBackground(null);
            label.setForeground(Color.LIGHT_GRAY);

            if (selected) {
                label.setBackground(Color.DARK_GRAY);
                label.setForeground(Color.LIGHT_GRAY);
            }
            if (hasFocus) {
                label.setForeground(Color.WHITE);
            }

            if (isDrop) {
                label.setBackground(Color.LIGHT_GRAY);
                label.setForeground(Color.BLACK);
            }
            return label;
        }
    }

    private class SelectionObserver implements PropertyChangeListener {

        public void propertyChange( PropertyChangeEvent event ) {
            if (!(event.getPropertyName().equals("singleSelection"))) {
                return;
            }

            if(event.getNewValue() != event.getOldValue() ){
                gui.runOnSwing(new Runnable() {
                    @Override
                    public void run() {
                        updateSelection();
                    }
                });

            }

        }
    }

    private void updateSelection() {
        stopPropagation = true;
        SelectionModel selection = gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        sceneTree.getSelectionModel().clearSelection();
        for (Object o : selection) {
            DefaultMutableTreeNode node = searchNode(o);
            if (node == null) {
                continue;
            }
            if (sceneTree.getLastSelectedPathComponent() != node) {
                sceneTree.getSelectionModel().addSelectionPath(new TreePath(node.getPath()));
            }
        }
        stopPropagation = false;
    }

    public DefaultMutableTreeNode searchNode(Object userObject) {
        DefaultMutableTreeNode node = null;
        Enumeration e = ((DefaultMutableTreeNode)sceneTree.getModel().getRoot()).breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            node = (DefaultMutableTreeNode) e.nextElement();
            if( node == null || userObject == null){
                continue;
            }
            if (userObject.equals(node.getUserObject())) {
                return node;
            }
        }
        return null;
    }

    private class ExplorerTransferHandler extends TransferHandler{
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        private java.util.List<DefaultMutableTreeNode> nodes = new ArrayList<>();

        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree) c;
            TreePath paths[] = tree.getSelectionPaths();
            nodes.clear();
            for (TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object data = node.getUserObject();
                if (data != null
                        && (Spatial.class.isAssignableFrom(data.getClass())
                        || Light.class.isAssignableFrom(data.getClass())
                        || Control.class.isAssignableFrom(data.getClass()))
                        ) {

                    nodes.add(node);
                }

            }

            if (!nodes.isEmpty()) {
                return new NodesTransferable(nodes);
            }

            return null;
        }

        public boolean canImport(TransferHandler.TransferSupport info) {
            // for the demo, we'll only support drops (not clipboard paste)
            if (!info.isDrop()) {
                return false;
            }
            info.setShowDropLocation(true);

            // fetch the drop location
            JTree.DropLocation dl = (JTree.DropLocation)info.getDropLocation();

            //only allow drop on Nodes
            TreePath path = dl.getPath();
            if (path == null) {
                return false;
            }
            DefaultMutableTreeNode target = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object data = target.getUserObject();
            if (data instanceof Node) {
                return true;
            }

            java.util.List<DefaultMutableTreeNode> draggedData = getData(info);
            if (draggedData == null || draggedData.isEmpty()) {
                return false;
            }

            for (DefaultMutableTreeNode defaultMutableTreeNode : draggedData) {
                Object o = defaultMutableTreeNode.getUserObject();
                if (Control.class.isAssignableFrom(o.getClass())
                        || Light.class.isAssignableFrom(o.getClass())) {
                    if (Spatial.class.isAssignableFrom(data.getClass())) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean importData(TransferHandler.TransferSupport info) {
            // if we can't handle the import, say so
            if (!canImport(info)) {
                return false;
            }

            // fetch the drop location
            JTree.DropLocation dl = (JTree.DropLocation)info.getDropLocation();

            // fetch the path and child index from the drop location
            TreePath path = dl.getPath();
            DefaultMutableTreeNode target = (DefaultMutableTreeNode)path.getLastPathComponent();
            Spatial targetSpatial = (Spatial)target.getUserObject();
            Node targetNode = null;
            if(targetSpatial instanceof Node){
                targetNode = (Node)targetSpatial;
            }

            // fetch the data and bail if this fails
            java.util.List<DefaultMutableTreeNode> dataList = getData(info);
            if (dataList == null || dataList.isEmpty()) {
                return false;
            }

            for (DefaultMutableTreeNode node : dataList) {
                Object data = node.getUserObject();
                if (Spatial.class.isAssignableFrom(data.getClass())) {
                    if (targetNode == null) {
                        continue;
                    }
                    //move spatial to node
                    gui.getService(SceneService.class).moveSpatial((Spatial) data, targetNode);
                    continue;
                }

                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) sceneTree.getSelectionPath().getParentPath().getParentPath().getLastPathComponent();
                Spatial parentSpatial = (Spatial) parent.getUserObject();

                if (Light.class.isAssignableFrom(data.getClass())) {
                    //move light to node
                    gui.getService(SceneService.class).moveLight((Light) data, targetSpatial, parentSpatial);
                } else if (Control.class.isAssignableFrom(data.getClass())) {
                    //move control to node
                    gui.getService(SceneService.class).moveControl((Control) data, targetSpatial, parentSpatial);
                }
            }



            return true;
        }


    }

    private java.util.List<DefaultMutableTreeNode> getData(TransferHandler.TransferSupport info) {
        try {
            java.util.List<DefaultMutableTreeNode> treeNodes = (java.util.List<DefaultMutableTreeNode>) info.getTransferable().getTransferData(nodeFlavor);
            return treeNodes;
        } catch (UnsupportedFlavorException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public class NodesTransferable implements Transferable {
        java.util.List<DefaultMutableTreeNode> nodes = new ArrayList<>();
        DataFlavor[] flavors= new DataFlavor[1];

        public NodesTransferable(java.util.List<DefaultMutableTreeNode> nodes) {
            try {
                String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
                        ";class=\"" +
                        nodes.get(0).getUserObject().getClass().getName() +
                        "\"";
                flavors[0] = new DataFlavor(mimeType);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            this.nodes.addAll(nodes);
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            return nodes;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return true;
        }
    }

}