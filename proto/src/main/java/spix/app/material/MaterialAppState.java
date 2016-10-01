package spix.app.material;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.*;
import com.jme3.light.PointLight;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.*;
import com.jme3.shader.*;
import com.jme3.texture.*;
import com.jme3.util.*;

import java.lang.reflect.Field;
import java.nio.*;
import java.util.*;
import java.util.logging.*;

import static spix.swing.materialEditor.icons.Icons.mat;

/**
 * Created by Nehon on 24/05/2016.
 */
public class MaterialAppState extends BaseAppState {


    public static final int SIZE = 128;
    private ViewPort vp;
    private Camera cam;

    private Geometry sphere;
    private Geometry box;
    private Geometry quad;
    private Node previewNode = new Node("PreviewNode");
    private FrameBuffer offBuffer;
    private ByteBuffer cpuBuf = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
    private ShaderNode dummySN;
    private Glsl150ShaderGenerator glsl15;
    private Glsl100ShaderGenerator glsl10;
    private Texture2D[] texs;

    public enum DisplayType {

        Sphere,
        Box,
        Quad
    }

    @Override
    protected void initialize(Application app) {
        Sphere sphMesh = new Sphere(24, 24, 2.5f);
        sphMesh.setTextureMode(Sphere.TextureMode.Projected);
        Logger log = Logger.getLogger(TangentBinormalGenerator.class.getName());
        log.setLevel(Level.SEVERE);
        TangentBinormalGenerator.generate(sphMesh);
        setColorToMesh(sphMesh);
        sphere = new Geometry("previewSphere", sphMesh);
        sphere.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_X));

        Box boxMesh = new Box(1.75f, 1.75f, 1.75f);
        TangentBinormalGenerator.generate(boxMesh);
        setColorToMesh(boxMesh);
        box = new Geometry("previewBox", boxMesh);
        box.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.DEG_TO_RAD * 30, Vector3f.UNIT_X).multLocal(new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Y)));

        Quad quadMesh = new Quad(4.5f, 4.5f);
        TangentBinormalGenerator.generate(quadMesh);
        setColorToMesh(quadMesh);
        quad = new Geometry("previewQuad", quadMesh);
        quad.setLocalTranslation(new Vector3f(-2.25f, -2.25f, 0));

        cam = new Camera(SIZE,SIZE);
        cam.setFrustumPerspective(45f,1,1,100);
        cam.setLocation(new Vector3f(0, 0, 7));
        cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);

        vp = new ViewPort("Preview viewPort", cam);
        ColorRGBA color = new ColorRGBA();
        color.setAsSrgb(0.25f, 0.25f, 0.25f, 1.0f);
        vp.setBackgroundColor(color);
        vp.setClearFlags(true, true, true);

        PointLight light = new PointLight();
        light.setPosition(cam.getLocation());
        light.setColor(ColorRGBA.White);
        previewNode.addLight(light);

        // attach the scene to the viewport to be rendered
        vp.attachScene(previewNode);

        offBuffer = new FrameBuffer(SIZE, SIZE, 1);
        offBuffer.setDepthBuffer(Image.Format.Depth);
        offBuffer.setSrgb(true);
        vp.setOutputFrameBuffer(offBuffer);

        ShaderNodeDefinitionKey k = new ShaderNodeDefinitionKey("MatDefs/default/dummy.j3sn");
        List<ShaderNodeDefinition> defList =  app.getAssetManager().loadAsset(k);
        this.dummySN = new ShaderNode("Dummy", defList.get(0), null);
        ShaderNodeVariable in = new ShaderNodeVariable("vec4", "Dummy", "inColor");
        ShaderNodeVariable global = new ShaderNodeVariable("vec4", "Global", "color");
        ShaderNodeVariable out = new ShaderNodeVariable("vec4","Dummy", "outColor");

        dummySN.getInputMapping().add(new VariableMapping(in, "", global, "", null));
      //  dummySN.getOutputMapping().add(new VariableMapping(global, "", out, "", null));

        glsl15 = new Glsl150ShaderGenerator(getApplication().getAssetManager());
        glsl10 = new Glsl100ShaderGenerator(getApplication().getAssetManager());

        texs = new Texture2D[8];
        for (int i = 0; i < texs.length; i++) {
            texs[i] = new Texture2D(SIZE,SIZE, Image.Format.RGBA8);
        }

    }

    public ShaderNode getDummySN() {
        return dummySN;
    }

    @Override
    protected void cleanup(Application app) {

    }

    public ByteBuffer requestPreview(Material mat, String techniqueName, DisplayType displayType, int index) throws RendererException {
        setupScene(displayType, mat);
        RenderManager rm = getApplication().getRenderManager();
        mat.selectTechnique(techniqueName, rm);

        int nbOut = mat.getActiveTechnique().getDef().getShaderGenerationInfo().getFragmentGlobals().size();
        offBuffer.resetObject();
        offBuffer.setMultiTarget(nbOut > 1);
        offBuffer.clearColorTargets();
        for (int i = 0; i < nbOut; i++) {
            //textures here are useless, I never use them because I read directly from the framebuffer.
            //However they are needed because you can't add multiple render target in the frame buffer class otherwise.
            // TODO: 04/06/2016 add support in the FrameBuffer class for adding more render buffer with out a texture.
            offBuffer.addColorTexture(texs[i]);
        }

        Renderer r = rm.getRenderer();
        r.setFrameBuffer(offBuffer);
        r.clearBuffers(true, true, true);
        rm.renderViewPortRaw(vp);

        try {
            // this is a shameful hack.
            // readFrameBufferWithFormat only read from the first render buffer.
            // to read the relevant buffer, I access the list with reflection and remove the first buffers until the one I need is the first one.
            // TODO: 04/06/2016 Add support in the renderer for reading all the buffers of a framebuffer or/and a specified one.
            Field colorBufField = offBuffer.getClass().getDeclaredField("colorBufs");
            colorBufField.setAccessible(true);
            List<FrameBuffer.RenderBuffer> buffers = (List<FrameBuffer.RenderBuffer>)colorBufField.get(offBuffer);
            for (int i = 0; i < index; i++) {
                FrameBuffer.RenderBuffer rb = buffers.remove(0);
                rb.resetObject();
            }
            r.readFrameBufferWithFormat(offBuffer, cpuBuf, Image.Format.BGRA8);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return cpuBuf;
    }

    public Map<String, Shader> generateCode(TechniqueDef def){
        Map<String, Shader> shaders = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        sb.append(def.getShaderPrologue());

        glsl15.initialize(def);
        glsl10.initialize(def);

        shaders.put("GLSL150",glsl15.generateShader(sb.toString()));
        shaders.put("GLSL100",glsl10.generateShader(sb.toString()));

        return shaders;
    }

    public Map<String, Shader> getCode(TechniqueDef def){

        StringBuilder sb = new StringBuilder();
        sb.append(def.getShaderPrologue());
        Application app = getApplication();
        try {
            Shader s = def.getShader(app.getAssetManager(), app.getRenderer().getCaps(), new DefineList(0));
            Map<String, Shader> shaders = new HashMap<>();
            shaders.put(def.getShaderProgramLanguages().get(0), s);
            return shaders;
        }catch (AssetNotFoundException e){
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Could not load shader source for technique def {0}", def.getName());
            e.printStackTrace();
            return null;
        }
    }

    private void setupScene(DisplayType displayType, Material mat) {

        previewNode.detachAllChildren();
        switch (displayType) {
            case Box:
                previewNode.attachChild(box);
                break;
            case Quad:
                previewNode.attachChild(quad);
                break;
            case Sphere:
                previewNode.attachChild(sphere);
                break;
        }
        previewNode.setMaterial(mat);
        previewNode.updateGeometricState();
    }

    /**
     * sets an arbitrary color to the mesh so that if the user uses vertex color the buffer is not random.
     * @param mesh
     */
    private void setColorToMesh(Mesh mesh){
        ColorRGBA[] colors = new ColorRGBA[mesh.getVertexCount()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = ColorRGBA.randomColor();
        }
        FloatBuffer b = BufferUtils.createFloatBuffer(colors);
        mesh.setBuffer(VertexBuffer.Type.Color, 4, b);
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
