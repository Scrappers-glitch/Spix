package spix.app.utils;

import com.jme3.material.MatParam;
import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.shader.*;

import java.util.List;
import java.util.Set;

/**
 * Created by bouquet on 14/05/16.
 */
public class MaterialDefUtils {

    /**
     * TODO put this in core but this should be changed to handle all kinds of shader not just Vertex and Fragments.
     * TODO Also getShaderGenerationInfo ArrayLists should be sets really, to avoid duplicated and not have to avoid them ourselves.
     * This method could be in core actually and be used after loading a techniqueDef.
     * It computes all the information needed to generate the shader faster, from the ShaderNodes.
     * @param technique

     */
    public static void computeShaderNodeGenerationInfo(TechniqueDef technique) {

        List<ShaderNodeVariable> attributes = technique.getShaderGenerationInfo().getAttributes();
        List<ShaderNodeVariable> fragmentGlobals = technique.getShaderGenerationInfo().getFragmentGlobals();
        List<ShaderNodeVariable> fragmentUniforms = technique.getShaderGenerationInfo().getFragmentUniforms();
        List<ShaderNodeVariable> vertexUniforms = technique.getShaderGenerationInfo().getVertexUniforms();
        List<ShaderNodeVariable> varyings = technique.getShaderGenerationInfo().getVaryings();
        List<String> unusedNodes = technique.getShaderGenerationInfo().getUnusedNodes();

        //considering that none of the nodes are used, we'll remove them from the list when we have proof they are actually used.
        for (ShaderNode shaderNode : technique.getShaderNodes()) {
            unusedNodes.add(shaderNode.getName());
        }


        for (ShaderNode sn : technique.getShaderNodes()) {

            ShaderNodeDefinition def = sn.getDefinition();
            List<VariableMapping> in = sn.getInputMapping();
            if (in != null) {
                for (VariableMapping map : in) {
                    ShaderNodeVariable var = map.getRightVariable();
                    if (var.getNameSpace().equals("Global")) {
                        computeGlobals(technique, fragmentGlobals, def, var);
                    } else if (var.getNameSpace().equals("Attr")) {
                        attributes.add(var);
                    } else if (var.getNameSpace().equals("MatParam") || var.getNameSpace().equals("WorldParam")){
                        if(def.getType() == Shader.ShaderType.Fragment){
                            fragmentUniforms.add(var);
                        } else {
                            vertexUniforms.add(var);
                        }
                    } else {
                        //the nameSpace is the name of another node, if it comes from a different type of node the var is a varying
                        ShaderNode otherNode = null;
                        otherNode = findShaderNodeByName(technique, var.getNameSpace());
                        if(otherNode == null){
                            //we have a problem this should not happen...but let's not crash...
                            //TODO Maybe we could have an error list and report in it, then present the errors to the user.
                            continue;
                        }
                        if(otherNode.getDefinition().getType() != def.getType()){
                            varyings.add(var);
                        }
                        //and this other node is apparently used so we remove it from the unusedNodes list
                        unusedNodes.remove(otherNode.getName());
                    }
                }
            }
            List<VariableMapping> out = sn.getOutputMapping();
            if (out != null) {
                for (VariableMapping map : out) {
                    ShaderNodeVariable var = map.getLeftVariable();
                    if (var.getNameSpace().equals("Global")) {
                        computeGlobals(technique, fragmentGlobals, def, var);
                    }
                }
            }
        }
    }

    private static ShaderNode findShaderNodeByName(TechniqueDef technique, String name) {
        for (ShaderNode shaderNode : technique.getShaderNodes()) {
            if(shaderNode.getName().equals(name)){
                return shaderNode;
            }
        }
        return null;
    }

    /**
     * Some parameters may not be used, or not used as an input, but as a flag to command a define.
     * We didn't get them when looking into shader nodes mappings so let's do that now.
     * @param uniforms
     */
    public static void getAllUniforms(TechniqueDef technique, MaterialDef matDef, List<ShaderNodeVariable> uniforms){
        uniforms.clear();
        uniforms.addAll(technique.getShaderGenerationInfo().getFragmentUniforms());
        uniforms.addAll(technique.getShaderGenerationInfo().getVertexUniforms());

        for (UniformBinding worldParam : technique.getWorldBindings()) {
            ShaderNodeVariable var = new ShaderNodeVariable(worldParam.getGlslType(), "WorldParam", worldParam.name());
            if(!contains(uniforms, var)) {
                uniforms.add(var);
            }
        }

        for (MatParam matParam : matDef.getMaterialParams()) {
            ShaderNodeVariable var = new ShaderNodeVariable(matParam.getVarType().getGlslType(), "MatParam", matParam.getName());
            if(!contains(uniforms, var)) {
                uniforms.add(var);
            }
        }
    }

    private static void computeGlobals(TechniqueDef technique, List<ShaderNodeVariable> fragmentGlobals, ShaderNodeDefinition def, ShaderNodeVariable var) {
        if (def.getType() == Shader.ShaderType.Vertex) {
            if (technique.getShaderGenerationInfo().getVertexGlobal() == null) {
                technique.getShaderGenerationInfo().setVertexGlobal(var);
            }
        } else {
            if (!contains(fragmentGlobals, var)) {
                fragmentGlobals.add(var);
            }
        }
    }


    /**
     * returns true if a ShaderNode variable is already contained in a list of variables.
     * TODO This could be handled with a Collection.contains, if ShaderNodeVariable had a proper equals and hashcode
     * @param vars
     * @param var
     * @return
     */
    public static boolean contains(List<ShaderNodeVariable> vars, ShaderNodeVariable var) {
        for (ShaderNodeVariable shaderNodeVariable : vars) {
            if (shaderNodeVariable.getName().equals(var.getName()) && shaderNodeVariable.getNameSpace().equals(var.getNameSpace())) {
                return true;
            }
        }
        return false;
    }
}
