package spix.swing.sceneexplorer;

import com.jme3.audio.AudioNode;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.scene.*;
import com.jme3.scene.control.Control;
import spix.app.DefaultConstants;
import spix.app.light.LightWrapper;
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
import java.awt.List;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.*;

import static spix.app.DefaultConstants.SCENE_ROOT;
import static spix.app.DefaultConstants.SELECTION_PROPERTY;

/**
 * Created by bouquet on 30/09/16.
 */
public class SceneExplorerPanel extends DockPanel {

    private JTree sceneTree;
    private SwingGui gui;
    private Object lastSelected;
    private static String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
            ";class=\"" +
            Node.class.getName() +
            "\"";
    private static DataFlavor nodeFlavor;
    private boolean stopPropagation;

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
        sceneTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        sceneTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if(stopPropagation){
                    return;
                }
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
        }
        if (s.getLocalLightList().size() != 0) {
            DefaultMutableTreeNode lights = new DefaultMutableTreeNode("Lights");
            item.add(lights);
            for (Light light : s.getLocalLightList()) {
                DefaultMutableTreeNode l = new DefaultMutableTreeNode(light);
                lights.add(l);
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

                if (lastSelected != null) {
                    updateSelection(lastSelected);
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
            if (hasFocus || selected || isDrop) {
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
                            updateSelection(o);
                        }
                    }
                });

            }

        }
    }



    private void updateSelection(Object o) {
        stopPropagation = true;
        DefaultMutableTreeNode node = searchNode(o);
        if (node == null) {
            lastSelected = o;
            return;
        }
        if (sceneTree.getLastSelectedPathComponent() != node) {
            sceneTree.getSelectionModel().setSelectionPath(new TreePath(node.getPath()));
        }
        lastSelected = null;
        stopPropagation = false;
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

    private class ExplorerTransferHandler extends TransferHandler{
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree) c;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();

            Object data  = node.getUserObject();
            if(data!= null
                && (Spatial.class.isAssignableFrom(data.getClass())
                    ||Light.class .isAssignableFrom(data.getClass())
                    || Control.class.isAssignableFrom(data.getClass()))
                ){

                return new NodesTransferable(node);
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
            DefaultMutableTreeNode target = (DefaultMutableTreeNode)path.getLastPathComponent();
            Object data = target.getUserObject();

            Object draggedData = getData(info);
            if(draggedData == null){
                return false;
            }

            if (data instanceof Node) {
                return true;
            }

            if(Control.class.isAssignableFrom(draggedData.getClass())
                    ||Light.class.isAssignableFrom(draggedData.getClass())){
                if(Spatial.class.isAssignableFrom(data.getClass())){
                    return true;
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
            Object data = getData(info);
            if(data == null){
                return false;
            }

            if(Spatial.class.isAssignableFrom(data.getClass())){
                if(targetNode == null){
                    return false;
                }
                //move spatial to node
                gui.getService(SceneService.class).moveSpatial((Spatial)data, targetNode);
                return true;
            }

            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)sceneTree.getSelectionPath().getParentPath().getParentPath().getLastPathComponent();
            Spatial parentSpatial = (Spatial)parent.getUserObject();

            if(Light.class.isAssignableFrom(data.getClass())){
                //move light to node
                gui.getService(SceneService.class).moveLight((Light)data, targetSpatial, parentSpatial);
            } else if(Control.class.isAssignableFrom(data.getClass())){
                //move control to node
                gui.getService(SceneService.class).moveControl((Control)data, targetSpatial, parentSpatial);
            }

            return true;
        }


    }

    private Object getData(TransferHandler.TransferSupport info){
        try {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)info.getTransferable().getTransferData(nodeFlavor);
            return treeNode.getUserObject() ;
        } catch (UnsupportedFlavorException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public class NodesTransferable implements Transferable {
        DefaultMutableTreeNode node;
        DataFlavor[] flavors= new DataFlavor[1];

        public NodesTransferable(DefaultMutableTreeNode node) {
            try {
                String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
                        ";class=\"" +
                        node.getUserObject().getClass().getName() +
                        "\"";
                flavors[0] = new DataFlavor(mimeType);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            this.node = node;
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            return node;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return true;
        }
    }

}