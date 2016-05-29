/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor.preview;

import com.jme3.material.*;
import spix.app.material.*;
import spix.core.RequestCallback;
import spix.swing.SwingGui;
import spix.swing.materialEditor.errorlog.*;
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
    private MaterialDef matDef;
    private String techniqueName;
    private ErrorLog errorLog;

    public MaterialPreviewRenderer(SwingGui gui, ErrorLog errorLog) {
        this.gui = gui;
        this.errorLog = errorLog;
    }

    public void setMatDef(MaterialDef matDef) {
        this.matDef = matDef;
    }

    public void setTechniqueName(String techniqueName) {
        this.techniqueName = techniqueName;
    }

    public void showMaterial(PreviewRequest request) {
        request.setMaterialDef(matDef);
        request.setTechniqueName(techniqueName);
        gui.getSpix().getService(MaterialService.class).requestPreview(request,
        new RequestCallback<BufferedImage>() {
            @Override
            public void done(BufferedImage result) {
                request.getTargetLabel().setIcon(new ImageIcon(result));
                errorLog.noError();
            }
        }, new RequestCallback<MaterialService.CompilationError>() {
            @Override
            public void done(MaterialService.CompilationError result) {
                if(matDef.getTechniqueDefs(techniqueName).get(0).getShaderNodes().size() == result.getNbNodes()) {
                    errorLog.error(result);
                }
                request.getTargetLabel().setIcon(Icons.error);
            }
        });

    }

    private static int lastErrorHash = 0;

    private void smartLog(String expText, String message) {
        int hash = message.hashCode();
        if (hash != lastErrorHash) {
            Logger.getLogger(MaterialPreviewRenderer.class.getName()).log(Level.SEVERE, expText, message);
            lastErrorHash = hash;
        }
    }

}
