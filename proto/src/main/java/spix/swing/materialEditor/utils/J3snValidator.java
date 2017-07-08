package spix.swing.materialEditor.utils;

import com.jme3.asset.ShaderNodeDefinitionKey;
import com.jme3.material.plugins.ShaderNodeLoaderDelegate;
import com.jme3.shader.ShaderNode;
import com.jme3.shader.ShaderNodeDefinition;
import com.jme3.util.blockparser.BlockLanguageParser;
import com.jme3.util.blockparser.Statement;
import spix.app.material.MaterialService;
import spix.swing.materialEditor.controller.*;
import spix.swing.materialEditor.panels.ShaderNodeCodePanel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by Nehon on 08/07/2017.
 */
public class J3snValidator {

    Map<String, String> toRename = new HashMap<>();
    List<String> toCreate = new ArrayList<>();
    List<String> toDelete = new ArrayList<>();

    public boolean isValid(ShaderNodeCodePanel.Document document, DataHandler dataHandler, DiagramUiHandler diagramUiHandler, MatDefEditorController controller) {
        ByteArrayInputStream in = new ByteArrayInputStream(document.getContent().getBytes());
        try {
            List<Statement> roots = BlockLanguageParser.parse(in);
            ShaderNodeLoaderDelegate loader = new ShaderNodeLoaderDelegate();
            ShaderNodeDefinitionKey key = new ShaderNodeDefinitionKey(document.getName());
            key.setLoadDocumentation(true);
            List<ShaderNodeDefinition> defs = loader.readNodesDefinitions(roots.get(0).getContents(), key);
            for (ShaderNodeDefinition def : defs) {
                List<ShaderNode> nodes = dataHandler.getShaderNodesWithDef(def);
                ShaderNodeDefinition oldDef = null;
                for (ShaderNode node : nodes) {
                    boolean typeChanged = node.getDefinition().getType() != def.getType();
                    oldDef = node.getDefinition();
                    dataHandler.cleanUpMappings(node, def);
                    node.setDefinition(def);
                    diagramUiHandler.refreshShaderNodePanel(controller, node, dataHandler.getCurrentTechnique(), !typeChanged);
                }
                if (oldDef != null) {
                    //check for shader files changes
                    for (String path : oldDef.getShadersPath()) {
                        if (!def.getShadersPath().contains(path)) {
                            toDelete.add(path);
                        }
                    }
                    for (String path : def.getShadersPath()) {
                        if (!oldDef.getShadersPath().contains(path)) {
                            toCreate.add(path);
                        }
                    }

                    for (int i = 0; i < oldDef.getShadersLanguage().size(); i++) {
                        String lang = oldDef.getShadersLanguage().get(i);
                        String filePath = oldDef.getShadersPath().get(i);
                        if (toDelete.contains(filePath)) {
                            int index = getLangIndex(def, lang);
                            if (index >= 0) {
                                String newFilePath = def.getShadersPath().get(index);
                                toDelete.remove(filePath);
                                toCreate.remove(newFilePath);
                                toRename.put(filePath, newFilePath);
                            }
                        }
                    }

                }
            }
            controller.getShaderNodeCodePanel().clearError(document.getName());
        } catch (IOException e1) {
            e1.printStackTrace();
            controller.getErrorLog().error(new MaterialService.CompilationError("Error in shader node definition " + document.getName() + "\n" + e1.getMessage(), e1, 0));
            //gui.getSpix().getService(MessageRequester.class).showMessage("Error in shader node definition " + document.getName(), e1.getMessage(), MessageRequester.Type.Error);
            controller.getShaderNodeCodePanel().setError(document.getName(), e1);
            controller.getShaderNodeCodePanel().refreshErrors();
            return false;
        }
        return true;
    }

    private int getLangIndex(ShaderNodeDefinition def, String lang) {
        for (int i = 0; 1 < def.getShadersLanguage().size(); i++) {
            if (def.getShadersLanguage().get(i).equals(lang)) {
                return i;
            }
        }
        return -1;
    }

    public void reset() {
        toRename.clear();
        toCreate.clear();
        toDelete.clear();
    }

    public List<String> getToDelete() {
        return toDelete;
    }

    public Map<String, String> getToRename() {
        return toRename;
    }

    public List<String> getToCreate() {
        return toCreate;
    }
}
