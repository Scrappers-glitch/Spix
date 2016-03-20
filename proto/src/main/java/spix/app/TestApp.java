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
import spix.BeanProperty;
import spix.awt.AwtPanelState;
import spix.core.*;
import spix.core.Action;
import spix.swing.ActionUtils;
import spix.swing.*;
import spix.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
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
              new FlyCamAppState(), new OrbitCameraState(false),
              new DecoratorViewPortState(), new GridState(),
              new SelectionHighlightState(),
              new SpixState(new Spix())); 
 
        stateManager.attach(new ScreenshotAppState("", System.currentTimeMillis()) {
            @Override
            protected void writeImageFile( final File file ) throws IOException {
                super.writeImageFile(file);
System.out.println("Wrote file:" + file);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // For now do it the 'incorrect' way 
                        //JOptionPane.showMessageDialog(mainFrame, "Saved screenshot:" + file);
                        spix.getService(MessageRequester.class).showMessage("File Saved", "Saved screenshot:" + file, null); 
                    }
                });
            }
        });
 
        //this.spix = new Spix();
        this.spix = stateManager.getState(SpixState.class).getSpix();

        SelectionModel selectionModel = new SelectionModel();
        spix.getBlackboard().set("main.selection", selectionModel);
        selectionModel.setupHack(spix.getBlackboard(), "main.selection.singleSelect");
        BeanProperty highlight = BeanProperty.create(getStateManager().getState(SelectionHighlightState.class), "highlightMode");
        spix.getBlackboard().set("highlight.mode", highlight);
 
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
                left.add(testLabel);
                split.add(left, JSplitPane.LEFT);
                mainFrame.getContentPane().add(split, BorderLayout.CENTER); 
 
                stateManager.attach(new AwtPanelState(split, JSplitPane.RIGHT));
                
                // Register some handlers that depend on the main window
                spix.registerRequestHandler(GetFile.class, new GetFileHandler(mainFrame));
                
                // An alternate approach that uses more defined services
                spix.registerService(FileRequester.class, new SwingFileRequester(spix, mainFrame));
                spix.registerService(MessageRequester.class, new SwingMessageRequester(mainFrame));
                
                
                // Setup a selection test to change the test label
                spix.getBlackboard().bind("main.selection.singleSelect", 
                                           testLabel, "text", ToStringFunction.INSTANCE);
                
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
                spix.request(new GetFile("Open Scene", "JME Object", "j3o", true), 
                             new RequestCallback<File>() {
                                public void done( File f ) {
                                    System.out.println("Need to load:" + f + "   Thread:" + Thread.currentThread());
                                    loadFile(f);
                                }
                             });
            }
        });
        file.add(new NopAction("Open 2") {
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
        fly.setToggled(true);
        spix.getBlackboard().bind("camera.mode", fly, "toggled", Predicates.equalTo("fly"));
 
        // Bind it to the flycam app state also
        spix.getBlackboard().bind("camera.mode", stateManager.getState(FlyCamAppState.class), 
                                  "enabled", Predicates.equalTo("fly"));
        
        
        /*
        final Function<Boolean, Void> testSetter = Reflection.setter(fly, "setToggled", Boolean.class);
        System.out.println("test setter:" + testSetter);
 
        spix.getBlackboard().addListener("camera.mode", new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent event ) {
                String s = spix.getBlackboard().get("camera.mode", String.class);
                System.out.println("fly listener... s:" + s + "  propval:" +event.getNewValue());                
                //fly.setToggled("fly".equals(s));
                testSetter.apply("fly".equals(s));   
            }
        });*/
        
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
 
        /*
        spix.getBlackboard().addListener("camera.mode", new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent event ) {
                String s = spix.getBlackboard().get("camera.mode", String.class);
                System.out.println("orbit listener... s:" + s + "  propval:" +event.getNewValue());                
                orbit.setToggled("orbit".equals(s));   
            }
        });*/

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


        ActionList highlight = main.add(new DefaultActionList("Highlight"));
        ToggleAction highlightWireframe = highlight.add(new AbstractToggleAction("Wireframe") {
            public void performAction( Spix spix ) {
                BeanProperty mode = spix.getBlackboard().get("highlight.mode", BeanProperty.class);
                mode.setValue(SelectionHighlightState.HighlightMode.Wireframe);
                spix.getBlackboard().set("highlight.mode", mode);
            }
        });
        highlightWireframe.isToggled();
        //@Paul Not sure how to use the predicate here.
        //Clearly something is missing to untoggle the other check box, but since my value is embed in a BeanProperty, idk how to test it.
        spix.getBlackboard().bind("highlight.mode", highlightWireframe, "toggled", Predicates.equalTo(SelectionHighlightState.HighlightMode.Wireframe));


        ToggleAction highlighOutLine = highlight.add(new AbstractToggleAction("Outline") {
            public void performAction( Spix spix ) {
                BeanProperty mode = spix.getBlackboard().get("highlight.mode", BeanProperty.class);
                mode.setValue(SelectionHighlightState.HighlightMode.Outline);
                spix.getBlackboard().set("highlight.mode", mode);
            }
        });
        spix.getBlackboard().bind("highlight.mode", highlighOutLine, "toggled", Predicates.equalTo(SelectionHighlightState.HighlightMode.Outline));

        
        ActionList test = main.add(createTestActions());
        
        ActionList help = main.add(new DefaultActionList("Help"));
        help.add(new NopAction("About") {
            public void performAction( Spix spix ) {
                // Another case where we'll cheat until we have proper
                // user request objects
                //JOptionPane.showMessageDialog(mainFrame, "What's it all about?");
                spix.getService(MessageRequester.class).showMessage("About", "What's it all about?", null); 
            }
        });
                
        return main;        
    }
    
    private ActionList createTestActions() {
        final ActionList testActions = new DefaultActionList("Test");
        final ActionList testActions2 = new DefaultActionList("Test");
 
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
        
 
        spix.getBlackboard().addListener("test.list", new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent event ) {
                System.out.println("=======test.list changed:" + event);
            }
        });
        
        // Testing something
        groovy.util.ObservableList list1 = new groovy.util.ObservableList();
        spix.getBlackboard().set("test.list", list1);

        System.out.println("---- adding Testing 1");
        list1.add("Testing 1");
        System.out.println("---- adding Testing 2");
        list1.add("Testing 2");
        list1.clear();
        System.out.println("---- adding Single");
        list1.add("Single");

        groovy.util.ObservableList list2 = new groovy.util.ObservableList();
        spix.getBlackboard().set("test.list", list2);

        System.out.println("---- adding Silent");
        list1.add("Silent");

        /*        
        SelectionModel testModel = new SelectionModel();
        spix.getBlackboard().set("mainSelection", testModel);
        testModel.setupHack(spix.getBlackboard(), "main.selection.singleSelect");
        
        spix.getBlackboard().addListener("main.selection.singleSelect", new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent event ) {
                System.out.println("main.selection.singleSelect change:" + event.getNewValue());
            }
        });
    
        System.out.println("---- adding 1");
        testModel.add("1");    
        System.out.println("---- adding 2");
        testModel.add("2");
        System.out.println("---- adding 3");
        testModel.add("3");    
        System.out.println("---- setting a");
        testModel.setSingleSelection("a");
        System.out.println("---- clearing");        
        testModel.clear();
        */    
            
    }

    @Override
    public void simpleUpdate( float tpf ) {
        spix.runTasks();
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

    public static class NopAction extends spix.core.AbstractAction {
        
        public NopAction( String name ) {
            super(name);
        }
        
        public void performAction( Spix spix ) {
        }
    } 
}
