package spix.app.painting;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.*;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.*;
import com.simsilica.lemur.input.*;
import spix.app.*;
import spix.app.utils.CursorAdapter;
import spix.core.SelectionModel;
import spix.core.Spix;
import spix.undo.UndoManager;
import spix.undo.edit.VertexPaintEdit;

import java.nio.FloatBuffer;
import java.util.*;

public class VertexPaintAppState extends BaseAppState {

    public static final String TOOLS_VERTEXPAINTING_SIZE = "tools.vertexpainting.size";
    public static final String TOOLS_VERTEXPAINTING_HARDNESS = "tools.vertexpainting.hardness";
    public static final String TOOLS_VERTEXPAINTING_LAYER = "tools.vertexpainting.layer";
    private Camera cam;
    private Geometry shootable;
    private Spatial brush;
    private float brushRadius = 0.7f;
    private float brushStartSize;
    private float hardness = 0.2f;
    private int layer = 0;
    private BoundingSphere bSphere = new BoundingSphere(1, Vector3f.ZERO);
    private Triangle tri = new Triangle();
    private float[] colorBuffer;
    private boolean painting = false;
    private float direction = 1;
    CollisionResults results = new CollisionResults();
    private Vector3f prevPos = new Vector3f();
    private PainterCursorListener cursorListener = new PainterCursorListener();
    private ResizeCursorListener resizeListener = new ResizeCursorListener();
    private VertexPaintEdit lastEdit;

    private static final String GROUP = "VertexPaintingBrush";
    private static final String GROUP_BRUSH_RESIZE = "VertexPaintingBrushResize";
    private static final FunctionId F_BRUSH_SIZE_START = new FunctionId(GROUP, "BrushSizeStart");
    private static final FunctionId F_BRUSH_SIZE_END = new FunctionId(GROUP_BRUSH_RESIZE, "BrushSizeEnd");
    private static final FunctionId F_BRUSH_SIZE_CANCEL = new FunctionId(GROUP_BRUSH_RESIZE, "BrushSizeCancel");
    private static final FunctionId F_LAYER_NEXT = new FunctionId(GROUP, "LayerNext");
    private static final FunctionId F_LAYER_PREV = new FunctionId(GROUP, "LayerPrev");


    public VertexPaintAppState(boolean enabled) {
        this.setEnabled(enabled);
    }

    @Override
    protected void initialize(Application app) {
        cam = app.getCamera();
        initBrush(app.getAssetManager());
        getSpix().getBlackboard().bind(TOOLS_VERTEXPAINTING_HARDNESS, this, "hardness");
        getSpix().getBlackboard().bind(TOOLS_VERTEXPAINTING_SIZE, this, "brushRadius");
        getSpix().getBlackboard().bind(TOOLS_VERTEXPAINTING_LAYER, this, "layer");

        GuiGlobals globals = GuiGlobals.getInstance();
        InputMapper im = globals.getInputMapper();
        im.map(F_BRUSH_SIZE_START, KeyInput.KEY_B);
        im.map(F_BRUSH_SIZE_END, Button.MOUSE_BUTTON1);
        im.map(F_BRUSH_SIZE_CANCEL, Button.MOUSE_BUTTON2);
        im.map(F_LAYER_NEXT, KeyInput.KEY_RIGHT);
        im.map(F_LAYER_PREV, KeyInput.KEY_LEFT);

        im.deactivateGroup(GROUP);
        im.deactivateGroup(GROUP_BRUSH_RESIZE);

        brushStartSize = brushRadius;

        im.addStateListener(new StateFunctionListener() {
            @Override
            public void valueChanged(FunctionId func, InputState value, double tpf) {
                if (value != InputState.Off) {
                    return;
                }
                if (func == F_BRUSH_SIZE_START) {
                    CursorEventControl.removeListenersFromSpatial(shootable, cursorListener);
                    CursorEventControl.addListenersToSpatial(shootable, resizeListener);
                    im.activateGroup(GROUP_BRUSH_RESIZE);
                }
                if (func == F_BRUSH_SIZE_END || func == F_BRUSH_SIZE_CANCEL) {
                    saveBrushRadius();
                    im.deactivateGroup(GROUP_BRUSH_RESIZE);
                    CursorEventControl.removeListenersFromSpatial(shootable, resizeListener);
                    CursorEventControl.addListenersToSpatial(shootable, cursorListener);
                }
                if (func == F_BRUSH_SIZE_CANCEL) {
                    brushRadius = brushStartSize;
                }
                if (func == F_LAYER_NEXT) {
                    getSpix().getBlackboard().set(TOOLS_VERTEXPAINTING_LAYER, (layer + 1) % 4);
                }
                if (func == F_LAYER_PREV) {
                    getSpix().getBlackboard().set(TOOLS_VERTEXPAINTING_LAYER, ((layer - 1) % 4 + 4) % 4);
                }
            }
        }, F_BRUSH_SIZE_END, F_BRUSH_SIZE_START, F_BRUSH_SIZE_CANCEL, F_LAYER_NEXT, F_LAYER_PREV);

    }

    protected Node getRoot() {
        return getState(DecoratorViewPortState.class).getRoot();
    }

