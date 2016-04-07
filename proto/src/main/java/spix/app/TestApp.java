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
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.awt.AwtPanelsContext;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.*;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel;

import spix.app.light.*;
import spix.app.properties.*;
import spix.awt.AwtPanelState;
import spix.core.*;
import spix.core.Action;
import spix.swing.ActionUtils;
import spix.swing.*;
import spix.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;


/**
 *
 *
 *  @author    Paul Speed
 */
public class TestApp extends SimpleApplication {

    private volatile JFrame mainFrame;
    private Spix spix;

    public static void main(String[] args) throws Exception {

        JFrame.setDefaultLookAndFeelDecorated(true);
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        UIManager.setLookAndFeel(new SubstanceGraphiteGlassLookAndFeel());

        final TestApp app = new TestApp();
        app.setShowSettings(false);

        AppSettings settings = new AppSettings(true);
        settings.setCustomRenderer(AwtPanelsContext.class);
        settings.setFrameRate(60);
        app.setSettings(settings);
        app.start();
    }

    public TestApp() throws Exception {
        super(new StatsAppState(), new DebugKeysAppState(), new BasicProfilerState(false),
              new FlyCamAppState(), new OrbitCameraState(false), new BlenderCameraState(true),
              new GridState(), new BackgroundColorState(),
              new SelectionHighlightState(),
              new TranslationWidgetState(),
                new LightWidgetState(),
              new DecoratorViewPortState(), // Put this last because of some dodgy update vs render stuff
              new SpixState(new Spix()));

        stateManager.attach(new ScreenshotAppState("", System.currentTimeMillis()) {
            @Override
            protected void writeImageFile( final File file ) throws IOException {
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

        SelectionModel selectionModel = new SelectionModel();
        spix.getBlackboard().set("main.selection", selectionModel);
        selectionModel.setupHack(spix.getBlackboard(), "main.selection.singleSelect");

        // Have to create the frame on the AWT EDT.
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                mainFrame = new JFrame("Test App");
                mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                mainFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        stop();
                    }
                });

                mainFrame.setJMenuBar(createMainMenu());

                JSplitPane split = new JSplitPane();
                split.setContinuousLayout(false);
                split.setBackground(Color.black);

                JPanel left = new JPanel();
                JLabel testLabel = new JLabel("Testing");
                JLabel testLabel2 = new JLabel("Testing2");
                left.add(testLabel);
                //left.add(testLabel2);
                split.add(left, JSplitPane.LEFT);
                mainFrame.getContentPane().add(split, BorderLayout.CENTER);

                // For the right panel, we'll split it again so we can have a property
                // pane on the right-right.
                JSplitPane rightSplit = new JSplitPane();
                split.add(rightSplit, JSplitPane.RIGHT);
                rightSplit.setContinuousLayout(false);
                rightSplit.setBackground(Color.black);

                PropertyEditorPanel objectEditor = new PropertyEditorPanel(spix);
                rightSplit.add(objectEditor, JSplitPane.RIGHT);

                stateManager.attach(new AwtPanelState(rightSplit, JSplitPane.LEFT));

                // Register services to handle certain UI requests
                spix.registerService(FileRequester.class, new SwingFileRequester(spix, mainFrame));
                spix.registerService(MessageRequester.class, new SwingMessageRequester(mainFrame));
                spix.registerService(ColorRequester.class, new SwingColorRequester(spix, mainFrame));


                // Setup a selection test to change the test label
                spix.getBlackboard().bind("main.selection.singleSelect",
                                           testLabel, "text", ToStringFunction.INSTANCE);

                // Bind the selection to the editor panel, converting objects to
                // property set wrappers if appropriate.
                spix.getBlackboard().bind("main.selection.singleSelect",
                                          objectEditor, "object",
                                          new ToPropertySetFunction(spix));

                /*spix.getBlackboard().bind("main.selection.singleSelect",
                                           testLabel2, "text",
                                           Functions.compose(
                                                ToStringFunction.INSTANCE,
                                                new ToPropertySetFunction(spix)));*/

                spix.getBlackboard().get("main.selection", SelectionModel.class).add("Test Selection");
            }
        });


        stateManager.getState(AwtPanelState.class).addEnabledCommand(new Runnable() {
            public void run() {
                if( !mainFrame.isVisible() ) {
                    // By now we should have the panel inside
                    mainFrame.pack();
                    mainFrame.setLocationRelativeTo(null);
                    mainFrame.setVisible(true);
                }
            }
        });

    }

    private JMenuBar createMainMenu() {
        return ActionUtils.createActionMenuBar(createMainActions(), spix);
    }

    private ActionList createMainActions() {
        ActionList main = new DefaultActionList("root");

        ActionList file = main.add(new DefaultActionList("File"));
        file.add(new NopAction("New"));
        file.add(null); // separator
        file.add(new NopAction("Open") {
            public void performAction( Spix spix ) {
                // Uses the alternate requester service approach
                spix.getService(FileRequester.class).requestFile("Open Scene",
                                                                 "JME Object", "j3o", null, true,
                             new RequestCallback<File>() {
                                public void done( File f ) {
                                    System.out.println("Need to load:" + f + "   Thread:" + Thread.currentThread());
                                    loadFile(f);
                                }
                             });
            }
        });
        file.add(new NopAction("Save"));
        file.add(null); // separator
        file.add(new NopAction("Take Screenshot") {
            public void performAction( Spix spix ) {
                stateManager.getState(ScreenshotAppState.class).takeScreenshot();
            }
        });
        file.add(null); // separator
        file.add(new NopAction("Exit") {
            public void performAction( Spix spix ) {
                // Need to tell the app to shutdown... this is one case where
                // we need some back chain.  We'll cheat for now.
                // FIXME
                mainFrame.dispose();
            }
        });

        ActionList edit = main.add(new DefaultActionList("Edit"));
        ActionList transform = edit.add(new DefaultActionList("Transform"));

        ToggleAction translate = transform.add(new AbstractToggleAction("Translate") {
            public void performAction( Spix spix ) {
                spix.getBlackboard().set("transform.mode", "translate");
            }
        });
        spix.getBlackboard().bind("transform.mode", translate, "toggled", Predicates.equalTo("translate"));

        // Bind it to the translation widget app state also
        spix.getBlackboard().bind("transform.mode", stateManager.getState(TranslationWidgetState.class),
                                  "enabled", Predicates.equalTo("translate"));

        // And translate by default
        spix.getBlackboard().set("transform.mode", "translate");

        edit.add(null);
        // Just some test edit actions
        edit.add(new NopAction("Cut"));
        edit.add(new NopAction("Copy"));
        edit.add(new NopAction("Paste"));

        ActionList camera = main.add(new DefaultActionList("Camera"));
        final ToggleAction fly = camera.add(new AbstractToggleAction("Fly") {
            public void performAction( Spix spix ) {
                System.out.println("camera.mode=fly");
                spix.getBlackboard().set("camera.mode", "fly");
            }
        });
        spix.getBlackboard().bind("camera.mode", fly, "toggled", Predicates.equalTo("fly"));

        // Bind it to the flycam app state also
        spix.getBlackboard().bind("camera.mode", stateManager.getState(FlyCamAppState.class),
                                  "enabled", Predicates.equalTo("fly"));


        final ToggleAction orbit = camera.add(new AbstractToggleAction("Orbit") {
            public void performAction( Spix spix ) {
                System.out.println("camera.mode=orbit");
                spix.getBlackboard().set("camera.mode", "orbit");
            }
        });
        spix.getBlackboard().bind("camera.mode", orbit, "toggled", Predicates.equalTo("orbit"));

        // Bind it to the orbit app state
        spix.getBlackboard().bind("camera.mode", stateManager.getState(OrbitCameraState.class),
                                  "enabled", Predicates.equalTo("orbit"));

        final ToggleAction blenderCam = camera.add(new AbstractToggleAction("Blender") {
            public void performAction( Spix spix ) {
                System.out.println("camera.mode=blender");
                spix.getBlackboard().set("camera.mode", "blender");
            }
        });
        blenderCam.setToggled(true);
        spix.getBlackboard().bind("camera.mode", blenderCam, "toggled", Predicates.equalTo("blender"));

        // Bind it to the orbit app state
        spix.getBlackboard().bind("camera.mode", stateManager.getState(BlenderCameraState.class),
                "enabled", Predicates.equalTo("blender"));

        initPovMenus(camera);

        // Set the default camera mode
        //spix.getBlackboard().set("camera.mode", "blender");
        // ...except flycam app state is stupid and NPEs... we'll wait until
        // we replace it.  And when the blender state stops resetting the camera location.

        ActionList view = main.add(new DefaultActionList("View"));
        ToggleAction showGrid = view.add(new AbstractToggleAction("Grid") {
            public void performAction( Spix spix ) {
                Boolean on = spix.getBlackboard().get("view.grid", Boolean.class);
                if( on == null ) {
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
            public void performAction( final Spix spix ) {
                ColorRGBA initialColor = spix.getBlackboard().get("viewport.color", ColorRGBA.class);
                spix.getService(ColorRequester.class).requestColor("Select the viewport color", initialColor, true,
                        new RequestCallback<ColorRGBA>() {
                            public void done( ColorRGBA color ) {
                                spix.getBlackboard().set("viewport.color", color);
                            }
                        });
            }
        });
        spix.getBlackboard().bind("viewport.color", getStateManager().getState(BackgroundColorState.class), "backgroundColor");


        ActionList highlight = view.add(new DefaultActionList("Highlight"));
        ToggleAction highlightWireframe = highlight.add(new AbstractToggleAction("Wireframe") {
            public void performAction( Spix spix ) {
                spix.getBlackboard().set("highlight.mode", SelectionHighlightState.HighlightMode.Wireframe);
            }
        });
        spix.getBlackboard().bind("highlight.mode", highlightWireframe, "toggled", Predicates.equalTo(SelectionHighlightState.HighlightMode.Wireframe));


        ToggleAction highlighOutLine = highlight.add(new AbstractToggleAction("Outline") {
            public void performAction( Spix spix ) {
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
        lightMenu.add(new NopAction("Directional light") {
            @Override
            public void performAction(Spix spix) {
                addLight(DirectionalLight.class);
            }
        });
        lightMenu.add(new NopAction("Spot light") {
            @Override
            public void performAction(Spix spix) {
                addLight(SpotLight.class);
            }
        });
        lightMenu.add(new NopAction("Point light") {
            @Override
            public void performAction(Spix spix) {
                addLight(PointLight.class);
            }
        });
        lightMenu.add(new NopAction("Ambient light") {
            @Override
            public void performAction(Spix spix) {
                addLight(AmbientLight.class);
            }
        });


        ActionList help = main.add(new DefaultActionList("Help"));
        help.add(new NopAction("About") {
            public void performAction( Spix spix ) {
                spix.getService(MessageRequester.class).showMessage("About", "What's it all about?", null);
            }
        });



        return main;
    }

    private void initPovMenus(ActionList camera) {
        ActionList povMenu = camera.add(new DefaultActionList("Point of view"));
        povMenu.add(new NopAction("Front") {
            public void performAction( final Spix spix ) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if(bcs == null){
                    return;
                }
                bcs.switchToFront();
            }
        });
        povMenu.add(new NopAction("Back") {
            public void performAction( final Spix spix ) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if(bcs == null){
                    return;
                }
                bcs.switchToBack();
            }
        });
        povMenu.add(new NopAction("Left") {
            public void performAction( final Spix spix ) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if(bcs == null){
                    return;
                }
                bcs.switchToLeft();
            }
        });
        povMenu.add(new NopAction("Right") {
            public void performAction( final Spix spix ) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if(bcs == null){
                    return;
                }
                bcs.switchToRight();
            }
        });
        povMenu.add(new NopAction("Top") {
            public void performAction( final Spix spix ) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if(bcs == null){
                    return;
                }
                bcs.switchToTop();
            }
        });
        povMenu.add(new NopAction("Bottom") {
            public void performAction( final Spix spix ) {
                BlenderCameraState bcs = getStateManager().getState(BlenderCameraState.class);
                if(bcs == null){
                    return;
                }
                bcs.switchToBottom();
            }
        });
    }

    private ActionList createObjectActions() {
        ActionList objects = new DefaultActionList("Selection");

        AnimationActionList animation = objects.add(new AnimationActionList("Animation"));
        spix.getBlackboard().bind("main.selection.singleSelect",
                                  animation, "selection");

        NopAction test1 = objects.add(new NopAction("Clone Test 1") {
            public void performAction( Spix spix ) {
                cloneTest1();
            }
        });
        spix.getBlackboard().bind("main.selection.singleSelect",
                                  test1, "enabled", Predicates.instanceOf(Spatial.class));

        NopAction test2 = objects.add(new NopAction("Clone Test 2") {
            public void performAction( Spix spix ) {
                cloneTest2();
            }
        });
        spix.getBlackboard().bind("main.selection.singleSelect",
                                  test2, "enabled", Predicates.instanceOf(Spatial.class));

        NopAction test3 = objects.add(new NopAction("Clone Test 3") {
            public void performAction( Spix spix ) {
                cloneTest3();
            }
        });
        spix.getBlackboard().bind("main.selection.singleSelect",
                                  test3, "enabled", Predicates.instanceOf(Spatial.class));

        return objects;
    }

    private ActionList createTestActions() {
        final ActionList testActions = new DefaultActionList("Test");
        final ActionList testActions2 = new DefaultActionList("Test");

        testActions.add(new NopAction("Select Color") {
            public void performAction( Spix spix ) {
                spix.getService(ColorRequester.class).requestColor("Select a Test Color", null, false,
                                                                   new RequestCallback<ColorRGBA>() {
                    public void done( ColorRGBA color ) {
                        System.out.println("Selected color:" + color);
                    }
                });
            }
        });

        testActions.add(new NopAction("Select Color Interactive") {
            public void performAction( Spix spix ) {
                spix.getService(ColorRequester.class).requestColor("Select a Test Color", null, true,
                                                                   new RequestCallback<ColorRGBA>() {
                    public void done( ColorRGBA color ) {
                        System.out.println("Selected color:" + color);
                    }
                });
            }
        });

        // A self test
        final Action testAction = testActions.add(new spix.core.AbstractAction("Test") {
            private int count = 1;

            public void performAction( Spix spix ) {
                System.out.println("A test spix action.  Thread:" + Thread.currentThread());
                put(Action.NAME, "Test " + (++count));
            }
        });

        // An add test
        Action addAction = testActions.add(new spix.core.AbstractAction("Add") {
            public void performAction( Spix spix ) {
                testActions.add(testAction);
                testActions2.add(testAction);
            }
        });

        // A remove test
        Action removeAction = testActions.add(new spix.core.AbstractAction("Remove") {
            public void performAction( Spix spix ) {
                testActions.remove(testAction);
                testActions2.remove(testAction);
            }
        });

        testActions.add(testActions2);

        return testActions;
    }

    @Override
    public void simpleInitApp() {
        System.out.println("---------simpleInitApp()");

        stateManager.getState(FlyCamAppState.class).setEnabled(false);
        flyCam.setDragToRotate(true);

        // Set an initial camera position
        cam.setLocation(new Vector3f(0, 1, 10));


        Box b = new Box(Vector3f.ZERO, 1, 1, 1);
        Geometry geom = new Geometry("Box", b);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        //mat.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
        mat.setColor("Diffuse", ColorRGBA.Blue);
        mat.setColor("Ambient", ColorRGBA.Blue);
        mat.setBoolean("UseMaterialColors", true);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);

        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(-0.2f, -1, -0.3f).normalizeLocal());
        rootNode.addLight(light);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.5f, 0.5f, 0.2f, 1));
        rootNode.addLight(ambient);

        PointLight pl = new PointLight(new Vector3f(-2,0,-2), 4);
        rootNode.addLight(pl);

        SpotLight sl = new SpotLight(new Vector3f(3,3,-3),new Vector3f(-1f, -1, 1f).normalizeLocal(), 10, ColorRGBA.White.mult(2));
        sl.setSpotOuterAngle(FastMath.DEG_TO_RAD * 15);
        sl.setSpotInnerAngle(FastMath.DEG_TO_RAD * 10);
        rootNode.addLight(sl);

        // Because we will use Lemur for some things... go ahead and setup
        // the very basics
        GuiGlobals.initialize(this);

        // Setup for some scene picking... need to move this to an app state or something
        // but we're just hacking
        CursorEventControl.addListenersToSpatial(rootNode, new CursorListener() {

            private CursorMotionEvent lastMotion;

            public void cursorButtonEvent( CursorButtonEvent event, Spatial target, Spatial capture ) {
                System.out.println("cursorButtonEvent(" + event + ", " + target + ", " + capture + ")");

                if( !event.isPressed() && lastMotion != null ) {
                    // Set the selection
                    Geometry selected = null;
                    if( lastMotion.getCollision() != null ) {
                        selected = lastMotion.getCollision().getGeometry();
                    }
                    System.out.println("Setting selection to:" + selected);
                    spix.getBlackboard().get("main.selection", SelectionModel.class).setSingleSelection(selected);
                }
            }

            public void cursorEntered( CursorMotionEvent event, Spatial target, Spatial capture ) {
                System.out.println("cursorEntered(" + event + ", " + target + ", " + capture + ")");
            }

            public void cursorExited( CursorMotionEvent event, Spatial target, Spatial capture ) {
                System.out.println("cursorExited(" + event + ", " + target + ", " + capture + ")");
            }

            public void cursorMoved( CursorMotionEvent event, Spatial target, Spatial capture ) {
                //System.out.println("cursorMoved(" + event + ", " + target + ", " + capture + ")");
                this.lastMotion = event;
            }

        });

    }

    @Override
    public void simpleUpdate( float tpf ) {
//System.out.println("-----------frame-------------");
    }


    private void loadFile( File f ) {
        // JME doesn't really make this easy... so we cheat a little and make some
        // assumptions.
        File assetRoot = f.getParentFile();
        String modelPath = f.getName();
        while( assetRoot.getParentFile() != null && !"assets".equals(assetRoot.getName()) ) {
            modelPath = assetRoot.getName() + "/" + modelPath;
            assetRoot = assetRoot.getParentFile();
        }
        System.out.println("Asset root:" + assetRoot + "   modelPath:" + modelPath);

        assetManager.registerLocator(assetRoot.toString(), FileLocator.class);

        Spatial scene = assetManager.loadModel(modelPath);

        System.out.println("Scene:" + scene);

        // For now, find out where to put the scene so that it is next to whatever
        // is currently loaded
        BoundingBox currentBounds = (BoundingBox)rootNode.getWorldBound();
        BoundingBox modelBounds = (BoundingBox)scene.getWorldBound();

        System.out.println("root bounds:" + currentBounds);
        System.out.println("model bounds:" + modelBounds);

        float worldRight = currentBounds.getCenter().x + currentBounds.getXExtent();
        float modelLeft = -modelBounds.getCenter().x + modelBounds.getXExtent();

        scene.setLocalTranslation(worldRight + modelLeft, 0, 0);
        rootNode.attachChild(scene);
    }

    private void addSpatial( Spatial spatial ) {

        spatial.setLocalTranslation(0, 0, 0);

        // For now, find out where to put the scene so that it is next to whatever
        // is currently loaded
        BoundingBox currentBounds = (BoundingBox)rootNode.getWorldBound();
        BoundingBox modelBounds = (BoundingBox)spatial.getWorldBound();

        System.out.println("root bounds:" + currentBounds);
        System.out.println("model bounds:" + modelBounds);

        float worldRight = currentBounds.getCenter().x + currentBounds.getXExtent();
        float modelLeft = -modelBounds.getCenter().x + modelBounds.getXExtent();

        spatial.setLocalTranslation(worldRight + modelLeft, 0, 0);
        rootNode.attachChild(spatial);
    }

    private Spatial getSelectedModel() {
        Spatial selected = spix.getBlackboard().get("main.selection.singleSelect", Spatial.class);
        if( selected == null ) {
            return null;
        }

        // We need to traverse up and find the spatial that was actually loaded...
        // the 'node' that contained us.  This is a bit of a hack but we keep
        // going up until we find a node with an asset key.
        for( Spatial parent = selected.getParent(); parent != null; parent = parent.getParent() ) {
            if( parent.getKey() != null ) {
                selected = parent;
                break;
            }
        }
        return selected;
    }

    private void cloneTest1() {
        Spatial selected = getSelectedModel();
        if( selected == null ) {
            return;
        }
System.out.println("Cloning, cloning materials.");        
        addSpatial(selected.clone());
    }

    private void cloneTest2() {
        Spatial selected = getSelectedModel();
        if( selected == null ) {
            return;
        }
System.out.println("Cloning, sharing materials.");        
        addSpatial(selected.clone(false));
    }

    private void cloneTest3() {
        Spatial selected = getSelectedModel();
        if( selected == null ) {
            return;
        }
System.out.println("Deep cloning.");
        addSpatial(selected.deepClone());
    }

    private void addLight(Class<? extends Light> lightClass){
        Spatial selected = getSelectedSpatial();
        Spatial anchor;
        if(selected != null){
            anchor = selected;
            while(anchor != null && !(anchor instanceof Node)){
                anchor = anchor.getParent();
            }
        }else {
            anchor = rootNode;
        }

        try {
            Light l = lightClass.newInstance();
            anchor.addLight(l);
            getStateManager().getState(LightWidgetState.class).addLight(rootNode, l);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private Spatial getSelectedSpatial() {
        SelectionModel model = getStateManager().getState(SpixState.class).getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        Object obj = model.getSingleSelection();
        if( obj != null && obj instanceof Spatial){
            return (Spatial) obj;
        }
        return null;
    }

    public static class NopAction extends spix.core.AbstractAction {

        public NopAction( String name ) {
            super(name);
        }

        public void performAction( Spix spix ) {
        }
    }
}
