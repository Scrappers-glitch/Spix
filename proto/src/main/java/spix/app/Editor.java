/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package spix.app;

import com.google.common.base.Predicates;
import com.jme3.app.*;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.shader.*;
import com.jme3.system.AppSettings;
import com.jme3.system.awt.AwtPanelsContext;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.*;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel;
import spix.app.action.*;
import spix.app.action.file.*;
import spix.app.form.*;
import spix.app.light.*;
import spix.app.material.*;
import spix.app.material.hack.*;
import spix.app.properties.*;
import spix.awt.AwtPanelState;
import spix.core.*;
import spix.core.Action;
import spix.swing.ActionUtils;
import spix.swing.*;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.panels.*;
import spix.swing.materialEditor.utils.NoneSelectedButtonGroup;
import spix.swing.sceneexplorer.SceneExplorerPanel;
import spix.type.Type;
import spix.ui.*;
import spix.undo.*;
import spix.undo.edit.LightAddEdit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.prefs.Preferences;

import static spix.app.DefaultConstants.*;


/**
 * @author Paul Speed
 */
public class Editor extends SimpleApplication {

    private volatile JFrame mainFrame;
    private Spix spix;
    private SwingGui gui;

    public static final String EDITOR_WIDTH = "Editor.width";
    public static final String EDITOR_HEIGHT = "Editor.height";
    public static final String EDITOR_X = "Editor.x";
    public static final String EDITOR_Y = "Editor.y";
    private Preferences prefs = Preferences.userNodeForPackage(Editor.class);

    public static void main(String[] args) throws Exception {

        JFrame.setDefaultLookAndFeelDecorated(true);
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        UIManager.setLookAndFeel(new SubstanceGraphiteGlassLookAndFeel());

        final Editor app = new Editor();
        app.setShowSettings(false);

        AppSettings settings = new AppSettings(true);
        settings.setCustomRenderer(AwtPanelsContext.class);
        settings.setFrameRate(30);
        app.setSettings(settings);
        app.start();
    }

