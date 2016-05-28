/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor.preview;

import com.jme3.material.*;
import com.jme3.renderer.RendererException;
import spix.app.material.*;
import spix.core.RequestCallback;
import spix.swing.SwingGui;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.logging.*;

/**
 *
 * @author Nehon
 */
public class MaterialPreviewRenderer {

    private SwingGui gui;
    private MaterialDef matdDef;
    private String techniqueName;

    public MaterialPreviewRenderer(SwingGui gui) {
        this.gui = gui;
    }

    public void setMatdDef(MaterialDef matdDef) {
        this.matdDef = matdDef;
    }

    public void setTechniqueName(String techniqueName) {
        this.techniqueName = techniqueName;
    }

    public void showMaterial(PreviewRequest request) {
        request.setMaterialDef(matdDef);
        request.setTechniqueName(techniqueName);
        gui.getSpix().getService(MaterialService.class).requestPreview(request,
        new RequestCallback<BufferedImage>() {
            @Override
            public void done(BufferedImage result) {
                request.getTargetLabel().setIcon(new ImageIcon(result));
            }
        }, new RequestCallback<RendererException>() {
            @Override
            public void done(RendererException result) {
                result.printStackTrace();
                request.getTargetLabel().setIcon(Icons.error);
            }
        });

//       gui.runOnRender(new Runnable() {
//           @Override
//           public void run() {
//                if (techniqueName != null) {
//
//                    try {
//                        m.selectTechnique(techniqueName, SceneApplication.getApplication().getRenderManager());
//                    } catch (Exception e) {
//                        //
//                    }
//                }
//                final Material mat = reloadMaterial(m);
//                if (mat != null) {
//                    java.awt.EventQueue.invokeLater(new Runnable() {
//                        public void run() {
//                            currentMaterial = mat;
//                            currentGeom.setMaterial(mat);
//                            try {
//                                if (currentGeom.getMaterial() != null) {
//                                    PreviewRequest request = new PreviewRequest(MaterialPreviewRenderer.this, currentGeom, label.getWidth(), label.getHeight());
//                                    request.getCameraRequest().setLocation(new Vector3f(0, 0, 7));
//                                    request.getCameraRequest().setLookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
//                                    SceneApplication.getApplication().createPreview(request);
//                                }
//                            } catch (Exception e) {
//                                java.awt.EventQueue.invokeLater(new Runnable() {
//                                    public void run() {
//                                        label.setIcon(Icons.error);
//                                    }
//                                });
//                                smartLog("Error rendering material{0}", e.getMessage());
//                            }
//                        }
//                    });
//
//                }
//            }
//        });
    }

    private static int lastErrorHash = 0;

    private void smartLog(String expText, String message) {
        int hash = message.hashCode();
        if (hash != lastErrorHash) {
            Logger.getLogger(MaterialPreviewRenderer.class.getName()).log(Level.SEVERE, expText, message);
            lastErrorHash = hash;
        }
    }

//    public Material reloadMaterial(Material mat) {
//
//        ((ProjectAssetManager) mat.getMaterialDef().getAssetManager()).clearCache();
//
//        //creating a dummy mat with the mat def of the mat to reload
//        Material dummy = new Material(mat.getMaterialDef());
//        try {
//            for (MatParam matParam : mat.getParams()) {
//                dummy.setParam(matParam.getName(), matParam.getVarType(), matParam.getValue());
//            }
//            if (mat.getActiveTechnique() != null) {
//                dummy.selectTechnique(mat.getActiveTechnique().getDef().getName(), SceneApplication.getApplication().getRenderManager());
//            }
//            dummy.getAdditionalRenderState().set(mat.getAdditionalRenderState());
//
//            //creating a dummy geom and assigning the dummy material to it
//            Geometry dummyGeom = new Geometry("dummyGeom", new Box(1f, 1f, 1f));
//            dummyGeom.setMaterial(dummy);
//
//            //preloading the dummyGeom, this call will compile the shader again
//            SceneApplication.getApplication().getRenderManager().preloadScene(dummyGeom);
//        } catch (RendererException e) {
//            //compilation error, the shader code will be output to the console
//            //the following code will output the error
//            //System.err.println(e.getMessage());
//            //Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.SEVERE, e.getMessage());
//            smartLog("{0}", e.getMessage());
//
//            java.awt.EventQueue.invokeLater(new Runnable() {
//                public void run() {
//                    label.setIcon(Icons.error);
//                }
//            });
//            return null;
//        } catch (NullPointerException npe) {
//            //utterly bad, but for some reason I get random NPE here and can't figure out why so to avoid bigger issues, I just catch it.
//            //the printStackTrace is intended, it will show up in debug mode, but won't be displayed in standzrd mode
//            npe.printStackTrace();
//            return null;
//        }
//
//        //Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.INFO, "Material succesfully reloaded");
//        //System.out.println("Material succesfully reloaded");
//        return dummy;
//    }

//    public void switchDisplay(DisplayType type) {
//        switch (type) {
//            case Box:
//                currentGeom = box;
//                break;
//            case Sphere:
//                currentGeom = sphere;
//                break;
//            case Quad:
//                currentGeom = quad;
//                break;
//        }
//       // showMaterial(currentMaterial);
//    }

}
