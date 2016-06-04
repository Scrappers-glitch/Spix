package spix.swing.materialEditor.preview;

import com.jme3.material.MaterialDef;
import com.jme3.shader.Shader;
import spix.app.material.MaterialAppState;

import javax.swing.*;

/**
 * Created by Nehon on 26/05/2016.
 */
public class PreviewRequest {

    private Shader.ShaderType shaderType;
    private MaterialAppState.DisplayType displayType;
    private JLabel targetLabel;
    private String outputForNode;
    private MaterialDef materialDef;
    private String techniqueName;
    private int outIndex = 0;

    public PreviewRequest(Shader.ShaderType shaderType, String outputForNode, JLabel targetLabel, MaterialAppState.DisplayType displayType) {
        this.shaderType = shaderType;
        this.outputForNode = outputForNode;
        this.targetLabel = targetLabel;
        this.displayType = displayType;
    }

    public Shader.ShaderType getShaderType() {
        return shaderType;
    }

    public MaterialAppState.DisplayType getDisplayType() {
        return displayType;
    }

    public String getOutputForNode() {
        return outputForNode;
    }

    public JLabel getTargetLabel() {
        return targetLabel;
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
}