    public Editor() throws Exception {
        super(new StatsAppState(), new DebugKeysAppState(), new BasicProfilerState(false),
                new FlyCamAppState(), new OrbitCameraState(false), new BlenderCameraState(true),
                new GridState(), new BackgroundColorState(),
                new SpixState(new Spix()),
                new FileIoAppState(),
                new SelectionHighlightState(),
                new TransformState(),
                new TranslationWidgetState(false),
                new ScaleWidgetState(false),
                new RotationWidgetState(true),
                new LightWidgetState(),
                new NodeWidgetState(),
                new MaterialAppState(),
                new DebugLightsState(),
                new DecoratorViewPortState() // Put this last because of some dodgy update vs render stuff
        );

        stateManager.attach(new ScreenshotAppState("", System.currentTimeMillis()) {
            @Override
            protected void writeImageFile(final File file) throws IOException {
                super.writeImageFile(file);
                System.out.println("Wrote file:" + file);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        spix.getService(MessageRequester.class).showMessage("File Saved", "Saved screenshot:" + file, null);
                    }
                });
            }
        });

        this.spix = stateManager.getState(SpixState.class).getSpix();

        spix.registerPropertySetFactory(Spatial.class, new SpatialPropertySetFactory());
        spix.registerPropertySetFactory(LightWrapper.class, new LightPropertySetFactory());
        spix.registerPropertySetFactory(MatDefWrapper.class, new MatDefPropertySetFactory());
        spix.registerPropertySetFactory(TechniqueDefWrapper.class, new TechniqueDefPropertySetFactory());
        spix.registerPropertySetFactory(ShaderNode.class, new ShaderNodePropertySetFactory());
        spix.registerPropertySetFactory(VariableMapping.class, new VariableMappingPropertySetFactory());
        spix.registerPropertySetFactory(Material.class, new MaterialPropertySetFactory());

        spix.registerFormFactory(new Type(Spatial.class), new SpatialFormFactory());
        spix.registerFormFactory(new Type(VariableMapping.class), new VariableMappingFormFactory());

        SelectionModel selectionModel = new SelectionModel();
        spix.getBlackboard().set(SELECTION_PROPERTY, selectionModel);
        selectionModel.setupHack(spix.getBlackboard(), "main.selection.singleSelect");


        // Have to create the frame on the AWT EDT.
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                mainFrame = new JFrame("jMonkeyEngine Tool Suite");
                mainFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/icons/model.gif")));
                mainFrame.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        prefs.putInt(EDITOR_WIDTH, e.getComponent().getWidth());
                        prefs.putInt(EDITOR_HEIGHT, e.getComponent().getHeight());
                    }

                    @Override
                    public void componentMoved(ComponentEvent e) {
                        prefs.putInt(EDITOR_X, e.getComponent().getX());
                        prefs.putInt(EDITOR_Y, e.getComponent().getY());
                    }
                });

                spix.getBlackboard().bind(SCENE_DIRTY, Editor.this, "sceneIsDirty");
                spix.getBlackboard().bind(SCENE_FILE_NAME, Editor.this, "sceneFileName");

                mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                mainFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        stop();
                    }
                });

                mainFrame.setJMenuBar(createMainMenu());


                JPanel centerPane = new JPanel();
                mainFrame.getContentPane().add(centerPane, BorderLayout.CENTER);

                centerPane.setLayout(new BorderLayout());


                // Register the SwingGui layer and let it handle all of the requests
                // for which it is capable.
                gui = spix.registerService(SwingGui.class, new SwingGui(spix, mainFrame));


                SceneExplorerPanel sceneExplorerPanel = new SceneExplorerPanel(DockPanel.Slot.West, centerPane, gui);
                sceneExplorerPanel.setPreferredSize(new Dimension(250, 10));
                sceneExplorerPanel.unDock();

                PropPanel propertiesPanel = new PropPanel(centerPane);
                propertiesPanel.setPreferredSize(new Dimension(250, 10));
                propertiesPanel.unDock();

                JToolBar eastToolBar = new JToolBar(JToolBar.VERTICAL);
                eastToolBar.setFloatable(false);
                mainFrame.getContentPane().add(eastToolBar, BorderLayout.EAST);
                NoneSelectedButtonGroup groupE = new NoneSelectedButtonGroup();
                groupE.add(propertiesPanel.getButton());
                eastToolBar.add(propertiesPanel.getButton());

                JToolBar westToolBar = new JToolBar(JToolBar.VERTICAL);
                westToolBar.setFloatable(false);
                mainFrame.getContentPane().add(westToolBar, BorderLayout.WEST);
                NoneSelectedButtonGroup groupW = new NoneSelectedButtonGroup();
                groupW.add(sceneExplorerPanel.getButton());
                westToolBar.add(sceneExplorerPanel.getButton());


                // Register a custom read-only display for Vector3fs that formats the values
                // a little better.
                gui.registerComponentFactory(Vector3f.class, new DefaultComponentFactory(new Vec3fStringFunction()));
                gui.registerComponentFactory(SwingGui.EDIT_CONTEXT, Vector3f.class, new DefaultComponentFactory(Vector3fPanel.class));
                gui.registerComponentFactory(SwingGui.EDIT_CONTEXT, Quaternion.class, new DefaultComponentFactory(QuaternionPanel.class));

                PropertyEditorPanel objectEditor = new PropertyEditorPanel(gui, "ui.editor");
                objectEditor.setPreferredSize(new Dimension(250, 100));
                propertiesPanel.setComponent(new JScrollPane(objectEditor));

                JPanel scenePane = new JPanel(new BorderLayout());
                centerPane.add(scenePane, BorderLayout.CENTER);
                scenePane.add(createSceneToolbar(), BorderLayout.NORTH);
                stateManager.attach(new AwtPanelState(scenePane, BorderLayout.CENTER));


                // Bind the selection to the editor panel, converting objects to
                // property set wrappers if appropriate.
                spix.getBlackboard().bind("main.selection.singleSelect",
                        objectEditor, "object",
                        new ToPropertySetFunction(spix));


