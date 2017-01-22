package spix.app.material;

import com.jme3.asset.TextureKey;
import com.jme3.material.*;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RendererException;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderNode;
import com.jme3.texture.Texture;
import spix.app.DefaultConstants;
import spix.app.utils.CloneUtils;
import spix.core.RequestCallback;
import spix.core.SelectionModel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.preview.PreviewRequest;
import spix.swing.materialEditor.utils.MaterialDefUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nehon on 24/05/2016.
 */
public class MaterialService {

    private MaterialAppState state;
    private SwingGui gui;
    private RendererExceptionHandler logHandler = new RendererExceptionHandler();

    public MaterialService(MaterialAppState state, SwingGui gui) {
        this.state = state;
        this.gui = gui;
    }

    public void requestPreview(PreviewRequest request, RequestCallback<PreviewResult> callback, RequestCallback<CompilationError> error) {
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
                boolean wire = request.getShaderType() == Shader.ShaderType.Vertex;
                m.getAdditionalRenderState().setWireframe(wire);


                m.setColor("Color", ColorRGBA.Yellow);

                try {
                    PreviewResult res = state.requestPreview(m, request.getTechniqueName(), request.getDisplayType(), request.getOutIndex());
                    gui.runOnSwing(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(res);
                        }
                    });
                } catch (RendererException e){
                    int nbNodesRendered = def.getTechniqueDefs(request.getTechniqueName()).get(0).getShaderNodes().size();
                    CompilationError ce = new CompilationError(logHandler.getBuffer(), e.getMessage(),nbNodesRendered);
                    gui.runOnSwing(new Runnable() {
                        @Override
                        public void run() {
                            error.done(ce);
                        }
                    });
                    return;
                }

            }
        });
    }

    public void requestCode(TechniqueDef def, RequestCallback<Map<String, Shader>> callback){

        //Not really needed as it should be thread safe.
        gui.runOnRender(new Runnable() {
            @Override
            public void run() {
                if(def.isUsingShaderNodes()) {
                    Map<String, Shader> shaders = state.generateCode(def);
                    gui.runOnSwing(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(shaders);
                        }
                    });
                } else {
                    Map<String, Shader> shaders = state.getCode(def);
                    gui.runOnSwing(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(shaders);
                        }
                    });
                }

            }
        });

    }

    public void requestTexturePreview(TextureKey textureKey, RequestCallback<PreviewResult> callback) {
        gui.runOnRender(new Runnable() {
            @Override
            public void run() {
                PreviewResult result = state.previewTexture(textureKey);
                gui.runOnSwing(new Runnable() {
                    @Override
                    public void run() {
                        callback.done(result);
                    }
                });

            }
        });
    }

    public void requestTexturePreview(String texturePath, RequestCallback<PreviewResult> callback) {
        gui.runOnRender(new Runnable() {
            @Override
            public void run() {
                PreviewResult result = state.previewTexture(texturePath);
                gui.runOnSwing(new Runnable() {
                    @Override
                    public void run() {
                        callback.done(result);
                    }
                });

            }
        });
    }

    public void reloadTexture(Texture texture, RequestCallback<Texture> callback) {
        TextureKey key = (TextureKey) texture.getKey();
        state.clearfromCache(key);
        Texture tex = state.loadTexture(key);

        tex.setWrap(Texture.WrapAxis.S, texture.getWrap(Texture.WrapAxis.S));
        tex.setWrap(Texture.WrapAxis.T, texture.getWrap(Texture.WrapAxis.T));
        if (tex.getType() == Texture.Type.CubeMap || tex.getType() == Texture.Type.ThreeDimensional) {
            tex.setWrap(Texture.WrapAxis.R, texture.getWrap(Texture.WrapAxis.R));
        }

        tex.setMagFilter(texture.getMagFilter());
        tex.setMinFilter(texture.getMinFilter());

        callback.done(tex);
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

    public static class CompilationError{
        private String shaderSource;
        private Map<Integer,String> errors = new HashMap<>();
        private int nbRenderedNodes;

        public CompilationError(String source, String error, int nbRenderedNodes){
            shaderSource = source;
            String[] lines = error.split("\\n");
            this.nbRenderedNodes = nbRenderedNodes;

            int index = 0;
            for (String line : lines) {
                String[] cells = line.split(":");
                if(cells.length == 3){
                    Pattern p = Pattern.compile("0\\((\\d*)\\)");
                    Matcher m = p.matcher(cells[0].trim());
                    if(m.find()) {
                        int ln = Integer.parseInt(m.group(1));
                        errors.put(ln, cells[1] + ": " + cells[2]);
                    } else {
                        errors.put(index, line);
                    }
                } else {
                    errors.put(index, line);
                }
                index++;
            }
        }

        public String getShaderSource() {
            return shaderSource;
        }

        public Map<Integer, String> getErrors() {
            return errors;
        }

        public int getNbRenderedNodes() {
            return nbRenderedNodes;
        }

        @Override
        public String toString() {
            String res = shaderSource + "\n";
            for (Integer key : errors.keySet()) {
                res += key+ " : " + errors.get(key) + "\n";
            }

            return res;

        }
    }

    public static class PreviewResult {

        public PreviewResult(ByteBuffer imageData, int size) {
            this.imageData = imageData;
            this.width = size;
            this.height = size;
            this.originalWidth = size;
            this.originalHeight = size;
        }

        public PreviewResult(ByteBuffer imageData, int width, int height, int originalWidth, int originalHeight) {
            this.imageData = imageData;
            this.width = width;
            this.height = height;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
        }

        public ByteBuffer imageData;
        public int width;
        public int height;
        public int originalWidth;
        public int originalHeight;
    }
}
