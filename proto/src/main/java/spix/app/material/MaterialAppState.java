package spix.app.material;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.ShaderNodeDefinitionKey;
import com.jme3.light.PointLight;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.*;
import com.jme3.shader.*;
import com.jme3.texture.*;
import com.jme3.util.*;

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
    private final static boolean debug = false;
    private Glsl150ShaderGenerator glsl15;
    private Glsl100ShaderGenerator glsl10;

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
        offBuffer.setColorBuffer(Image.Format.RGBA8);
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
    }

    public ShaderNode getDummySN() {
        return dummySN;
    }

    @Override
    protected void cleanup(Application app) {

    }

    public ByteBuffer requestPreview(Material mat, String techniqueName, DisplayType displayType) throws RendererException {
        setupScene(displayType, mat);
        RenderManager rm = getApplication().getRenderManager();
        mat.selectTechnique(techniqueName, rm);

        Renderer r = rm.getRenderer();
        r.setFrameBuffer(offBuffer);
        r.clearBuffers(true, true, true);
        rm.renderViewPortRaw(vp);

        displayCode(mat.getActiveTechnique().getDef());

        r.readFrameBufferWithFormat(offBuffer, cpuBuf, Image.Format.BGRA8);

        return cpuBuf;
    }

    private void displayCode(TechniqueDef def) {
        if(debug) {
            glsl15.initialize(def);
            // that how you need to generate the shader
            StringBuilder sb = new StringBuilder();
            sb.append(def.getShaderPrologue());

            Shader s = glsl15.generateShader(sb.toString());
            System.err.println("########################################################################");
            System.err.println(def.getName());
            System.err.println("########################################################################");
            //Then it's like before.
            for (Shader.ShaderSource shaderSource : s.getSources()) {
                System.err.println("##########   " + shaderSource.getType() + "   ##########");
                System.err.println(shaderSource.getSource());
            }
        }
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

    private void setupScene(DisplayType displayType, Material mat) {

//        mat= new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//        mat.setColor("Color", ColorRGBA.White);
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
