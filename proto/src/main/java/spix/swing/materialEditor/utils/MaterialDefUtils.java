package spix.swing.materialEditor.utils;

import com.jme3.material.*;
import com.jme3.shader.*;
import spix.swing.materialEditor.Dot;

import java.util.*;

import static java.awt.SystemColor.info;
import static spix.swing.materialEditor.icons.Icons.node;
import static sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl.ThreadStateMap.Byte1.other;

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
        attributes.clear();
        fragmentGlobals.clear();
        fragmentUniforms.clear();
        vertexUniforms.clear();
        varyings.clear();
        unusedNodes.clear();

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
                        addUnique(attributes, var);
                    } else if (var.getNameSpace().equals("MatParam") || var.getNameSpace().equals("WorldParam")){
                        //Remove the g_ and the m_ form the uniform name
                        var.setName(var.getName().replaceFirst("g_","").replaceAll("m_",""));
                        if(def.getType() == Shader.ShaderType.Fragment){
                            addUnique(fragmentUniforms, var);
                        } else {
                            addUnique( vertexUniforms, var);
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
                            addUnique(varyings, var);
                            var.setShaderOutput(true);
                            for (VariableMapping variableMapping : otherNode.getInputMapping()) {
                                if (variableMapping.getLeftVariable().getName().equals(var.getName())) {
                                    variableMapping.getLeftVariable().setShaderOutput(true);
                                }
                            }
                        }
                        //and this other node is apparently used so we remove it from the unusedNodes list
                        unusedNodes.remove(otherNode.getName());
                    }
                }

            }
            List<VariableMapping> out = sn.getOutputMapping();
            if (out != null && !out.isEmpty()) {
                for (VariableMapping map : out) {
                    ShaderNodeVariable var = map.getLeftVariable();
                    if (var.getNameSpace().equals("Global")) {
                        computeGlobals(technique, fragmentGlobals, def, var);
                    }
                }
                //shader has an output it's used in the shader code.
                unusedNodes.remove(sn.getName());
            } else {
                //some nodes has no output by design ans their def specifies so.
                if(sn.getDefinition().isNoOutput()){
                    unusedNodes.remove(sn.getName());
                }
            }
        }
        computeConditions(technique);
    }

    private static void computeConditions(TechniqueDef techniqueDef) {

        ShaderGenerationInfo info = techniqueDef.getShaderGenerationInfo();
        updateConditions(info.getVertexUniforms(), techniqueDef.getShaderNodes());
        updateConditions(info.getFragmentUniforms(), techniqueDef.getShaderNodes());
        updateConditions(info.getVaryings(), techniqueDef.getShaderNodes());

        for (ShaderNodeVariable v : info.getVaryings()) {
            for (ShaderNode sn : techniqueDef.getShaderNodes()) {
                if (sn.getDefinition().getType() == Shader.ShaderType.Vertex) {
                    for (VariableMapping mapping : sn.getInputMapping()) {
                        if (mapping.getLeftVariable().equals(v)) {
                            if (mapping.getCondition() == null || v.getCondition() == null) {
                                mapping.setCondition(v.getCondition());
                            } else {
                                mapping.setCondition("(" + mapping.getCondition() + ") || (" + v.getCondition() + ")");
                            }
                        }
                    }
                }
            }
        }

        updateConditions(info.getAttributes(), techniqueDef.getShaderNodes());
    }

    private static void updateConditions(List<ShaderNodeVariable> vars, List<ShaderNode> allNodes) {
        for (ShaderNodeVariable shaderNodeVariable : vars) {
            makeCondition(shaderNodeVariable, allNodes);
        }
    }

    // TODO: 01/06/2016 All this condition computation seems very very inefficient, I should rework this.
    private static void makeCondition(ShaderNodeVariable var, List<ShaderNode> allNodes) {
        var.setCondition(null);
        List<ShaderNode> nodes = new ArrayList<>();

        for (ShaderNode node : allNodes) {
            for (VariableMapping mapping : node.getInputMapping()) {
                if(mapping.getRightVariable() == var){
                    nodes.add(node);
                }
            }
        }


        for (ShaderNode node : nodes) {
            String condition = null;
            for (VariableMapping mapping : node.getInputMapping()) {
                if (mapping.getRightVariable().equals(var)) {
                    if (mapping.getCondition() == null) {
                        condition = null;
                        break;
                    }
                    if (condition == null) {
                        condition = "(" + mapping.getCondition() + ")";
                    } else {
                        if (!condition.contains(mapping.getCondition())) {
                            condition = condition + " || (" + mapping.getCondition() + ")";
                        }
                    }
                }
            }
            if (node.getCondition() == null && condition == null) {
                var.setCondition(null);
                return;
            }
            if (node.getCondition() != null) {
                if (condition == null) {
                    condition = node.getCondition();
                } else {
                    if (!condition.contains(node.getCondition())) {
                        condition = "(" + node.getCondition() + ") && (" + condition + ")";
                    }
                }
            }
            if (var.getCondition() == null) {
                var.setCondition(condition);
            } else {
                if (!var.getCondition().contains(condition)) {
                    var.setCondition("(" + var.getCondition() + ") || (" + condition + ")");
                }
            }

        }
    }

    /**
     * Adds back the g_ and m_ for world params and mat params needed for the technique to work properly
     * @param info
     */
    public static void fixUniformNames(ShaderGenerationInfo info) {
        for (ShaderNodeVariable var : info.getFragmentUniforms()) {
            fixUniformName(var);
        }

        for (ShaderNodeVariable var : info.getVertexUniforms()) {
            fixUniformName(var);
        }
    }


    private static void addUnique(List<ShaderNodeVariable> variables, ShaderNodeVariable var){
        for (ShaderNodeVariable variable : variables) {
            if(var.equals(variable)){
                return;
            }
        }
        variables.add(var);
    }
    /**
     * as previous method but for one variable.
     * @param var
     */
    private static void fixUniformName(ShaderNodeVariable var) {
        if(var.getNameSpace().equals("MatParam") && !var.getName().startsWith("m_")){
            var.setName("m_" + var.getName());
        } else if(var.getNameSpace().equals("WorldParam") && !var.getName().startsWith("g_")){
            var.setName("g_" + var.getName());
        }
    }

    /**
     * Retrieve a shader node by name
     * @param technique
     * @param name
     * @return
     */
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
        var.setShaderOutput(true);
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

    public static String makeConnectionKey(String rightNameSpace, String rightName, String leftNameSpace, String leftName, String techName) {
        return techName + "." + leftNameSpace + "." + leftName + "|" + rightNameSpace + "." + rightName;
    }

    public static String makeShaderNodeKey(String techName, String nodeName ){
        return techName + "." + nodeName;
    }

    public static String makeGlobalOutKey(String techName, String variableName, String index) {
        return techName + ".Global." + variableName + "." + index;
    }

    public static String makeInputKey(String techName, String nameSpace, String variableName) {
        return techName + "." + nameSpace + "." + variableName;
    }

    public static VariableMapping createVariableMapping(Dot start, Dot end){
        ShaderNodeVariable leftVariable = new ShaderNodeVariable(end.getType(), end.getNode().getName(), end.getText());
        ShaderNodeVariable rightVariable = new ShaderNodeVariable(start.getType(), start.getNode().getName(), start.getText());

        int endCard = ShaderUtils.getCardinality(end.getType(), "");
        int startCard = ShaderUtils.getCardinality(start.getType(), "");
        String swizzle = "xyzw";
        String rightVarSwizzle = "";
        String leftVarSwizzle ="";
        if (startCard > endCard) {
            rightVarSwizzle = swizzle.substring(0, endCard);
        } else if (endCard > startCard) {
            leftVarSwizzle = swizzle.substring(0, startCard);
        }

        return new VariableMapping(leftVariable, leftVarSwizzle, rightVariable, rightVarSwizzle, null);
    }
}
