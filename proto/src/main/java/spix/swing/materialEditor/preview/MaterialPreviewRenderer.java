/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor.preview;

import com.jme3.material.*;
import com.jme3.shader.ShaderNodeVariable;
import spix.app.material.MaterialService;
import spix.core.RequestCallback;
import spix.swing.SwingGui;
import spix.swing.materialEditor.panels.ErrorLog;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.nodes.OutPanel;
import spix.swing.materialEditor.utils.MaterialDefUtils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * @author Nehon
 */
public class MaterialPreviewRenderer {

    private List<MaterialService.CompilationError> errors = new ArrayList<>();
    private int nbRequestsDone = 0;

    public void batchRequests(SwingGui gui, ErrorLog errorLog, final List<OutPanel> outs, MaterialDef matDef, String techniqueName) {

        TechniqueDef techDef = matDef.getTechniqueDefs(techniqueName).get(0);
        MaterialDefUtils.computeShaderNodeGenerationInfo(techDef);

        for (OutPanel out : outs) {

            PreviewRequest request = out.makePreviewRequest();
            request.setMaterialDef(matDef);
            request.setTechniqueName(techniqueName);
            for (int i = 0; i < techDef.getShaderGenerationInfo().getFragmentGlobals().size(); i++) {
                ShaderNodeVariable var = techDef.getShaderGenerationInfo().getFragmentGlobals().get(i);
                if(var.getName().equals(out.getVarName())){
                    request.setOutIndex(i);
                }
            }
            gui.getSpix().getService(MaterialService.class).requestPreview(request,
                    new RequestCallback<BufferedImage>() {
                        @Override
                        public void done(BufferedImage result) {
                            request.getTargetLabel().setIcon(new ImageIcon(result));
                            errorLog.noError();
                            closeRequest(outs, errorLog);
                        }
                    }, new RequestCallback<MaterialService.CompilationError>() {
                        @Override
                        public void done(MaterialService.CompilationError result) {
                            errors.add(result);
                            request.getTargetLabel().setIcon(Icons.error);
                            closeRequest(outs, errorLog);
                        }
                    });
        }

    }

    private void closeRequest(final List<OutPanel> outs, ErrorLog errorLog) {
        nbRequestsDone++;

        if (nbRequestsDone == outs.size()) {
            //we're done lets handle errors if any
            if(!errors.isEmpty()) {
                //We are looking for the error that occured on the shader woth the most nodes as it will have the most information
                MaterialService.CompilationError errorToDisplay = null;
                for (MaterialService.CompilationError error : errors) {
                    if (errorToDisplay == null || error.getNbRenderedNodes() > errorToDisplay.getNbRenderedNodes()) {
                        errorToDisplay = error;
                    }
                }
                errorLog.error(errorToDisplay);
            }

        }
    }


}
