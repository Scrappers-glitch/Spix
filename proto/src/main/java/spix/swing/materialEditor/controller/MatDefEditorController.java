package spix.swing.materialEditor.controller;

import com.jme3.asset.AssetManager;
import com.jme3.asset.ShaderNodeDefinitionKey;
import com.jme3.material.*;
import com.jme3.material.plugins.*;
import com.jme3.scene.Geometry;
import com.jme3.shader.*;
import com.jme3.util.blockparser.BlockLanguageParser;
import com.jme3.util.blockparser.Statement;
import groovy.util.ObservableList;
import spix.app.DefaultConstants;
import spix.app.FileIoService;
import spix.app.material.MaterialService;
import spix.app.material.hack.*;
import spix.app.metadata.MetadataService;
import spix.app.utils.CloneUtils;
import spix.core.*;
import spix.props.PropertySet;
import spix.swing.PropertyEditorPanel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.*;
import spix.swing.materialEditor.dialog.*;
import spix.swing.materialEditor.nodes.*;
import spix.swing.materialEditor.panels.*;
import spix.swing.materialEditor.sort.Node;
import spix.swing.materialEditor.utils.*;
import spix.ui.MessageRequester;
import spix.undo.UndoManager;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Created by Nehon on 18/05/2016.
 */
public class MatDefEditorController {

    public static final String MAT_DEF_EDITOR_SELECTED_ITEM = "matDefEditor.selection.item.singleSelect";
    public static final String MAT_DEF_EDITOR_SELECTED_MATDEF = "matDefEditor.selection.matdef.singleSelect";
    public static final String MAT_DEF_EDITOR_SELECTED_TECHNIQUE = "matDefEditor.selection.technique.singleSelect";
    private MaterialDef matDef;
    private MatDefEditorWindow editor;
    private SwingGui gui;
    private DragHandler dragHandler = new DragHandler();
    private SelectionModel sceneSelection;
    private SceneSelectionChangeListener sceneSelectionChangeListener = new SceneSelectionChangeListener();
    //private DiagramSelectionChangeListener diagramSelectionChangeListener = new DiagramSelectionChangeListener();
    private List<TechniqueDef> techniques = new ArrayList<>();
    private DiagramUiHandler diagramUiHandler;
    private SelectionHandler selectionHandler = new SelectionHandler();
    private DataHandler dataHandler = new DataHandler();
    private ErrorLog errorLog;
    private ShaderCodePanel shaderCodePanel;
    private ShaderNodeCodePanel shaderNodeCodePanel;
    private PropPanel propertiesPanel;
    private Deque<Node> sortedNodes;
    private TitledBorder selectionBorder;
    private J3snValidator j3snValidator = new J3snValidator();

    private Map<String, MatParam> matParams = new HashMap<>();


    public MatDefEditorController(SwingGui gui, MatDefEditorWindow editor) {
        this.gui = gui;
        this.editor = editor;

        setupSpixListener(gui);

        sceneSelection = gui.getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        sceneSelection.addPropertyChangeListener(sceneSelectionChangeListener);

        Diagram diagram = initUi(gui, editor);
        diagramUiHandler = new DiagramUiHandler(diagram);


    }

