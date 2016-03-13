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

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.awt.AwtPanel;
import com.jme3.system.awt.AwtPanelsContext;
import com.jme3.system.awt.PaintMode;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import javax.swing.*;

import org.pushingpixels.substance.api.skin.*;

import spix.awt.*;
import spix.core.Action;
import spix.core.ActionList;
import spix.core.DefaultActionList;
import spix.core.Spix;
import spix.swing.*;


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
                
                //stateManager.attach(new AwtPanelState(mainFrame.getContentPane(), BorderLayout.CENTER));
 
                JSplitPane split = new JSplitPane();
                split.setContinuousLayout(false);
                split.setBackground(Color.black);
                
                JPanel left = new JPanel();
                left.add(new JLabel("Testing"));
                split.add(left, JSplitPane.LEFT);
                mainFrame.getContentPane().add(split, BorderLayout.CENTER); 
 

                stateManager.attach(new AwtPanelState(split, JSplitPane.RIGHT));
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
/*    
        // Just for testing for now
        JMenuBar result = new JMenuBar();
        
        JMenu file = new JMenu("File");
        file.add(new AbstractAction("New") {
            public void actionPerformed( ActionEvent event ) {
            }
        });
        file.add(new AbstractAction("Open") {
            public void actionPerformed( ActionEvent event ) {
            }
        });
        file.add(new AbstractAction("Save") {
            public void actionPerformed( ActionEvent event ) {
            }
        });
        file.add(new AbstractAction("Exit") {
            public void actionPerformed( ActionEvent event ) {
                mainFrame.dispose();
            }
        });
        
        
        JMenu edit = new JMenu("Edit");
        edit.add(new AbstractAction("Cut") {
            public void actionPerformed( ActionEvent event ) {
            }
        });
        edit.add(new AbstractAction("Copy") {
            public void actionPerformed( ActionEvent event ) {
            }
        });
        edit.add(new AbstractAction("Paste") {
            public void actionPerformed( ActionEvent event ) {
            }
        });
 
        PropertyChangeListener testListener = new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent event ) {
                System.out.println("test:" + event);
            }
        };
 
        ActionList testActions = createTestActions();
        JMenuItem test = ActionUtils.createActionMenuItem(testActions, spix);
 */       
        /*
        JMenu test = new JMenu("Test");
        
        // A test of a Spix action
        Action testAction = new spix.core.AbstractAction("Test") {
            
            private int count = 1;
            
            public void performAction( Spix spix ) {
                System.out.println("A test spix action.");
                put(Action.NAME, "Test " + (++count));
            }
        };
        SwingAction swingAction = new SwingAction(testAction, spix); 
        test.add(swingAction);
        swingAction.addPropertyChangeListener(testListener);
 
        // A test of a regular swing action doing similar just to compare when
        // there are problems. 
        javax.swing.Action testAction2 = new AbstractAction("Test2") {
            private int count = 1;
                      
            public void actionPerformed( ActionEvent event ) {
                System.out.println("Test action 2.");
                putValue(javax.swing.Action.NAME, "Test2 " + (++count)); 
            }
        };
        test.add(testAction2);
        testAction2.addPropertyChangeListener(testListener);*/
 /*       
        JMenu help = new JMenu("Help");
        help.add(new AbstractAction("About") {
            public void actionPerformed( ActionEvent event ) {
                JOptionPane.showMessageDialog(mainFrame, "What's it all about?");
            }
        });
        
        result.add(file);
        result.add(edit);
        result.add(test);
        result.add(help);
        
        return result;*/
    }
    
    private ActionList createMainActions() {
        ActionList main = new DefaultActionList("root");
 
        ActionList file = main.add(new DefaultActionList("File"));
        file.add(new NopAction("New"));
        file.add(new NopAction("Open"));
        file.add(new NopAction("Save"));
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
        
        ActionList test = main.add(createTestActions());
        
        ActionList help = main.add(new DefaultActionList("Help"));
        help.add(new NopAction("About") {
            public void performAction( Spix spix ) {
                // Another case where we'll cheat until we have proper
                // user request objects
                JOptionPane.showMessageDialog(mainFrame, "What's it all about?");
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
                System.out.println("A test spix action.");
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
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        //mat.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);
    }

    public static class NopAction extends spix.core.AbstractAction {
        
        public NopAction( String name ) {
            super(name);
        }
        
        public void performAction( Spix spix ) {
        }
    } 
}