    private VertexBuffer initColorBuffer(Mesh mesh) {

        int size = mesh.getVertexCount() * 4;
        if (colorBuffer == null || colorBuffer.length != size) {
            colorBuffer = new float[size];
        }
        VertexBuffer vb = mesh.getBuffer(VertexBuffer.Type.Color);
        if (vb != null) {
            FloatBuffer b = ((FloatBuffer) vb.getData());
            b.rewind();
            b.get(colorBuffer);
            return vb;
        }

        // no color buffer lets crate is
        for (int i = 0; i < colorBuffer.length; i++) {
            colorBuffer[i] = 0;
        }
        FloatBuffer b = BufferUtils.createFloatBuffer(colorBuffer);
        mesh.setBuffer(VertexBuffer.Type.Color, 4, b);
        vb = mesh.getBuffer(VertexBuffer.Type.Color);
        return vb;
    }

    protected void initBrush(AssetManager assetManager) {
        brush = assetManager.loadModel("Models/brush.j3o");
    }

    @Override
    protected void cleanup(Application app) {

    }

    private Spix getSpix() {
        return getState(SpixState.class).getSpix();
    }

    public float getBrushRadius() {
        return brushRadius;
    }

    public void setBrushRadius(float brushRadius) {
        BoundingVolume bv = shootable.getWorldBound();
        float factor = 1;
        if (bv instanceof BoundingBox) {
            BoundingBox bb = (BoundingBox) bv;
            factor = Math.max(bb.getXExtent(), bb.getZExtent());
        } else if (bv instanceof BoundingSphere) {
            factor = ((BoundingSphere) bv).getRadius();
        }

        this.brushRadius = brushRadius * factor * 0.5f;
    }


    public void saveBrushRadius() {
        BoundingVolume bv = shootable.getWorldBound();
        float factor = 1;
        if (bv instanceof BoundingBox) {
            BoundingBox bb = (BoundingBox) bv;
            factor = Math.max(bb.getXExtent(), bb.getZExtent());
        } else if (bv instanceof BoundingSphere) {
            factor = ((BoundingSphere) bv).getRadius();
        }

        getSpix().getBlackboard().set(TOOLS_VERTEXPAINTING_SIZE, brushRadius / factor / 0.5f);
    }

    public float getHardness() {
        return hardness;
    }

