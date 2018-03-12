package spix.swing.materialEditor.utils;

import com.jme3.material.MatParam;
import com.jme3.material.MaterialDef;
import com.jme3.material.ShaderGenerationInfo;
import com.jme3.material.TechniqueDef;
import com.jme3.material.plugins.ConditionParser;
import com.jme3.material.plugins.MatParseException;
import com.jme3.shader.*;
import spix.swing.materialEditor.Dot;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static org.yaml.snakeyaml.nodes.NodeId.mapping;

/**
 * Created by bouquet on 14/05/16.
 */
public class MaterialDefUtils {

    private static ConditionParser parser = new ConditionParser();

    /**
     * TODO put this in core but this should be changed to handle all kinds of shader not just Vertex and Fragments.
     * TODO Also getShaderGenerationInfo ArrayLists should be sets really, to avoid duplicated and not have to avoid them ourselves.
     * This method could be in core actually and be used after loading a techniqueDef.
     * It computes all the information needed to generate the shader faster, from the ShaderNodes.
     * @param technique

     */
    public static void computeShaderNodeGenerationInfo(TechniqueDef technique, MaterialDef matDef) throws IOException {


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
            checkDefineFromCondition(sn.getCondition(), matDef, technique);
            ShaderNodeDefinition def = sn.getDefinition();
            List<VariableMapping> in = sn.getInputMapping();
            if (in != null) {
                for (VariableMapping map : in) {
                    checkDefineFromCondition(map.getCondition(), matDef, technique);
                    ShaderNodeVariable var = map.getRightVariable();
                    if (var.getNameSpace().equals("Global")) {
                        computeGlobals(technique, fragmentGlobals, def, var);
                    } else if (var.getNameSpace().equals("Attr")) {
                        addUnique(attributes, var);
                    } else if (var.getNameSpace().equals("MatParam") || var.getNameSpace().equals("WorldParam")){
                        checkMultiplicity(technique, matDef, map, var);
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
                    checkDefineFromCondition(map.getCondition(), matDef, technique);
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
    }

    private static void checkDefineFromCondition(String condition, MaterialDef matDef, TechniqueDef techniqueDef) {
        if (condition == null) {
            return;
        }
        List<String> defines = parser.extractDefines(condition);
        for (String define : defines) {
            MatParam param = findMatParam(define, matDef);
            if (param != null) {
                addDefine(techniqueDef, param.getName(), param.getVarType());
            }
        }
    }

    public static void checkMultiplicity(TechniqueDef technique, MaterialDef matDef, VariableMapping map, ShaderNodeVariable var) throws IOException {
        if (map.getLeftVariable().getMultiplicity() != null) {
            MatParam param = findMatParam(map.getRightVariable().getName(), matDef);
            if (!param.getVarType().name().endsWith("Array")) {
                throw new IOException(param.getName() + " is not of Array type");
            }
            String multiplicity = map.getLeftVariable().getMultiplicity();
            try {
                Integer.parseInt(multiplicity);
            } catch (NumberFormatException nfe) {
                //multiplicity is not an int attempting to find for a material parameter.
                MatParam mp = findMatParam(multiplicity, matDef);
                if (mp != null) {
                    //It's tied to a material param, let's create a define and use this as the multiplicity
                    addDefine(technique, multiplicity, VarType.Int);
                    multiplicity = multiplicity.toUpperCase();
                    map.getLeftVariable().setMultiplicity(multiplicity);
                    //only declare the variable if the define is defined.
                    map.getLeftVariable().setCondition(mergeConditions(map.getLeftVariable().getCondition(), "defined(" + multiplicity + ")", "||"));
                } else {
                    throw new IOException("Wrong multiplicity for variable" + map.getLeftVariable().getName() + ". " + multiplicity + " should be an int or a declared material parameter.");
                }
            }
            //the right variable must have the same multiplicity and the same condition.
            var.setMultiplicity(multiplicity);
            var.setCondition(map.getLeftVariable().getCondition());
        }
    }

    /**
     * merges 2 condition with the given operator
     *
     * @param condition1 the first condition
     * @param condition2 the second condition
     * @param operator   the operator ("&&" or "||&)
     * @return the merged condition
     */
    public static String mergeConditions(String condition1, String condition2, String operator) {
        if (condition1 != null) {
            if (condition1.equals(condition2)) {
                return condition1;
            }
            if (condition2 == null) {
                return condition1;
            } else {
                String mergedCondition = "(" + condition1 + ") " + operator + " (" + condition2 + ")";
                return mergedCondition;
            }
        } else {
            return condition2;
        }
    }

    public static void addDefine(TechniqueDef techniqueDef, String paramName, VarType paramType) {
        if (techniqueDef.getShaderParamDefine(paramName) == null) {
            techniqueDef.addShaderParamDefine(paramName, paramType, paramName.toUpperCase());
        }
    }

    public static MatParam findMatParam(String varName, MaterialDef matDef) {
        for (MatParam matParam : matDef.getMaterialParams()) {
            if (varName.toLowerCase().equals(matParam.getName().toLowerCase())) {
                return matParam;
            }
        }
        return null;
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

    public static String makeConnectionKey(VariableMapping m, String techName) {
        return techName + "." + m.getLeftVariable().getNameSpace() + "." + m.getLeftVariable().getName() + "|" + m.getRightVariable().getNameSpace() + "." + m.getRightVariable().getName();
    }

    public static String makeShaderNodeKey(String techName, String nodeName ){
        return techName + "." + nodeName;
    }

    public static String makeGlobalOutKey(String techName, String variableName, String index) {
        return techName + ".Global." + variableName;
    }

    public static String makeInputKey(String techName, String nameSpace, String variableName) {
        return techName + "." + nameSpace + "." + variableName;
    }

    public static VariableMapping createVariableMapping(Dot start, Dot end){
        ShaderNodeVariable leftVariable = new ShaderNodeVariable(end.getType(), end.getNode().getName(), end.getText());
        ShaderNodeVariable rightVariable = new ShaderNodeVariable(start.getType(), start.getNode().getName(), start.getText());

        if (rightVariable.getNameSpace().equals("MatParam")) {
            rightVariable.setPrefix("m_");
        } else if (rightVariable.getNameSpace().equals("WorldParam")) {
            rightVariable.setPrefix("g_");
        }

        if(rightVariable.getType().indexOf("|")>0){
            String[] types = rightVariable.getType().split("\\|");
            for (String type : types) {
                if(leftVariable.getType().equals(type)){
                    rightVariable.setType(type);
                }
            }
        }

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

    public static void removeParam(MaterialDef matDef, String paramName) {
        try {
            Field matParams = matDef.getClass().getDeclaredField("matParams");
            matParams.setAccessible(true);
            Map<String, MatParam> params = (Map<String, MatParam>) matParams.get(matDef);
            params.remove(paramName);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
