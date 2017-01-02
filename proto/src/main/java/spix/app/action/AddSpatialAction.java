package spix.app.action;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.*;
import com.jme3.scene.shape.*;
import spix.core.SelectionModel;
import spix.core.Spix;
import spix.undo.UndoManager;
import spix.undo.edit.SpatialAddEdit;

import static spix.app.DefaultConstants.ASSET_MANAGER;
import static spix.app.DefaultConstants.SELECTION_PROPERTY;

/**
 * Created by nehon on 28/12/16.
 */
public class AddSpatialAction extends AddAction{

    public static final Node DEFAULT_NODE = new Node("New Node");
    public static final Geometry DEFAULT_BOX = new Geometry("Box", new Box(1,1,1));
    public static final Geometry DEFAULT_QUAD = new Geometry("Quad", new Quad(1,1));
    public static final Geometry DEFAULT_SPHERE = new Geometry("Sphere", new Sphere(16,16,1));
    public static final Geometry DEFAULT_CYLINDER = new Geometry("Cylinder", new Cylinder(10,16, 1, 5));
    public static final Geometry DEFAULT_TORUS = new Geometry("Torus", new Torus(16,16,0.8f, 1));

    private Spatial spatial;

    public AddSpatialAction(String id, Spatial spatial, Spix spix) {
        super(id);
        this.spatial = spatial;
    }


    @Override
    public void performAction(Spix spix) {
        Spatial toAdd = spatial.deepClone();
        Node node = getAncestorNode(spix);
        if(toAdd instanceof Geometry){
            Geometry g = (Geometry)toAdd;
            if(g.getMaterial() == null){
                g.setMaterial(getDefaultMaterial(spix));
            }
        }
        node.attachChild(toAdd);
        UndoManager um = spix.getService(UndoManager.class);
        um.addEdit(new SpatialAddEdit(node, toAdd));
        //select the newly created node
        SelectionModel model = spix.getBlackboard().get(SELECTION_PROPERTY, SelectionModel.class);
        model.setSingleSelection(toAdd);
    }

    private Material getDefaultMaterial(Spix spix){
        AssetManager assetManager = spix.getBlackboard().get(ASSET_MANAGER, AssetManager.class);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setName(mat.getMaterialDef().getName());
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.Gray.clone());
        mat.setColor("Ambient", ColorRGBA.DarkGray.clone());
        return mat;
    }

    private Material getUnshadedMaterial(Spix spix) {
        AssetManager assetManager = spix.getBlackboard().get(ASSET_MANAGER, AssetManager.class);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setName(mat.getMaterialDef().getName());
        mat.setColor("Color", ColorRGBA.Blue.clone());
        return mat;
    }
}
