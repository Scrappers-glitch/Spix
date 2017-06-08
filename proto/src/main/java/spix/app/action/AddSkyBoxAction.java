package spix.app.action;

import com.jme3.asset.AssetManager;
import com.jme3.scene.*;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import spix.app.FileIoService;
import spix.core.*;
import spix.undo.UndoManager;
import spix.undo.edit.SpatialAddEdit;

import static spix.app.DefaultConstants.ASSET_MANAGER;
import static spix.app.DefaultConstants.SCENE_ROOT;
import static spix.app.DefaultConstants.SELECTION_PROPERTY;

/**
 * Created by nehon on 28/12/16.
 */
public class AddSkyBoxAction extends AddAction {

    public AddSkyBoxAction(String id) {
        super(id);
    }


    @Override
    public void performAction(Spix spix) {
        spix.getService(FileIoService.class).requestTexture("HDR Equirect file", ".hdr", true, new RequestCallback<Texture>() {
            @Override
            public void done(Texture result) {
                Spatial skyBox = SkyFactory.createSky(spix.getBlackboard().get(ASSET_MANAGER, AssetManager.class), result, SkyFactory.EnvMapType.EquirectMap);
                skyBox.setLocalScale(1000);
                Node scene = spix.getBlackboard().get(SCENE_ROOT, Node.class);
                scene.attachChild(skyBox);
                UndoManager um = spix.getService(UndoManager.class);
                um.addEdit(new SpatialAddEdit(scene, skyBox));
                //select the newly created node
                SelectionModel model = spix.getBlackboard().get(SELECTION_PROPERTY, SelectionModel.class);
                model.setSingleSelection(skyBox);
            }
        });
    }
}


