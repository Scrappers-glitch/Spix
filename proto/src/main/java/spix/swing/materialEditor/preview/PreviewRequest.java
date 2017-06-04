package spix.swing.materialEditor.preview;

import com.jme3.material.MatParam;
import com.jme3.material.MaterialDef;
import com.jme3.shader.Shader;
import spix.app.material.MaterialAppState;

import java.util.List;
import java.util.Map;

/**
 * Created by Nehon on 26/05/2016.
 */
public class PreviewRequest {

    private Shader.ShaderType shaderType;
    private MaterialAppState.DisplayType displayType;
    private List<String> nodeGraph;
    private MaterialDef materialDef;
    private Map<String, MatParam> matParams;
    private String techniqueName;
    private int outIndex = 0;

    public PreviewRequest(Shader.ShaderType shaderType, List<String> nodeGraph, MaterialAppState.DisplayType displayType) {
        this.shaderType = shaderType;
        this.nodeGraph = nodeGraph;
        this.displayType = displayType;
    }

    public Shader.ShaderType getShaderType() {
        return shaderType;
    }

    public MaterialAppState.DisplayType getDisplayType() {
        return displayType;
    }

    public List<String> getNodeGraph() {
        return nodeGraph;
    }

    public int getOutIndex() {
        return outIndex;
    }

    public void setOutIndex(int outIndex) {
        this.outIndex = outIndex;
    }

    public MaterialDef getMaterialDef() {
        return materialDef;
    }

    public void setMaterialDef(MaterialDef materialDef) {
        this.materialDef = materialDef;
    }

    public String getTechniqueName() {
        return techniqueName;
    }

    public void setTechniqueName(String techniqueName) {
        this.techniqueName = techniqueName;
    }

    public Map<String, MatParam> getMatParams() {
        return matParams;
    }

    public void setMatParams(Map<String, MatParam> matParams) {
        this.matParams = matParams;
    }
}