    private void createToolbar(final SwingGui gui, MatDefEditorWindow editor) {
        //todo fix this
        JToolBar tb = new JToolBar();

        Action save = new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateAndSave(gui);
            }
        };
        save.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control S"));
        // create a button, configured with the Action
        JButton b = new JButton(save);
        // manually register the accelerator in the button's component input map
        b.getActionMap().put("save", save);
        b.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                (KeyStroke) save.getValue(Action.ACCELERATOR_KEY), "save");

        tb.add(b);
        editor.getContentPane().add(tb, BorderLayout.NORTH);
    }

    public void validateAndSave(SwingGui gui) {
        Collection<ShaderNodeCodePanel.Document> documents = shaderNodeCodePanel.getDocuments();
        boolean needsReload = false;
        for (ShaderNodeCodePanel.Document document : documents) {
            if (document.isModified()) {
                j3snValidator.reset();
                if (document.getName().endsWith(".j3sn")) {
                    //definition we have to validate it and reload it if necessary.
                    if (!j3snValidator.isValid(document, dataHandler, diagramUiHandler, this)) {
                        return;
                    }
                    needsReload = true;
                }
                gui.getSpix().getService(FileIoService.class).saveFile(document.getName(), document.getContent());
                shaderNodeCodePanel.refreshErrors();
                document.setModified(false);
            }
        }
        gui.getSpix().getService(FileIoService.class).saveMaterialDef(matDef);
        gui.getSpix().getService(MetadataService.class).setMetadata(diagramUiHandler.getMatDefMetadata(), matDef);
        if (!j3snValidator.getToCreate().isEmpty()) {
            for (String path : j3snValidator.getToCreate()) {
                gui.getSpix().getService(FileIoService.class).createShaderFile(path);
            }
        }
        if (!j3snValidator.getToRename().isEmpty()) {
            for (String oldPath : j3snValidator.getToRename().keySet()) {
                gui.getSpix().getService(FileIoService.class).renameFile(oldPath, j3snValidator.getToRename().get(oldPath));
            }
        }
        errorLog.noError();
        refreshPreviews();

        if (needsReload) {
            Object selection = gui.getSpix().getBlackboard().get(MAT_DEF_EDITOR_SELECTED_ITEM);
            shaderNodeCodePanel.refreshForSelection(selection);
        }
    }

    public ShaderNodeCodePanel getShaderNodeCodePanel() {
        return shaderNodeCodePanel;
    }

    public ErrorLog getErrorLog() {
        return errorLog;
    }

    private void setupSpixListener(SwingGui gui) {
        gui.getSpix().addSpixListener(new SpixListener() {

            @Override
            public PropertySet propertySetCreated(Object wrapped, PropertySet newSet) {

                if (wrapped instanceof ShaderNode) {
                    newSet.getProperty("name").addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            diagramUiHandler.renameShaderNode((String) evt.getOldValue(), (String) evt.getNewValue());
                            dataHandler.renameShaderNode((String) evt.getOldValue(), (String) evt.getNewValue());
                        }
                    });
                }
                return newSet;
            }
        });
    }

    public Diagram initUi(SwingGui gui, MatDefEditorWindow editor) {
        Container mainContainer = editor.getContentPane();
        mainContainer.setLayout(new BorderLayout());
        JPanel centerPane = new JPanel();
        mainContainer.add(centerPane, BorderLayout.CENTER);

        centerPane.setLayout(new BorderLayout());

        Diagram diagram = new Diagram(this);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(diagram);
        centerPane.add(scrollPane, BorderLayout.CENTER);
        scrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                diagram.fitContent();
            }
        });

        errorLog = new ErrorLog(centerPane);
        errorLog.setPreferredSize(new Dimension(100, 500));
        centerPane.add(errorLog, BorderLayout.SOUTH);

        shaderCodePanel = new ShaderCodePanel(centerPane, gui);
        shaderCodePanel.setPreferredSize(new Dimension(500, 10));

        shaderNodeCodePanel = new ShaderNodeCodePanel(centerPane, gui);
        shaderNodeCodePanel.setPreferredSize(new Dimension(500, 10));

        JToolBar westToolBar = new JToolBar(JToolBar.VERTICAL);
        westToolBar.setFloatable(false);
        editor.getContentPane().add(westToolBar, BorderLayout.WEST);
        NoneSelectedButtonGroup groupW = new NoneSelectedButtonGroup();
        groupW.add(shaderCodePanel.getButton());
        westToolBar.add(shaderCodePanel.getButton());
        groupW.add(shaderNodeCodePanel.getButton());
        westToolBar.add(shaderNodeCodePanel.getButton());

        JToolBar southToolBar = new JToolBar();
        southToolBar.setFloatable(false);
        editor.getContentPane().add(southToolBar, BorderLayout.SOUTH);
        southToolBar.addSeparator(new Dimension(30, 10));
        southToolBar.add(errorLog.getButton());

        propertiesPanel = new PropPanel(centerPane);
        propertiesPanel.setPreferredSize(new Dimension(250, 10));

        JToolBar eastToolBar = new JToolBar(JToolBar.VERTICAL);
        eastToolBar.setFloatable(false);
        editor.getContentPane().add(eastToolBar, BorderLayout.EAST);
        NoneSelectedButtonGroup groupE = new NoneSelectedButtonGroup();
        groupE.add(propertiesPanel.getButton());
        eastToolBar.add(propertiesPanel.getButton());

        createToolbar(gui, editor);

        PropertyEditorPanel matDefProps = new PropertyEditorPanel(gui, "ui.matdef.editor");
        JPanel p = new JPanel(new GridLayout(1, 1));
        p.setBorder(new TitledBorder("Material definition"));
        p.add(matDefProps);

        PropertyEditorPanel techDefProps = new PropertyEditorPanel(gui, "ui.matdef.editor");
        JPanel p2 = new JPanel(new GridLayout(1, 1));
        p2.setBorder(new TitledBorder("Technique definition"));
        p2.add(techDefProps);

        PropertyEditorPanel shaderNodeProp = new PropertyEditorPanel(gui, "ui.matdef.editor");
        JPanel p3 = new JPanel(new GridLayout(1, 1));
        selectionBorder = new TitledBorder("Selection");
        p3.setBorder(selectionBorder);
        p3.add(shaderNodeProp);

        JPanel props = new JPanel();
        SpringLayout sl = new SpringLayout();

        sl.putConstraint(SpringLayout.NORTH, p, 3, SpringLayout.NORTH, props);
        sl.putConstraint(SpringLayout.NORTH, p2, 3, SpringLayout.SOUTH, p);
        sl.putConstraint(SpringLayout.NORTH, p3, 3, SpringLayout.SOUTH, p2);
        sl.putConstraint(SpringLayout.WEST, p, 3, SpringLayout.WEST, props);
        sl.putConstraint(SpringLayout.EAST, p, 3, SpringLayout.EAST, props);
        sl.putConstraint(SpringLayout.WEST, p2, 3, SpringLayout.WEST, props);
        sl.putConstraint(SpringLayout.EAST, p2, 3, SpringLayout.EAST, props);
        sl.putConstraint(SpringLayout.WEST, p3, 3, SpringLayout.WEST, props);
        sl.putConstraint(SpringLayout.EAST, p3, 3, SpringLayout.EAST, props);


        props.setLayout(sl);
        props.add(p);
        props.add(p2);
        props.add(p3);

        propertiesPanel.setComponent(new JScrollPane(props));

        // Bind the selection to the editor panel, converting objects to
        // property set wrappers if appropriate.
        gui.getSpix().getBlackboard().bind(MAT_DEF_EDITOR_SELECTED_MATDEF,
                matDefProps, "object",
                new ToPropertySetFunction(gui.getSpix()));
        gui.getSpix().getBlackboard().bind(MAT_DEF_EDITOR_SELECTED_TECHNIQUE,
                techDefProps, "object",
                new ToPropertySetFunction(gui.getSpix()));
        gui.getSpix().getBlackboard().bind(MAT_DEF_EDITOR_SELECTED_ITEM,
                shaderNodeProp, "object",
                new ToPropertySetFunction(gui.getSpix()));

        gui.getSpix().getBlackboard().bind(MAT_DEF_EDITOR_SELECTED_ITEM, shaderNodeCodePanel, "selectedNode");

        return diagram;
    }

    public void cleanup() {
        sceneSelection.removePropertyChangeListener(sceneSelectionChangeListener);
        matDef = null;
        techniques.clear();
    }

    public MatDefEditorWindow getEditor() {
        return editor;
    }

    public DragHandler getDragHandler() {
        return dragHandler;
    }

    void initTechnique(TechniqueDef technique, MaterialDef matDef, Map<String, Object> matDefMetadata) {
        if (!technique.isUsingShaderNodes()) {
            return;
        }
        diagramUiHandler.setCurrentTechniqueName(technique.getName());
        diagramUiHandler.setMatDefMetadata(matDefMetadata);
        dataHandler.setCurrentTechnique(technique);
        dataHandler.setCurrentMatDef(matDef);

        if (technique.isUsingShaderNodes()) {
            MaterialDefUtils.computeShaderNodeGenerationInfo(technique, matDef);
            List<ShaderNodeVariable> uniforms = new ArrayList<>();
            MaterialDefUtils.getAllUniforms(technique, matDef, uniforms);

            for (ShaderNode sn : technique.getShaderNodes()) {
                diagramUiHandler.addShaderNodePanel(this, sn);
                dataHandler.registerShaderNode(sn);
            }

            for (ShaderNodeVariable shaderNodeVariable : technique.getShaderGenerationInfo().getAttributes()) {
                diagramUiHandler.addInputPanel(this, shaderNodeVariable);
            }

            for (ShaderNodeVariable shaderNodeVariable : uniforms) {
                diagramUiHandler.addInputPanel(this, shaderNodeVariable);
            }

            for (ShaderNode sn : technique.getShaderNodes()) {
                List<VariableMapping> ins = sn.getInputMapping();
                if (ins != null) {
                    for (VariableMapping mapping : ins) {
                        diagramUiHandler.makeConnection(this, mapping, technique, sn.getName());
                        dataHandler.registerMapping(mapping);
                    }
                }
                List<VariableMapping> outs = sn.getOutputMapping();
                if (outs != null) {
                    for (VariableMapping mapping : outs) {
                        diagramUiHandler.makeConnection(this, mapping, technique, sn.getName());
                        dataHandler.registerMapping(mapping);
                    }
                }
            }
        }

        //this will change when we have meta data
        refreshPreviews();
        diagramUiHandler.autoLayout(sortedNodes);
    }

    public void onSelectionPropertyChange(Geometry selection) {
        populateMatParams(selection);
        refreshPreviews();
    }

    private void populateMatParams(Geometry g) {
        Material mat = g.getMaterial();
        if (mat.getMaterialDef().getAssetName() != matDef.getAssetName()) {
            return;
        }
        matParams.clear();
        for (MatParam matParam : mat.getParams()) {
            matParams.put(matParam.getName(), matParam);
        }
    }

    public void refreshPreviews() {
        if (dataHandler.getCurrentTechnique().isUsingShaderNodes()) {
            sortedNodes = dataHandler.sortNodes(diagramUiHandler.getNodesForSort());
            diagramUiHandler.refreshPreviews(gui, errorLog, matDef, sortedNodes, matParams);
        }

        shaderCodePanel.refreshCode(dataHandler.getCurrentTechnique(), matDef);
    }

    public Connection connect(Dot start, Dot end) {
        Connection conn = diagramUiHandler.connect(this, start, end);
        dataHandler.addMapping(MaterialDefUtils.createVariableMapping(start, end));
        refreshPreviews();
        return conn;
    }

    void removeConnectionNoRefresh(Connection conn) {

        diagramUiHandler.removeConnection(conn);
        removeMappingForConnection(conn);
    }

    void removeMappingForConnection(Connection conn) {
        dataHandler.removeMappingForKey(conn.getKey());
    }

    public void removeConnection(Connection conn) {
        removeConnectionNoRefresh(conn);
        refreshPreviews();
    }

    public void removeNode(String key) {

        /*the order in those calls is very important.
            1. the node deletion in the ui
                we also iterate over the connections and delete them from the UI AND from the data model
            2. the node deletion in the data model.

            This allow to avoid iterating in all the shader in the data model to find inputs of other nodes that were
            coming from this node
        */
        diagramUiHandler.removeNode(this, key);

        if (key.contains("WorldParam")) {
            dataHandler.removeWorldParam(key);
        } else if (key.contains("MatParam")) {
            dataHandler.removeMatParam(key);
        } else {
            dataHandler.removeShaderNodeForKey(key);
        }

        refreshPreviews();
    }

    public NodePanel addShaderNode(ShaderNode sn) {
        dataHandler.addShaderNode(sn);
        NodePanel node = diagramUiHandler.addShaderNodePanel(this, sn);
        return node;
    }

    public NodePanel addOutPanel(Shader.ShaderType type, ShaderNodeVariable var) {
        NodePanel node = diagramUiHandler.makeOutPanel(this, type, var);
        return node;
    }

    public NodePanel addOutPanel(Shader.ShaderType type, ShaderNodeVariable var, Point point) {
        NodePanel node = diagramUiHandler.makeOutPanel(this, type, var);
        node.setLocation(point);
        diagramUiHandler.refreshDiagram();
        return node;
    }

    public void displayAddFragmentOutputDialog(Point clickPosition) {
        AddFragmentOutputDialog d = new AddFragmentOutputDialog(editor, true, this, clickPosition);
        d.setVisible(true);
    }

    public void displayAddAttibuteDialog(Point clickPosition) {
        AddAttributeDialog d = new AddAttributeDialog(editor, true, this, clickPosition);
        d.setVisible(true);
    }

    public void addAttribute(String name, String type, Point point) {
        ShaderNodeVariable param = new ShaderNodeVariable(type, "Attr", name);
        NodePanel node = diagramUiHandler.addInputPanel(this, param);
        node.setLocation(point);
        diagramUiHandler.refreshDiagram();
    }

    public void displayAddMatParamDialog(Point clickPosition) {
        AddMaterialParameterDialog d = new AddMaterialParameterDialog(editor, true, this, clickPosition);
        d.setVisible(true);
    }

    public void addMatParam(String name, String type, Point point) {
        String fixedType = type;
        if (type.equals("Color")) {
            fixedType = "Vector4";
        }
        fixedType = VarType.valueOf(fixedType).getGlslType();
        ShaderNodeVariable param = new ShaderNodeVariable(fixedType, "MatParam", name);
        NodePanel node = diagramUiHandler.addInputPanel(this, param);
        node.setLocation(point);
        diagramUiHandler.refreshDiagram();
        dataHandler.addMatParam(param, type);
    }

    public void displayAddWorldParamDialog(Point clickPosition) {
        AddWorldParameterDialog d = new AddWorldParameterDialog(editor, true, this, clickPosition);
        d.setVisible(true);
    }

    public void addWorldParam(UniformBinding binding, Point point) {
        dataHandler.addWorldParm(binding);
        ShaderNodeVariable param = new ShaderNodeVariable(binding.getGlslType(), "WorldParam", binding.name());
        NodePanel node = diagramUiHandler.addInputPanel(this, param);
        node.setLocation(point);
        diagramUiHandler.refreshDiagram();
    }


    public void displayAddNodeDialog(Point clickPosition) {
        AddNodeDialog d = new AddNodeDialog(editor, true, this, gui, clickPosition);
        d.setVisible(true);
    }

    public void addNodesFromDefs(List<ShaderNodeDefinition> defList, Point point) {

        for (ShaderNodeDefinition def : defList) {
            ShaderNode sn = new ShaderNode(def.getName(), def, null);
            sn.setName(fixNodeName(sn.getName()));

            NodePanel np = addShaderNode(sn);
            np.setLocation(point);
            np.revalidate();
        }
        diagramUiHandler.refreshDiagram();

    }

    private String fixNodeName(String name) {
        return diagramUiHandler.fixNodeName(name, 0);
    }


    public void multiMove(DraggablePanel movedPanel, int xOffset, int yOffset) {
        selectionHandler.multiMove(movedPanel, xOffset, yOffset);
    }

    public void multiStartDrag(DraggablePanel movedPanel) {
        selectionHandler.multiStartDrag(movedPanel);
    }

    public void multiStopDrag() {
        selectionHandler.multiStopDrag(this);
    }

    public void select(Selectable selectable, boolean multi) {
        if (selectable instanceof ShaderNodePanel) {
            ShaderNode node = dataHandler.getShaderNodeForKey(selectable.getKey());
            gui.getSpix().getBlackboard().set(MAT_DEF_EDITOR_SELECTED_ITEM, node);
        } else if (selectable instanceof Connection) {
            VariableMapping mapping = dataHandler.getMappingForKey(selectable.getKey());
            gui.getSpix().getBlackboard().set(MAT_DEF_EDITOR_SELECTED_ITEM, mapping);
        } else if (selectable instanceof InputPanel) {
            String key = selectable.getKey();
            if (key.startsWith(dataHandler.getCurrentTechnique().getName() + ".MatParam.")) {
                key = key.substring(key.lastIndexOf(".") + 1);
                MatParam param = matDef.getMaterialParam(key);
                gui.getSpix().getBlackboard().set(MAT_DEF_EDITOR_SELECTED_ITEM, new MatParamWrapper(param));
            }
        }
        selectionHandler.select(selectable, multi);
        diagramUiHandler.refreshDiagram();
    }

    public void findSelection(MouseEvent me, boolean multi) {
        //Click on the diagram, we are trying to find if we clicked in a connection area and select it
        Connection conn = diagramUiHandler.pickForConnection(me);

        if (conn != null) {
            select(conn, multi);
            me.consume();
            return;
        }

        //we didn't find anything, let's unselect
        selectionHandler.clearSelection();
        diagramUiHandler.refreshDiagram();
    }

    public void removeSelected() {

        int result = JOptionPane.showConfirmDialog(editor, "Delete all selected items, nodes and mappings?", "Delete Selected", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            selectionHandler.removeSelected(this);
        }
    }

    public void dispatchEventToDiagram(MouseEvent e, Component source) {
        diagramUiHandler.dispatchEventToDiagram(e, source);
    }

    public void onNodeMoved(NodePanel node) {
        diagramUiHandler.onNodeMoved(node);
    }


    private class SceneSelectionChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {

            if (!editor.isVisible()) {
                return;
            }

            //I receive this event before the property change.
            //Ask Paul about this as it seems it's intentional.
            if (evt instanceof ObservableList.ElementUpdatedEvent) {
                return;
            }

            if (evt.getNewValue() instanceof Geometry) {
                Geometry g = (Geometry) evt.getNewValue();
                populateMatParams(g);
            }
        }
    }

    public void initialize() {
        try {
            Blackboard blackboard = gui.getSpix().getBlackboard();
            SelectionModel m = blackboard.get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
            if (!(m.getSingleSelection() instanceof Geometry)) {
                return;
            }
            Geometry g = (Geometry) m.getSingleSelection();
            matDef = CloneUtils.cloneMatDef(g.getMaterial().getMaterialDef(), techniques);
            Map<String, Object> matDefMetadata = gui.getSpix().getService(MetadataService.class).getMetadata(matDef);
            populateMatParams(g);
            blackboard.set(MAT_DEF_EDITOR_SELECTED_MATDEF, new MatDefWrapper(matDef));
            blackboard.set(MAT_DEF_EDITOR_SELECTED_TECHNIQUE, new TechniqueDefWrapper(techniques.get(0)));

            gui.runOnSwing(new Runnable() {
                @Override
                public void run() {
                    editor.setTitle("Material Definition Editor - " + matDef.getAssetName());
                    initTechnique(techniques.get(0), matDef, matDefMetadata);
                }
            });

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

//    private class DiagramSelectionChangeListener implements PropertyChangeListener {
//        @Override
//        public void propertyChange(PropertyChangeEvent evt) {
//
//            //I receive this event before the property change.
//            //Ask Paul about this as it seems it's intentional.
//            if (evt instanceof ObservableList.ElementUpdatedEvent) {
//                return;
//            }
//
//            if (evt.getNewValue() instanceof VariableMapping) {
//
//
//                gui.runOnSwing(new Runnable() {
//                    @Override
//                    public void run() {
//                        editor.setTitle(matDef.getName());
//                        initTechnique(techniques.get(0), matDef);
//                    }
//                });
//
//            }
//        }
//    }

}

