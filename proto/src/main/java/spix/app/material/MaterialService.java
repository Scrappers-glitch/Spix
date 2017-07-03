package spix.app.material;

import com.jme3.asset.TextureKey;
import com.jme3.material.*;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RendererException;
import com.jme3.scene.*;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderNode;
import com.jme3.texture.Texture;
import spix.app.FileIoAppState;
import spix.app.utils.CloneUtils;
import spix.app.utils.MaterialUtils;
import spix.core.RequestCallback;
import spix.props.BeanProperty;
import spix.props.Property;
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

    private MaterialAppState materialState;
    private FileIoAppState ioState;
    private SwingGui gui;
    private RendererExceptionHandler logHandler = new RendererExceptionHandler();
    private Map<String, List<Material>> materialSyncMap = new HashMap<>();

    private ReplaceMatDefVisitor replaceMatDefVisitor = new ReplaceMatDefVisitor();

    public MaterialService(MaterialAppState materialState, FileIoAppState ioState, SwingGui gui) {
        this.materialState = materialState;
        this.ioState = ioState;
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

                for (MatParam matParam : def.getMaterialParams()) {
                    MatParam param = request.getMatParams().get(matParam.getName());
                    if (param != null) {
                        m.setParam(param.getName(), param.getVarType(), param.getValue());
                    }
                }

                try {
                    PreviewResult res = materialState.requestPreview(m, request.getTechniqueName(), request.getDisplayType(), request.getOutIndex());
                    gui.runOnSwing(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(res);
                        }
                    });
                } catch (RuntimeException e) {
                    int nbNodesRendered = def.getTechniqueDefs(request.getTechniqueName()).get(0).getShaderNodes().size();

                    CompilationError ce = new CompilationError(logHandler.getBuffer(), e, nbNodesRendered);
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

    public void requestCode(TechniqueDef def, MaterialDef matDef, RequestCallback<Map<String, Shader>> callback) {
        //re computing the shader generation information
        MaterialDefUtils.computeShaderNodeGenerationInfo(def, matDef);

        //Not really needed as it should be thread safe.
        gui.runOnRender(new Runnable() {
            @Override
            public void run() {
                if(def.isUsingShaderNodes()) {
                    Map<String, Shader> shaders = materialState.generateCode(def);
                    gui.runOnSwing(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(shaders);
                        }
                    });
                } else {
                    Map<String, Shader> shaders = materialState.getCode(def);
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
                ioState.loadTexture(textureKey, new RequestCallback<Texture>() {
                    @Override
                    public void done(Texture texture) {
                        PreviewResult result = materialState.previewTexture(texture);
                        gui.runOnSwing(new Runnable() {
                            @Override
                            public void run() {
                                callback.done(result);
                            }
                        });
                    }
                });
            }
        });
    }

    public void requestTexturePreview(String texturePath, RequestCallback<PreviewResult> callback) {
        gui.runOnRender(new Runnable() {
            @Override
            public void run() {
                ioState.loadTexture(texturePath, new RequestCallback<Texture>() {
                    @Override
                    public void done(Texture texture) {

                        PreviewResult result = materialState.previewTexture(texture);
                        gui.runOnSwing(new Runnable() {
                            @Override
                            public void run() {
                                callback.done(result);
                            }
                        });
                    }
                });
            }
        });
    }

    public void reloadTexture(Texture texture, RequestCallback<Texture> callback) {
        TextureKey key = (TextureKey) texture.getKey();
        materialState.clearfromCache(key);
        ioState.loadTexture(key, new RequestCallback<Texture>() {
            @Override
            public void done(Texture tex) {
                tex.setWrap(Texture.WrapAxis.S, texture.getWrap(Texture.WrapAxis.S));
                tex.setWrap(Texture.WrapAxis.T, texture.getWrap(Texture.WrapAxis.T));
                if (tex.getType() == Texture.Type.CubeMap || tex.getType() == Texture.Type.ThreeDimensional) {
                    tex.setWrap(Texture.WrapAxis.R, texture.getWrap(Texture.WrapAxis.R));
                }

                tex.setMagFilter(texture.getMagFilter());
                tex.setMinFilter(texture.getMinFilter());

                callback.done(tex);
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

        //if the nodeGraph is empty, this means the outputPanel that requested the preview is actually an input
        //This can happen and is valid in term of shader nodes generation.
        //In this case, we only use the vertex nodes, and add a dummy node that output a white color for the fragment shader.
        // TODO: 28/05/2016 we may want something better for the vertex shader different stages.
        if (request.getNodeGraph().isEmpty()) {
            for (ShaderNode node : techDef.getShaderNodes()) {
                if(node.getDefinition().getType() == Shader.ShaderType.Vertex) {
                    newNodes.add(node);
                }
            }
            newNodes.add(materialState.getDummySN());
        } else {
            boolean hasFragment = false;
            //we only keep the nodes in the nodeGraph to make a specific matDef
            for (ShaderNode node : techDef.getShaderNodes()) {
                if (request.getNodeGraph().contains(node.getName())) {
                    newNodes.add(node);
                    if (node.getDefinition().getType() == Shader.ShaderType.Fragment) {
                        hasFragment = true;
                    }
                }
            }
            //there is no fragment shader, adding the dummy one
            if (!hasFragment) {
                newNodes.add(materialState.getDummySN());
            }
        }
        //setting the new shaderNodes to the technique definition
        techDef.setShaderNodes(newNodes);
        //re computing the shader generation information
        MaterialDefUtils.computeShaderNodeGenerationInfo(techDef, def);
        //fixing the world and mat param g_ and m_ names.
        //MaterialDefUtils.fixUniformNames(techDef.getShaderGenerationInfo());

        return def;
    }

    public void replaceMatDef(Spatial spatial, MaterialDef def) {
        replaceMatDefVisitor.init(def);
        spatial.depthFirstTraversal(replaceMatDefVisitor);
    }

    public static class CompilationError{
        private String shaderSource;
        private Map<Integer,String> errors = new HashMap<>();
        private int nbRenderedNodes;
        private Exception exception;

        public CompilationError(String source, Exception exception, int nbRenderedNodes) {
            shaderSource = source;
            this.exception = exception;
            String error = exception.getMessage();
            if (error == null) {
                error = "Failed to generate shader code";
            }
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

        public Exception getException() {
            return exception;
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


    public void gatherMaterialsForSync(Spatial scene) {
        SceneGraphVisitor sgv = new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof Geometry) {
                    Geometry g = (Geometry) spatial;
                    Material m = g.getMaterial();
                    if (m.getKey() != null) {
                        registerMaterialForSync(m.getKey().getName(), m);
                    }
                }
            }
        };
        scene.depthFirstTraversal(sgv);
    }

    public void registerMaterialForSync(String path, Material mat) {
        List<Material> mats = materialSyncMap.get(path);
        if (mats == null) {
            mats = new ArrayList<>();
            materialSyncMap.put(path, mats);
        }
        mats.add(mat);
    }

    public void unregisterMaterialForSync(String path, Material mat) {
        List<Material> mats = materialSyncMap.get(path);
        if (mats == null) {
            return;
        }
        mats.remove(mat);
    }

    public Material getMaterialForPath(String path) {
        List<Material> mats = materialSyncMap.get(path);
        if (mats == null || mats.isEmpty()) {
            return null;
        }
        return mats.get(0);
    }

    public void syncMatsForPath(String path, Material mat, Property prop) {
        if (prop == null) {
            return;
        }
        List<Material> mats = materialSyncMap.get(path);
        if (mats == null) {
            return;
        }
        for (Material material : mats) {
            if (material == mat) {
                continue;
            }
            Property p = null;
            if (prop instanceof BeanProperty) {
                Object obj = material;
                if (((BeanProperty) prop).getObject() instanceof RenderState) {
                    obj = material.getAdditionalRenderState();
                }
                String id = prop.getId();
                if (id.equals("materialName")) {
                    id = "name";
                }
                p = BeanProperty.create(obj, id, prop.getName(), false, null);
            } else if (prop instanceof MatParamProperty) {
                MatParamProperty matProp = (MatParamProperty) prop;
                p = new MatParamProperty(matProp.getId(), matProp.getVarType(), material);
                if (prop.getValue() instanceof Texture) {
                    Texture t = (Texture) prop.getValue();
                    if (t.getImage() == null) {
                        p.setValue(null);
                        return;
                    }
                }
            } else if (prop instanceof PolyOffsetProperty) {
                p = new PolyOffsetProperty(material.getAdditionalRenderState(), prop.getName());
            }
            p.setValue(prop.getValue());
        }
    }

    private class ReplaceMatDefVisitor extends SceneGraphVisitorAdapter {
        MaterialDef def;

        public void init(MaterialDef def) {
            this.def = def;
        }

        @Override
        public void visit(Geometry geom) {
            if (!(geom.getMaterial().getMaterialDef().getAssetName().equals(def.getAssetName()))) {
                return;
            }
            Material oldMat = geom.getMaterial();
            Material newMat = new Material(def);
            MaterialUtils.copyParams(oldMat, newMat);
            geom.setMaterial(newMat);
        }
    }
}
