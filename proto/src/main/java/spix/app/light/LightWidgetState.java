/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package spix.app.light;

import com.jme3.app.*;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.*;
import com.jme3.light.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.control.*;
import com.jme3.scene.shape.*;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.*;
import spix.app.*;
import spix.core.*;

import java.nio.*;


/**
 * Draws a translation widget over the currently selected object(s)
 * and allows dragging them using various manipulations of the widget.
 *
 * @author Paul Speed
 */
public class LightWidgetState extends BaseAppState {

//    private Node widget;
//    private Node centerNode;
//    private Geometry radial;
//    private Geometry center;
//    private Material[] axisMaterials = new Material[3];
//    private ColorRGBA[] axisColors = new ColorRGBA[3];
    private Camera cam;
    private Node lightNode;
    private Material dashed;
    private Material dot;
    private Quaternion tmpRot = new Quaternion();


   // private Vector3f selectionCenter = new Vector3f();

    public LightWidgetState() {
    }

    protected Spix getSpix() {
        return getState(SpixState.class).getSpix();
    }

    protected Node getRoot() {
        return getState(DecoratorViewPortState.class).getRoot();
    }

    @Override
    protected void initialize(Application app) {
        initMaterials();


        Node rootNode = ((SimpleApplication) app).getRootNode();
        cam = app.getCamera();
        lightNode = new Node("Light Node");


        //Compute some arbitrary position for the Directional and AmbientLight.
        //Not really great, but will do for now
        Vector3f pos = new Vector3f();
        BoundingVolume bv = rootNode.getWorldBound();
        if (bv.getType() == BoundingVolume.Type.AABB) {
            BoundingBox bb = (BoundingBox) bv;
            pos.set(bb.getCenter()).addLocal(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
        } else {
            BoundingSphere bs = (BoundingSphere) bv;
            pos.set(bs.getCenter()).addLocal(bs.getRadius() / 2f, bs.getRadius() / 2f, bs.getRadius() / 2f);
        }
        pos.multLocal(4);


        //light are loaded at initialization, for now
        // there will be a process to update them whenever they get modified/added/removed
        for (Light light : rootNode.getLocalLightList()) {
            Node widget = new Node();
            switch (light.getType()){
                case Directional:
                    widget.setLocalTranslation(pos);
                    DirectionalLight dl = (DirectionalLight)light;
                    widget.attachChild(makeDirectionalLightWidget());
                    Spatial lightDirW = widget.getChild("LightDirection");
                    tmpRot.set(lightDirW.getWorldRotation()).lookAt(dl.getDirection(),Vector3f.UNIT_Y);
                    lightDirW.setLocalRotation(tmpRot);
                    break;
                case Ambient:
                    widget.setLocalTranslation(pos);
                    widget.attachChild(makeAmbientLightWidget());
                    break;
                case Point:
                    PointLight pl = (PointLight)light;
                    widget.setLocalTranslation(pl.getPosition());
                    widget.attachChild(makePointLightWidget());
                    Spatial lightRadiusW = widget.getChild("LightRadius");
                    lightRadiusW.setLocalScale(pl.getRadius());
                    break;
                case Spot:
                    widget.setLocalTranslation(((SpotLight)light).getPosition());
                    SpotLight sl = (SpotLight)light;
                    widget.attachChild(makeSpotLightWidget());
                    lightDirW = widget.getChild("LightDirection");
                    tmpRot.set(lightDirW.getWorldRotation()).lookAt(sl.getDirection(),Vector3f.UNIT_Y);
                    lightDirW.setLocalRotation(tmpRot);
                    lightDirW.setLocalScale(sl.getSpotRange());
                    break;
                default:
                    widget.setLocalTranslation(pos);
                    break;
            }
            //Offset the position so that the next light doesn't overlap
            pos.addLocal(1f,0,0);
            lightNode.attachChild(widget);
        }

    }

    private void initMaterials() {
        GuiGlobals globals = GuiGlobals.getInstance();

        Texture texture = globals.loadTexture("Interface/small-circle.png", false, false);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        dot = globals.createMaterial(texture, false).getMaterial();
        dot.setColor("Color", ColorRGBA.Black);
        dot.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        dashed = new Material(getApplication().getAssetManager(), "MatDefs/dashed/dashed.j3md");
        dashed.getAdditionalRenderState().setWireframe(true);
        dashed.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        dashed.getAdditionalRenderState().setDepthWrite(false);
        dashed.setFloat("DashSize", 0.5f);
        dashed.setColor("Color", ColorRGBA.Black);
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {
        getRoot().attachChild(lightNode);

    }

    @Override
    protected void onDisable() {
        lightNode.removeFromParent();
    }


    @Override
    public void update(float tpf) {
    }

    @Override
    public void render(RenderManager renderManager) {
    }

    private Node makeDirectionalLightWidget() {
        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);
        int radialSamples = 16;
        int lineSegments = 10;

        FloatBuffer posBuf = BufferUtils.createVector3Buffer((radialSamples + 1 + lineSegments + 1) * 2);
        FloatBuffer texBuf = BufferUtils.createVector2Buffer((radialSamples + 1 + lineSegments + 1) * 2);
        ShortBuffer idxBuf = BufferUtils.createShortBuffer((2 * (radialSamples + lineSegments)) * 2);

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        m.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        int idx = 0;

        idx = makeCircle(radialSamples, 0.14f, posBuf, texBuf, idxBuf, idx);
        idx = makeCircle(radialSamples, 0.11f, posBuf, texBuf, idxBuf, idx);
        idx = makeSegmentedLine(lineSegments, Axis.X , 0.4f, -0.2f, Vector3f.ZERO, posBuf, texBuf, idxBuf, idx);
        idx = makeSegmentedLine(lineSegments, Axis.Y , 0.4f, -0.2f, Vector3f.ZERO, posBuf, texBuf, idxBuf, idx);

        m.updateBound();
        m.setStatic();

        Geometry geom = new Geometry("LightDebug", m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setMaterial(dashed);


        Mesh line = new Mesh();
        line.setMode(Mesh.Mode.Lines);
        lineSegments = 30;

        posBuf = BufferUtils.createVector3Buffer(lineSegments + 1);
        texBuf = BufferUtils.createVector2Buffer(lineSegments + 1);
        idxBuf = BufferUtils.createShortBuffer(2 * lineSegments);

        line.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        line.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        line.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        makeSegmentedLine(lineSegments, Axis.Z , 4f, 0.2f, Vector3f.ZERO, posBuf, texBuf, idxBuf, 0);

        line.updateBound();
        line.setStatic();

        Geometry geom2 = new Geometry("LightDirection", line);
        geom2.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom2.setMaterial(dashed);


        Node widget = new Node();
        Node center = makeCenter();
        center.attachChild(geom);
        widget.attachChild(center);
        widget.attachChild(geom2);
        return widget;

    }

    private Node makePointLightWidget() {
        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);
        int radialSamples = 16;

        FloatBuffer posBuf = BufferUtils.createVector3Buffer(radialSamples + 1);
        FloatBuffer texBuf = BufferUtils.createVector2Buffer(radialSamples + 1);
        ShortBuffer idxBuf = BufferUtils.createShortBuffer(2 * radialSamples);

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        m.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        makeCircle(radialSamples, 0.14f, posBuf, texBuf, idxBuf, 0);

        m.updateBound();
        m.setStatic();

        Geometry geom = new Geometry("LightDebug", m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setMaterial(dashed);


        Mesh line = new Mesh();
        line.setMode(Mesh.Mode.Lines);
        radialSamples = 128;

        posBuf = BufferUtils.createVector3Buffer(radialSamples + 1);
        texBuf = BufferUtils.createVector2Buffer(radialSamples + 1);
        idxBuf = BufferUtils.createShortBuffer(2 * radialSamples);

        line.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        line.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        line.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        makeCircle(radialSamples, 1f, posBuf, texBuf, idxBuf, 0);

        line.updateBound();
        line.setStatic();

        Geometry geom2 = new Geometry("LightRadius", line);
        geom2.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom2.setMaterial(dashed);


        Node widget = new Node();
        Node center = makeCenter();
        center.attachChild(geom);
        widget.attachChild(center);
        widget.attachChild(geom2);
        geom2.addControl(new BillboardControl());
        return widget;

    }

    private Node makeAmbientLightWidget() {
        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);
        int lineSegments = 10;

        FloatBuffer posBuf = BufferUtils.createVector3Buffer(( lineSegments + 1) * 3);
        FloatBuffer texBuf = BufferUtils.createVector2Buffer(( lineSegments + 1) * 3);
        ShortBuffer idxBuf = BufferUtils.createShortBuffer(2 * (lineSegments * 3));

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        m.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        int idx = 0;

        idx = makeSegmentedLine(lineSegments, Axis.X , 0.3f, -0.15f,  new Vector3f(0, 0.1f, 0), posBuf, texBuf, idxBuf, idx);
        idx = makeSegmentedLine(lineSegments, Axis.X , 0.4f, -0.2f,  Vector3f.ZERO, posBuf, texBuf, idxBuf, idx);
        idx = makeSegmentedLine(lineSegments, Axis.X , 0.3f, -0.15f,  new Vector3f(0, -0.1f, 0), posBuf, texBuf, idxBuf, idx);

        m.updateBound();
        m.setStatic();

        Geometry geom = new Geometry("LightDebug", m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setMaterial(dashed);

        Node widget = new Node();
        Node center = makeCenter();
        center.attachChild(geom);
        widget.attachChild(center);

        return widget;

    }

    private Node makeSpotLightWidget() {
        Mesh m = new Mesh();
        m.setMode(Mesh.Mode.Lines);
        int radialSamples = 16;

        FloatBuffer posBuf = BufferUtils.createVector3Buffer((radialSamples + 1));
        FloatBuffer texBuf = BufferUtils.createVector2Buffer((radialSamples + 1));
        ShortBuffer idxBuf = BufferUtils.createShortBuffer((2 * radialSamples));

        m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        m.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        m.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        makeCircle(radialSamples, 0.11f, posBuf, texBuf, idxBuf, 0);

        m.updateBound();
        m.setStatic();

        Geometry geom = new Geometry("LightDebug", m);
        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom.setMaterial(dashed);


        Mesh line = new Mesh();
        line.setMode(Mesh.Mode.Lines);
        int lineSegments = 30;

        posBuf = BufferUtils.createVector3Buffer((lineSegments + 1));
        texBuf = BufferUtils.createVector2Buffer((lineSegments + 1));
        idxBuf = BufferUtils.createShortBuffer(2 * lineSegments);

        line.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        line.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        line.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);

        makeSegmentedLine(lineSegments, Axis.Z , 1f, 0, Vector3f.ZERO, posBuf, texBuf, idxBuf, 0);

        line.updateBound();
        line.setStatic();

        Geometry geom2 = new Geometry("LightDirection", line);
        geom2.setQueueBucket(RenderQueue.Bucket.Transparent);
        geom2.setMaterial(dashed);


        Node widget = new Node();
        Node center = makeCenter();
        center.attachChild(geom);
        widget.attachChild(center);
        widget.attachChild(geom2);
        return widget;

    }


    private int makeCircle(int radialSamples, float radius, FloatBuffer posBuf, FloatBuffer texBuf, ShortBuffer idxBuf, int idx) {
        // generate geometry
        float fInvRS = 1.0f / radialSamples;

        // Generate points on the unit circle to be used in computing the mesh
        // points on a sphere slice.
        float[] afSin = new float[(radialSamples + 1)];
        float[] afCos = new float[(radialSamples + 1)];
        for (int iR = 0; iR < radialSamples; iR++) {
            float fAngle = FastMath.TWO_PI * fInvRS * iR;
            afCos[iR] = FastMath.cos(fAngle) * radius;
            afSin[iR] = FastMath.sin(fAngle) * radius;
        }
        afSin[radialSamples] = afSin[0];
        afCos[radialSamples] = afCos[0];

        for (int iR = 0; iR <= radialSamples; iR++) {
            posBuf.put(afCos[iR])
                    .put(afSin[iR])
                    .put(0);
            texBuf.put(iR % 2f)
                    .put(iR % 2f);

        }
        return writeIndex(radialSamples, idxBuf, idx);
    }

    private int writeIndex(int radialSamples, ShortBuffer idxBuf, int idx) {
        int segDone = 0;
        while (segDone < radialSamples) {
            idxBuf.put((short) idx);
            idxBuf.put((short) (idx + 1));
            idx++;
            segDone++;
        }
        idx++;
        return idx;
    }

    private Node makeCenter(){

        Node center = new Node("Light center");
        // Now the teeny tiny center that never disappears
        Mesh mesh = new Quad(0.08f, 0.08f);
        final Geometry g = new Geometry("centerOrigin", mesh);
        g.setMaterial(dot);
        g.setLocalTranslation(-0.04f,-0.04f,0.0f);
        center.attachChild(g);
        center.addControl(new BillboardControl());
        center.addControl(new ScaleControl());
        return center;
    }

    private int makeSegmentedLine(int nbSegments, Axis xAxis, float size, float start, Vector3f offset,FloatBuffer posBuf, FloatBuffer texBuf, ShortBuffer idxBuf, int idx) {

        for (int i = 0; i <= nbSegments; i++) {
            float value = start + (size / nbSegments) * i;
            float x = xAxis == Axis.X?value:offset.x;
            float y = xAxis == Axis.Y?value:offset.y;
            float z = xAxis == Axis.Z?value:offset.z;
            posBuf.put(x).put(y).put(z);
            texBuf.put(i % 2f).put(i % 2f);
        }
        return writeIndex(nbSegments, idxBuf, idx);
    }

    private class ScaleControl extends AbstractControl {

        @Override
        protected void controlUpdate(float tpf) {
            Vector3f dir = cam.getDirection();
            float distance = dir.dot(spatial.getWorldTranslation().subtract(cam.getLocation()));

            // m11 of the projection matrix defines the distance at which 1 pixel
            // is 1 unit.  Kind of.
            float m11 = cam.getProjectionMatrix().m11;
            // Magic scaling... trust the math... don't question the math... magic math...
            float halfHeight = cam.getHeight() * 0.5f;
            float scale = ((distance/halfHeight) * 100)/m11;
            spatial.setLocalScale(scale);
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {

        }
    }

}
