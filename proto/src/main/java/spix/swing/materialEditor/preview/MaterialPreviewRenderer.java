/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor.preview;

import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.shader.ShaderNodeVariable;
import spix.app.material.MaterialService;
import spix.core.RequestCallback;
import spix.swing.SwingGui;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.nodes.OutPanel;
import spix.swing.materialEditor.panels.ErrorLog;
import spix.swing.materialEditor.utils.MaterialDefUtils;

import javax.swing.*;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
                    new RequestCallback<ByteBuffer>() {
                        @Override
                        public void done(ByteBuffer result) {
                            out.updatePreview(new ImageIcon(MaterialPreviewRenderer.convert(result)));
                            errorLog.noError();
                            closeRequest(outs, errorLog);
                        }
                    }, new RequestCallback<MaterialService.CompilationError>() {
                        @Override
                        public void done(MaterialService.CompilationError result) {
                            errors.add(result);
                            out.updatePreview(Icons.error);
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

    public static BufferedImage convert(ByteBuffer cpuBuf) {
        int size = 128 * 128 * 4;
        // copy native memory to java memory
        byte[] cpuArray = new byte[size];
        cpuBuf.clear();
        cpuBuf.get(cpuArray);
        cpuBuf.clear();

        // flip the components the way AWT likes them
        for (int i = 0; i < size; i += 4) {
            byte b = cpuArray[i + 0];
            byte g = cpuArray[i + 1];
            byte r = cpuArray[i + 2];
            byte a = cpuArray[i + 3];

            cpuArray[i + 0] = a;
            cpuArray[i + 1] = b;
            cpuArray[i + 2] = g;
            cpuArray[i + 3] = r;
        }

        BufferedImage image = new BufferedImage(128, 128,
                BufferedImage.TYPE_4BYTE_ABGR);
        WritableRaster wr = image.getRaster();
        DataBufferByte db = (DataBufferByte) wr.getDataBuffer();
        System.arraycopy(cpuArray, 0, db.getData(), 0, cpuArray.length);

        return image;
    }

}
