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

import com.jme3.app.*;
import com.jme3.app.state.*;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.awt.AwtPanel;
import com.jme3.system.awt.AwtPanelsContext;
import com.jme3.system.awt.PaintMode;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import javax.swing.*;

import org.pushingpixels.substance.api.skin.*;

import com.google.common.base.*;

import spix.awt.*;
import spix.core.AbstractToggleAction;
import spix.core.Action;
import spix.core.ActionList;
import spix.core.DefaultActionList;
import spix.core.Spix;
import spix.core.ToggleAction;
import spix.reflect.*;
import spix.swing.*;
import spix.ui.*;


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
              new FlyCamAppState()); 
 
        stateManager.attach(new ScreenshotAppState("", System.currentTimeMillis()) {
            /*
            requires JME head until apha 3 is pushed
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
            }*/
        });
 
        this.spix = new Spix();
 
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
                left.add(new JLabel("Testing"));
                split.add(left, JSplitPane.LEFT);
                mainFrame.getContentPane().add(split, BorderLayout.CENTER); 
 
                stateManager.attach(new AwtPanelState(split, JSplitPane.RIGHT));
                
                // Register some handlers that depend on the main window
                spix.registerRequestHandler(GetFile.class, new GetFileHandler(mainFrame));
                
                // An alternate approach that uses more defined services
                spix.registerService(FileRequester.class, new SwingFileRequester(spix, mainFrame));
                spix.registerService(MessageRequester.class, new SwingMessageRequester(mainFrame));
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
        Function<Boolean, Void> testSetter = Reflection.setter(fly, "setToggled", Boolean.class);
        System.out.println("test setter:" + testSetter);
 
        spix.getBlackboard().addListener("camera.mode", new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent event ) {
                String s = spix.getBlackboard().get("camera.mode", String.class);
                System.out.println("fly listener... s:" + s + "  propval:" +event.getNewValue());                
                fly.setToggled("fly".equals(s));   
            }
        });
        
        final ToggleAction orbit = camera.add(new AbstractToggleAction("Orbit") {
            public void performAction( Spix spix ) {
                System.out.println("camera.mode=orbit");
                spix.getBlackboard().set("camera.mode", "orbit");
            }
        });
        
        spix.getBlackboard().addListener("camera.mode", new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent event ) {
                String s = spix.getBlackboard().get("camera.mode", String.class);
                System.out.println("orbit listener... s:" + s + "  propval:" +event.getNewValue());                
                orbit.setToggled("orbit".equals(s));   
            }
        });
        
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