    public void setHardness(float hardness) {
        this.hardness = hardness;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled()) {
            return;
        }
        brush.setLocalScale(brushRadius);

    }

    @Override
    protected void onEnable() {
        SelectionModel selection = getSpix().getBlackboard().get(DefaultConstants.SELECTION_PROPERTY, SelectionModel.class);
        if (!(selection.getSingleSelection() instanceof Geometry)) {
            this.setEnabled(false);
            return;
        }
        shootable = (Geometry) selection.getSingleSelection();
        initColorBuffer(shootable.getMesh());
        getState(SelectionAppState.class).setEnabled(false);
        getState(SelectionHighlightState.class).setEnabled(false);
        getState(NodeWidgetState.class).setEnabled(false);
        getState(RotationWidgetState.class).setEnabled(false);
        getState(ScaleWidgetState.class).setEnabled(false);
        getState(TranslationWidgetState.class).setEnabled(false);

        CursorEventControl.addListenersToSpatial(shootable, cursorListener);

        getSpix().getBlackboard().set("tools.active", "VertexPainting");
        Float radius = getSpix().getBlackboard().get("tools.vertexpainting.size", Float.class);
        if (radius != null) {
            setBrushRadius(radius);
        }
        Float hard = getSpix().getBlackboard().get("tools.vertexpainting.hardness", Float.class);
        if (hard != null) {
            setHardness(hard);
        }

        GuiGlobals globals = GuiGlobals.getInstance();
        InputMapper im = globals.getInputMapper();
        im.activateGroup(GROUP);
        im.deactivateGroup(GROUP_BRUSH_RESIZE);

    }

    @Override
    protected void onDisable() {
        getState(SelectionAppState.class).setEnabled(true);
        getState(NodeWidgetState.class).setEnabled(true);
        getState(SelectionHighlightState.class).setEnabled(true);
        CursorEventControl.removeListenersFromSpatial(shootable, cursorListener);
        shootable.removeControl(CursorEventControl.class);
        getSpix().getBlackboard().set("tools.active", null);
        brush.removeFromParent();
        GuiGlobals globals = GuiGlobals.getInstance();
        InputMapper im = globals.getInputMapper();
        im.deactivateGroup(GROUP);
        im.deactivateGroup(GROUP_BRUSH_RESIZE);
    }

    private void paint() {

        if (brush.getParent() == null) {
            return;
        }

        Vector3f contactPoint = brush.getWorldTranslation();

        bSphere.setCenter(contactPoint);
        bSphere.setRadius(brushRadius);
        results.clear();
        shootable.collideWith(bSphere, results);

        int[] idx = new int[3];
        List<Integer> doneIdx = new ArrayList<>();

        for (CollisionResult result : results) {
            Mesh mesh = result.getGeometry().getMesh();
            mesh.getTriangle(result.getTriangleIndex(), tri);
            int closestIdx = -1;
            float dist = Float.POSITIVE_INFINITY;
            for (int i = 0; i < 3; i++) {
                float ds = tri.get(i).distanceSquared(result.getContactPoint());
                if (ds < dist) {
                    closestIdx = i;
                }
            }
            mesh.getTriangle(result.getTriangleIndex(), idx);
            int vertIndex = idx[closestIdx];
            if (doneIdx.contains(vertIndex)) {
                continue;
            }
            doneIdx.add(vertIndex);
            Vector3f pos = tri.get(closestIdx);

            vertIndex *= 4;
            vertIndex += layer;
            float d = contactPoint.distance(pos);

            float att = FastMath.clamp(1.0f - d * d / (brushRadius * brushRadius), 0.0f, 1.0f);
            float h = hardness * hardness;
            att *= att;
            float oldVal = colorBuffer[vertIndex];
            if (direction > 0) {
                colorBuffer[vertIndex] = FastMath.clamp(colorBuffer[vertIndex] + att * h, 0, 1);
            } else {
                if (colorBuffer[vertIndex] < att * 0.003f) {
                    colorBuffer[vertIndex] = FastMath.clamp(colorBuffer[vertIndex] - att, 0, 1);
                } else {
                    colorBuffer[vertIndex] = FastMath.clamp(colorBuffer[vertIndex] - att * hardness * colorBuffer[vertIndex], 0, 1);
                }
            }
            if (oldVal != colorBuffer[vertIndex]) {
                lastEdit.addEntry(vertIndex, oldVal, colorBuffer[vertIndex]);
            }
        }

        writeToBuffer(colorBuffer, shootable);
    }

    private void writeToBuffer(float[] buffer, Geometry geom) {
        VertexBuffer vb = geom.getMesh().getBuffer(VertexBuffer.Type.Color);
        FloatBuffer color = (FloatBuffer) vb.getData();
        color.rewind();
        color.put(buffer);
        vb.updateData(color);
    }

    public float[] startBulkPaintSession(Geometry geom) {
        if (geom == shootable) {
            return colorBuffer;
        }
        int size = geom.getMesh().getVertexCount() * 4;
        float[] buff = new float[size];
        VertexBuffer vb = geom.getMesh().getBuffer(VertexBuffer.Type.Color);
        if (vb != null) {
            FloatBuffer b = ((FloatBuffer) vb.getData());
            b.rewind();
            b.get(buff);
        }
        return buff;
    }

    public void endBulkPaintSession(Geometry geom, float[] buffer) {
        if (geom.getMesh().getBuffer(VertexBuffer.Type.Color) != null) {
            writeToBuffer(buffer, geom);
        }
    }

    private class PainterCursorListener extends CursorAdapter {
        private CursorMotionEvent lastMotion;

        public void cursorButtonEvent(CursorButtonEvent event, Spatial target, Spatial capture) {
            if (event.getButtonIndex() == 0 || event.getButtonIndex() == 1) {
                painting = event.isPressed();
                if (painting) {
                    direction = event.getButtonIndex() == 0 ? 1 : -1;
                    lastEdit = new VertexPaintEdit(VertexPaintAppState.this, shootable);
                    paint();
                } else if (lastEdit != null && !lastEdit.isEmpty()) {
                    getSpix().getService(UndoManager.class).addEdit(lastEdit);
                    lastEdit = null;
                }

                if (!event.isPressed() && lastMotion != null) {
                    Vector3f contactPoint = null;
                    if (lastMotion.getCollision() != null) {
                        contactPoint = lastMotion.getCollision().getContactPoint();
                    }
                    getState(SpixState.class).getSpix().getBlackboard().set("main.selection.contactpoint", contactPoint);
                }
            }
        }

        public void cursorMoved(CursorMotionEvent event, Spatial target, Spatial capture) {
            this.lastMotion = event;

            CollisionResult closest = lastMotion.getCollision();
            if (closest == null) {
                brush.removeFromParent();
                return;
            }
            brush.setLocalTranslation(closest.getContactPoint());
            Quaternion q = brush.getLocalRotation();
            q.lookAt(closest.getContactNormal(), Vector3f.UNIT_Y);
            brush.setLocalRotation(q);
            brush.rotate(90f * FastMath.DEG_TO_RAD, 0, 0);
            brush.setLocalScale(brushRadius);

            if (brush.getParent() == null) {
                getRoot().attachChild(brush);
            }

            if (!painting) {
                return;
            }
            float dist = prevPos.distance(closest.getContactPoint());
            if (dist > brushRadius / 5f) {
                paint();
                prevPos.set(closest.getContactPoint());
            }
        }

    }

    private class ResizeCursorListener extends CursorAdapter {
        private CursorMotionEvent lastMotion;

        public void cursorMoved(CursorMotionEvent event, Spatial target, Spatial capture) {
            this.lastMotion = event;

            CollisionResult closest = lastMotion.getCollision();
            if (closest == null) {
                return;
            }

            brushRadius = brush.getWorldTranslation().distance(closest.getContactPoint());
        }

    }

}