//                final MatDefEditorWindow matDefEditorWindow = new MatDefEditorWindow(gui);
//                matDefEditorWindow.setVisible(true);
//                mainFrame.addWindowListener(new WindowAdapter() {
//                    @Override
//                    public void windowClosing(WindowEvent e) {
//                        matDefEditorWindow.dispose();
//                    }
//                });
//
//                spix.registerService(MaterialService.class, new MaterialService(stateManager.getState(MaterialAppState.class), gui));
            }
        });


        stateManager.getState(AwtPanelState.class).addEnabledCommand(new Runnable() {
            public void run() {
                if (!mainFrame.isVisible()) {
                    // By now we should have the panel inside

                    mainFrame.pack();
                    mainFrame.setLocationRelativeTo(null);

                    int width = prefs.getInt(EDITOR_WIDTH, 300);
                    int height = prefs.getInt(EDITOR_HEIGHT, 150);

                    int x = prefs.getInt(EDITOR_X, 300);
                    int y = prefs.getInt(EDITOR_Y, 150);

                    //test if the saved location is out of the screen (can happen when you have multiple screens.
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    int maxWidth = 0;
                    int maxHeight = 0;
                    for (GraphicsDevice screen : ge.getScreenDevices()) {
                        maxWidth += screen.getDisplayMode().getWidth();
                        maxHeight += screen.getDisplayMode().getHeight();
                    }
                    if (x >= maxWidth) {
                        x = maxWidth - width;
                    }
                    if (y >= maxHeight) {
                        y = maxHeight - height;
                    }

                    mainFrame.setSize(new Dimension(width, height));
                    mainFrame.setLocation(x, y);
                    mainFrame.setVisible(true);

                }
            }
        });

    }

    //maybe extract this in another class later...
    private boolean sceneDirty = false;
    public void setSceneIsDirty(boolean dirty){
        gui.runOnSwing(new Runnable() {
            @Override
            public void run() {
                if (dirty != sceneDirty) {
                    String title = mainFrame.getTitle();
                    if (title.endsWith("*")) {
                        title = title.substring(0, title.length() - 1);
                    }
                    if (dirty) {
                        title += "*";
                    }
                    mainFrame.setTitle(title);
                    sceneDirty = dirty;
                }
            }
        });
    }
    public void setSceneFileName(String fileName){
        gui.runOnSwing(new Runnable() {
            @Override
            public void run() {
                mainFrame.setTitle("jMonkeyEngine Tool Suite - " + spix.getBlackboard().get(MAIN_ASSETS_FOLDER) + " - " + spix.getBlackboard().get(SCENE_FILE_NAME));
            }
        });
    }


    private JMenuBar createMainMenu() {
        return ActionUtils.createActionMenuBar(createMainActions(), spix);
    }

    private JToolBar createSceneToolbar() {
        return ActionUtils.createActionToolbar(createSceneActions(), spix, JToolBar.HORIZONTAL);
    }

    private ActionList createSceneActions() {
        ActionList sceneActions = new DefaultActionList("scene");

        ActionList addAction = new DefaultActionList("Add");

        ActionList addSpatialAction = new DefaultActionList("Spatial");
        addSpatialAction.add(new AddSpatialAction("Node", AddSpatialAction.DEFAULT_NODE, spix));
        addSpatialAction.add(null);
        addSpatialAction.add(new AddSpatialAction("Box", AddSpatialAction.DEFAULT_BOX, spix));
        addSpatialAction.add(new AddSpatialAction("Quad", AddSpatialAction.DEFAULT_QUAD, spix));
        addSpatialAction.add(new AddSpatialAction("Sphere", AddSpatialAction.DEFAULT_SPHERE, spix));
        addSpatialAction.add(new AddSpatialAction("Cylinder", AddSpatialAction.DEFAULT_CYLINDER, spix));
        addSpatialAction.add(new AddSpatialAction("Torus", AddSpatialAction.DEFAULT_TORUS, spix));

        addAction.add(addSpatialAction);
        ActionList lightActions = new DefaultActionList("Light");
        createLightMenu(lightActions);

        addAction.add(lightActions);
        sceneActions.add(addAction);
        sceneActions.add(null);
        sceneActions.add(null);

        createTransformActions(sceneActions);
        sceneActions.add(null);
        sceneActions.add(null);

        Action toggleLight = new NopToggleAction("Debug Light", Icons.lightBulbOff, Icons.lightBulb) {
            @Override
            public void performAction(Spix spix) {
                Boolean on = spix.getBlackboard().get("view.debug.lights", Boolean.class);
                if (on == null) {
                    on = Boolean.FALSE;
                }
                spix.getBlackboard().set(VIEW_DEBUG_LIGHTS, !on);
            }
        };
        sceneActions.add(toggleLight);
        spix.getBlackboard().bind(VIEW_DEBUG_LIGHTS, toggleLight, "toggled");


        return sceneActions;
    }

    private ActionList createMainActions() {
        ActionList main = new DefaultActionList("root");

        ActionList file = main.add(new DefaultActionList("File"));
        FileIoAppState fileIoState = stateManager.getState(FileIoAppState.class);
        file.add(new NewFileAction(fileIoState));
        file.add(new OpenFileAction(fileIoState));
        file.add(new OpenRecentAction(spix, fileIoState));
        file.add(new ImportFileAction(fileIoState));
        file.add(null); // separator
        file.add(new SaveFileAction(fileIoState));
        file.add(new SaveAsFileAction(fileIoState));
        file.add(null); // separator
        file.add(new NopAction("Take Screenshot") {
            public void performAction(Spix spix) {
                stateManager.getState(ScreenshotAppState.class).takeScreenshot();
            }
        });
        file.add(null); // separator
        file.add(new NopAction("Exit") {
            public void performAction(Spix spix) {
                // Need to tell the app to shutdown... this is one case where
                // we need some back chain.  We'll cheat for now.
                // FIXME
                mainFrame.dispose();
            }
        });

        // Setup the undo manager
        UndoManager undoManager = new UndoManager(spix);
        spix.registerService(UndoManager.class, undoManager);

        ActionList edit = main.add(new DefaultActionList("Edit"));

        edit.add(new UndoAction(undoManager));
        edit.add(new RedoAction(undoManager));
        // Just some test edit actions
        //edit.add(null);
        //edit.add(new NopAction("Cut"));
        //edit.add(new NopAction("Copy"));
        //edit.add(new NopAction("Paste"));

        edit.add(null);
        //  ActionList transform = edit.add(new DefaultActionList("Transform"));

        //createTransformActions(transform);

        // Set the default camera mode
        //spix.getBlackboard().set("camera.mode", "blender");
        // ...except flycam app state is stupid and NPEs... we'll wait until
        // we replace it.  And when the blender state stops resetting the camera location.

        ActionList view = main.add(new DefaultActionList("View"));
        initPovMenus(view);

        ActionList camera = view.add(new DefaultActionList("Camera"));
        final ToggleAction fly = camera.add(new AbstractToggleAction("Fly") {
            public void performAction(Spix spix) {
                System.out.println("camera.mode=fly");
                spix.getBlackboard().set("camera.mode", "fly");
            }
        });
        spix.getBlackboard().bind("camera.mode", fly, "toggled", Predicates.equalTo("fly"));

        // Bind it to the flycam app state also
        spix.getBlackboard().bind("camera.mode", stateManager.getState(FlyCamAppState.class),
                "enabled", Predicates.equalTo("fly"));


        final ToggleAction orbit = camera.add(new AbstractToggleAction("Orbit") {
            public void performAction(Spix spix) {
                System.out.println("camera.mode=orbit");
                spix.getBlackboard().set("camera.mode", "orbit");
            }
        });
        spix.getBlackboard().bind("camera.mode", orbit, "toggled", Predicates.equalTo("orbit"));

        // Bind it to the orbit app state
        spix.getBlackboard().bind("camera.mode", stateManager.getState(OrbitCameraState.class),
                "enabled", Predicates.equalTo("orbit"));

        final ToggleAction blenderCam = camera.add(new AbstractToggleAction("Blender") {
            public void performAction(Spix spix) {
                System.out.println("camera.mode=blender");
                spix.getBlackboard().set("camera.mode", "blender");
            }
        });
        blenderCam.setToggled(true);
        spix.getBlackboard().bind("camera.mode", blenderCam, "toggled", Predicates.equalTo("blender"));

        // Bind it to the orbit app state
        spix.getBlackboard().bind("camera.mode", stateManager.getState(BlenderCameraState.class),
                "enabled", Predicates.equalTo("blender"));


        view.add(null);
        ToggleAction showGrid = view.add(new AbstractToggleAction("Grid") {
            public void performAction(Spix spix) {
                Boolean on = spix.getBlackboard().get("view.grid", Boolean.class);
                if (on == null) {
                    on = Boolean.FALSE;
                }
                spix.getBlackboard().set("view.grid", !on);
            }
        });
        spix.getBlackboard().bind("view.grid", showGrid, "toggled");

        // Set the initial state of the view.grid property to match the app state
        spix.getBlackboard().set("view.grid", stateManager.getState(GridState.class).isEnabled());

        // Bind it to the grid app state
        spix.getBlackboard().bind("view.grid", stateManager.getState(GridState.class),
                "enabled");

        view.add(new NopAction("Viewport Color") {
            public void performAction(final Spix spix) {
                ColorRGBA initialColor = spix.getBlackboard().get("viewport.color", ColorRGBA.class);
                spix.getService(ColorRequester.class).requestColor("Select the viewport color", initialColor, true,
                        new RequestCallback<ColorRGBA>() {
                            public void done(ColorRGBA color) {
                                spix.getBlackboard().set("viewport.color", color);
                            }
                        });
            }
        });
        spix.getBlackboard().bind("viewport.color", getStateManager().getState(BackgroundColorState.class), "backgroundColor");


        ActionList highlight = view.add(new DefaultActionList("Highlight"));
        ToggleAction highlightWireframe = highlight.add(new AbstractToggleAction("Wireframe") {
            public void performAction(Spix spix) {
                spix.getBlackboard().set("highlight.mode", SelectionHighlightState.HighlightMode.Wireframe);
            }
        });
        spix.getBlackboard().bind("highlight.mode", highlightWireframe, "toggled", Predicates.equalTo(SelectionHighlightState.HighlightMode.Wireframe));


        ToggleAction highlighOutLine = highlight.add(new AbstractToggleAction("Outline") {
            public void performAction(Spix spix) {
                spix.getBlackboard().set("highlight.mode", SelectionHighlightState.HighlightMode.Outline);
            }
        });
        spix.getBlackboard().bind("highlight.mode", highlighOutLine, "toggled", Predicates.equalTo(SelectionHighlightState.HighlightMode.Outline));

        // Bind the highlight.mode blackboard property to the state's property
        spix.getBlackboard().bind("highlight.mode", stateManager.getState(SelectionHighlightState.class),
                "highlightMode");

        // Set the mode default
        spix.getBlackboard().set("highlight.mode", SelectionHighlightState.HighlightMode.Outline);


        ActionList objects = main.add(createObjectActions());

        //ActionList test = main.add(createTestActions());


        ActionList sceneMenu = main.add(new DefaultActionList("Scene"));
        ActionList lightMenu = sceneMenu.add(new DefaultActionList("Add light"));
        createLightMenu(lightMenu);


        ActionList help = main.add(new DefaultActionList("Help"));
        help.add(new NopAction("About") {
            public void performAction(Spix spix) {
                spix.getService(MessageRequester.class).showMessage("About", "What's it all about?", null);
            }
        });


        return main;
    }

    private void createLightMenu(ActionList actionList) {
        actionList.add(new NopAction("Directional light") {
            @Override
            public void performAction(Spix spix) {
                addLight(new DirectionalLight(new Vector3f(1, -1, 0).normalizeLocal()));
            }
        });
        actionList.add(new NopAction("Spot light") {
            @Override
            public void performAction(Spix spix) {
                addLight(new SpotLight(new Vector3f(5, 5, 0), new Vector3f(-1, -1, 0).normalizeLocal(), 10));
            }
        });
        actionList.add(new NopAction("Point light") {
            @Override
            public void performAction(Spix spix) {
                addLight(new PointLight(new Vector3f(0, 3, 0), 10));
            }
        });
        actionList.add(new NopAction("Ambient light") {
            @Override
            public void performAction(Spix spix) {
                addLight(new AmbientLight());
            }
        });
    }

    private void createTransformActions(ActionList transform) {
        ToggleAction translate = transform.add(new NopToggleAction("Translate", Icons.translateOff, Icons.translate) {

            public void performAction(Spix spix) {
                spix.getBlackboard().set("transform.mode", "translate");
            }
        });
        spix.getBlackboard().bind("transform.mode", translate, "toggled", Predicates.equalTo("translate"));

        // Bind it to the translation widget app state also
        spix.getBlackboard().bind("transform.mode", stateManager.getState(TranslationWidgetState.class),
                "enabled", Predicates.equalTo("translate"));


        ToggleAction scale = transform.add(new NopToggleAction("Scale", Icons.scaleOff, Icons.scale) {
            public void performAction(Spix spix) {
                spix.getBlackboard().set("transform.mode", "scale");
            }
        });

        spix.getBlackboard().bind("transform.mode", scale, "toggled", Predicates.equalTo("scale"));

        // Bind it to the translation widget app state also
        spix.getBlackboard().bind("transform.mode", stateManager.getState(ScaleWidgetState.class),
                "enabled", Predicates.equalTo("scale"));

        ToggleAction rotate = transform.add(new NopToggleAction("Rotate", Icons.rotateOff, Icons.rotate) {
            public void performAction(Spix spix) {
                spix.getBlackboard().set("transform.mode", "rotate");
            }
        });

        spix.getBlackboard().bind("transform.mode", rotate, "toggled", Predicates.equalTo("rotate"));

        // Bind it to the translation widget app state also
        spix.getBlackboard().bind("transform.mode", stateManager.getState(RotationWidgetState.class),
                "enabled", Predicates.equalTo("rotate"));

        // And translate by default
        spix.getBlackboard().set("transform.mode", "rotate");

    }

    private void initPovMenus(ActionList camera) {
        ActionList povMenu = camera.add(new DefaultActionList("Point of view"));
        povMenu.add(new NopAction("Front") {
            public void performAction(final Spix spix) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if (bcs == null) {
                    return;
                }
                bcs.switchToFront();
            }
        });
        povMenu.add(new NopAction("Back") {
            public void performAction(final Spix spix) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if (bcs == null) {
                    return;
                }
                bcs.switchToBack();
            }
        });
        povMenu.add(new NopAction("Left") {
            public void performAction(final Spix spix) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if (bcs == null) {
                    return;
                }
                bcs.switchToLeft();
            }
        });
        povMenu.add(new NopAction("Right") {
            public void performAction(final Spix spix) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if (bcs == null) {
                    return;
                }
                bcs.switchToRight();
            }
        });
        povMenu.add(new NopAction("Top") {
            public void performAction(final Spix spix) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if (bcs == null) {
                    return;
                }
                bcs.switchToTop();
            }
        });
        povMenu.add(new NopAction("Bottom") {
            public void performAction(final Spix spix) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if (bcs == null) {
                    return;
                }
                bcs.switchToBottom();
            }
        });
    }

    private ActionList createObjectActions() {
        ActionList objects = new DefaultActionList("Selection");

        DeleteAction delete = new DeleteAction(spix);
        objects.add(delete);


        AnimationActionList animation = objects.add(new AnimationActionList("Animation"));
        spix.getBlackboard().bind("main.selection.singleSelect",
                animation, "selection");

        NopAction test1 = objects.add(new NopAction("Clone Test 1") {
            public void performAction(Spix spix) {
                cloneTest1();
            }
        });
        spix.getBlackboard().bind("main.selection.singleSelect",
                test1, "enabled", Predicates.instanceOf(Spatial.class));

        NopAction test2 = objects.add(new NopAction("Clone Test 2") {
            public void performAction(Spix spix) {
                cloneTest2();
            }
        });
        spix.getBlackboard().bind("main.selection.singleSelect",
                test2, "enabled", Predicates.instanceOf(Spatial.class));

        NopAction test3 = objects.add(new NopAction("Clone Test 3") {
            public void performAction(Spix spix) {
                cloneTest3();
            }
        });
        spix.getBlackboard().bind("main.selection.singleSelect",
                test3, "enabled", Predicates.instanceOf(Spatial.class));

        return objects;
    }
    @Override
    public void simpleInitApp() {

        stateManager.getState(FlyCamAppState.class).setEnabled(false);
        stateManager.getState(StatsAppState.class).setDisplayStatView(false);
        stateManager.getState(StatsAppState.class).setDisplayFps(false);
        flyCam.setDragToRotate(true);

        stateManager.getState(SpixState.class).getSpix().getBlackboard().set(ASSET_MANAGER, assetManager);

        // Set an initial camera position
        cam.setLocation(new Vector3f(0, 1, 10));

        // Because we will use Lemur for some things... go ahead and setup
        // the very basics
        GuiGlobals.initialize(this);

        // Setup for some scene picking... need to move this to an app state or something
        // but we're just hacking
        CursorEventControl.addListenersToSpatial(rootNode, new CursorListener() {

            private CursorMotionEvent lastMotion;

            public void cursorButtonEvent(CursorButtonEvent event, Spatial target, Spatial capture) {
                //System.out.println("cursorButtonEvent(" + event + ", " + target + ", " + capture + ")");

                if (!event.isPressed() && lastMotion != null) {
                    // Set the selection
                    Geometry selected = null;
                    if (lastMotion.getCollision() != null) {
                        selected = lastMotion.getCollision().getGeometry();
                    }
                    //System.out.println("Setting selection to:" + selected);
                    spix.getBlackboard().get("main.selection", SelectionModel.class).setSingleSelection(selected);
                }
            }

            public void cursorEntered(CursorMotionEvent event, Spatial target, Spatial capture) {
                // System.out.println("cursorEntered(" + event + ", " + target + ", " + capture + ")");
            }

            public void cursorExited(CursorMotionEvent event, Spatial target, Spatial capture) {
                //System.out.println("cursorExited(" + event + ", " + target + ", " + capture + ")");
            }

            public void cursorMoved(CursorMotionEvent event, Spatial target, Spatial capture) {
                //System.out.println("cursorMoved(" + event + ", " + target + ", " + capture + ")");
                this.lastMotion = event;
            }

        });
    }

    @Override
    public void simpleUpdate(float tpf) {

    }


    private void addSpatial(Spatial spatial) {

        spatial.setLocalTranslation(0, 0, 0);

        // For now, find out where to put the scene so that it is next to whatever
        // is currently loaded
        BoundingBox currentBounds = (BoundingBox) rootNode.getWorldBound();
        BoundingBox modelBounds = (BoundingBox) spatial.getWorldBound();

        System.out.println("root bounds:" + currentBounds);
        System.out.println("model bounds:" + modelBounds);

        float worldRight = currentBounds.getCenter().x + currentBounds.getXExtent();
        float modelLeft = -modelBounds.getCenter().x + modelBounds.getXExtent();

        spatial.setLocalTranslation(worldRight + modelLeft, 0, 0);
        rootNode.attachChild(spatial);
    }

    private Spatial getSelectedModel() {
        Spatial selected = spix.getBlackboard().get("main.selection.singleSelect", Spatial.class);
        if (selected == null) {
            return null;
        }

        // We need to traverse up and find the spatial that was actually loaded...
        // the 'node' that contained us.  This is a bit of a hack but we keep
        // going up until we find a node with an asset key.
        for (Spatial parent = selected.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.getKey() != null) {
                selected = parent;
                break;
            }
        }
        return selected;
    }

    private void cloneTest1() {
        Spatial selected = getSelectedModel();
        if (selected == null) {
            return;
        }
        System.out.println("Cloning, cloning materials.");
        addSpatial(selected.clone());
    }

    private void cloneTest2() {
        Spatial selected = getSelectedModel();
        if (selected == null) {
            return;
        }
        System.out.println("Cloning, sharing materials.");
        addSpatial(selected.clone(false));
    }

    private void cloneTest3() {
        Spatial selected = getSelectedModel();
        if (selected == null) {
            return;
        }
        System.out.println("Deep cloning.");
        addSpatial(selected.deepClone());
    }

    private void addLight(Light l) {
        Spatial selected = getSelectedSpatial();
        Spatial anchor;
        Spatial spatial = spix.getBlackboard().get(SCENE_ROOT, Spatial.class);
        if (selected != null) {
            anchor = selected;
            while (anchor != spatial && !(anchor instanceof Node)) {
                anchor = anchor.getParent();
            }
        } else {
            anchor = spatial;
        }

        UndoManager um = spix.getService(UndoManager.class);
        LightAddEdit edit = new LightAddEdit(anchor, l);
        edit.redo(spix);
        um.addEdit(edit);

    }

    private Spatial getSelectedSpatial() {
        SelectionModel model = getStateManager().getState(SpixState.class).getSpix().getBlackboard().get(SELECTION_PROPERTY, SelectionModel.class);
        Object obj = model.getSingleSelection();
        if (obj != null && obj instanceof Spatial) {
            return (Spatial) obj;
        }
        return null;
    }

    public static class NopAction extends spix.core.AbstractAction {

        public NopAction(String name) {
            super(name);
        }

        public NopAction(String id, ImageIcon icon) {
            super(id, "");
            put(LARGE_ICON, icon);
        }

        public NopAction(String name, String accelerator) {
            super(name);
            put(ACCELERATOR, accelerator);
        }

        public void performAction(Spix spix) {
        }
    }

    public static class NopToggleAction extends spix.core.AbstractToggleAction {

        public NopToggleAction(String name) {
            super(name);
        }

        public NopToggleAction(String id, ImageIcon icon, ImageIcon toggledIcon) {
            super(id);
            put(TOOLTIP, id);
            put(LARGE_ICON, icon);
            put(TOGGLED_LARGE_ICON, toggledIcon);
        }

        public NopToggleAction(String name, String accelerator) {
            super(name);
            put(ACCELERATOR, accelerator);
        }

        public void performAction(Spix spix) {
        }
    }
}
