package spix.app.material;

import com.jme3.material.*;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RendererException;
import com.jme3.shader.*;
import spix.app.utils.CloneUtils;
import spix.core.RequestCallback;
import spix.swing.SwingGui;
import spix.swing.materialEditor.preview.PreviewRequest;
import spix.swing.materialEditor.utils.MaterialDefUtils;

import java.awt.image.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by Nehon on 24/05/2016.
 */
public class MaterialService {

    private MaterialAppState state;
    private SwingGui gui;


    public MaterialService(MaterialAppState state, SwingGui gui) {
        this.state = state;
        this.gui = gui;

    }


    public void requestPreview(PreviewRequest request, RequestCallback<BufferedImage> callback, RequestCallback<RendererException> error){
        gui.runOnRender(new Runnable() {
            @Override
            public void run() {

                MaterialDef def = createMaterialDef(request);
                if (def == null) {
                    //something went wrong... no preview.
                    return;
                }

                //creating a new material with the mat def.
                Material m = new Material(def);
                //if the preview is for a vertex output, we switch to wireframe mode.
                boolean wire = request.getShaderType() == Shader.ShaderType.Vertex ? true : false;
                m.getAdditionalRenderState().setWireframe(wire);


                m.setColor("Color", ColorRGBA.Yellow);

                try {
                    BufferedImage image = convert(state.requestPreview(m, request.getTechniqueName(), request.getDisplayType()));
                    gui.runOnSwing(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(image);
                        }
                    });
                } catch (RendererException e){
                    gui.runOnSwing(new Runnable() {
                        @Override
                        public void run() {
                            error.done(e);
                        }
                    });
                    return;
                }

            }
        });
    }

    /**
     * Create a custom material definition with a technique that only goes to the output that requested the preview.
     * This allow to have a preview for the different stages of the shader.
     * @param request
     * @return
     */
    private MaterialDef createMaterialDef(PreviewRequest request) {
        MaterialDef def = null;
        try {
            //Cloning the mat def again before modifying it.
            def = CloneUtils.cloneMatDef(request.getMaterialDef(),new ArrayList<TechniqueDef>());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }

        // TODO: 28/05/2016 this might be a problem, we can now have several techniques with the same name. However it should not be the case with node based techniques.
        TechniqueDef techDef = def.getTechniqueDefs(request.getTechniqueName()).get(0);
        //a list of node that will contain the techniques node but only up to the output that requested the preview.
        List<ShaderNode> newNodes = new ArrayList<>();

        //if the outputForNode is null, this means the outputPanel that requested the preview is actually an input
        //This can happen and is valid in term of shader nodes generation.
        //In this case, we only use the vertex nodes, and add a dummy node that output a white color for the fragment shader.
        // TODO: 28/05/2016 we may want something better for the vertex shader different stages.
        if(request.getOutputForNode() == null){
            for (ShaderNode node : techDef.getShaderNodes()) {
                if(node.getDefinition().getType() == Shader.ShaderType.Vertex) {
                    newNodes.add(node);
                }
            }
            newNodes.add(state.getDummySN());
        } else {
            //we gather the nodes up to the outputForNode node.
            //this is the node which the output that requested the preview belongs to.
            for (ShaderNode node : techDef.getShaderNodes()) {
                newNodes.add(node);
                if (node.getName().equals(request.getOutputForNode())){
                    if(node.getDefinition().getType() == Shader.ShaderType.Vertex){
                        newNodes.add(state.getDummySN());
                    }
                    break;
                }
            }
        }
        //setting the new shaderNodes to the technique definition
        techDef.setShaderNodes(newNodes);
        //re computing the sahder generation information
        MaterialDefUtils.computeShaderNodeGenerationInfo(techDef);
        //fixing the world and mat param g_ and m_ names.
        MaterialDefUtils.fixUniformNames(techDef.getShaderGenerationInfo());

        return def;
    }

    private BufferedImage convert(ByteBuffer cpuBuf){
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
