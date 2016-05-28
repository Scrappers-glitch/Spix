package spix.app.utils;

import com.jme3.material.*;
import com.jme3.material.logic.DefaultTechniqueDefLogic;
import com.jme3.shader.*;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by bouquet on 14/05/16.
 */
public class CloneUtils {


    //this is a hack that clones the MatDef. We could add a clone() method in jme core, as this might be needed elsewhere
    //this method uses reflection to cheat the private access of some attributes.
    public static MaterialDef cloneMatDef(MaterialDef def, List<TechniqueDef> techniques) throws IllegalAccessException, NoSuchFieldException {
        MaterialDef newDef = new MaterialDef(def.getAssetManager(), def.getName());
        techniques.clear();
        newDef.setAssetName(def.getAssetName());

        //mat params
        for (MatParam matParam : def.getMaterialParams()) {
            if(matParam instanceof MatParamTexture){
                newDef.addMaterialParamTexture(matParam.getVarType(),matParam.getName(), ((MatParamTexture)matParam).getColorSpace());
            } else {
                newDef.addMaterialParam(matParam.getVarType(), matParam.getName(), matParam.getValue());
            }
        }

        //techniques
        //We can't access the techinques outside of the MaterialDef. We get it through reflection then store the techniquemaps in class
        Field techniquesFiled = newDef.getClass().getDeclaredField("techniques");
        techniquesFiled.setAccessible(true);
        Map<String, List<TechniqueDef>> techniquesMap = (Map<String, List<TechniqueDef>>)techniquesFiled.get(def);


        for (List<TechniqueDef> techniqueDefs : techniquesMap.values()) {

            for (TechniqueDef techniqueDef : techniqueDefs) {
                TechniqueDef techDef = cloneTechniqueDef(techniqueDef);
                newDef.addTechniqueDef(techDef);
                techniques.add(techDef);

            }
        }


        return newDef;
    }

    public static  TechniqueDef cloneTechniqueDef(TechniqueDef techniqueDef) throws NoSuchFieldException, IllegalAccessException {
        int sortId = (techniqueDef.getName() + UUID.randomUUID().toString()).hashCode();
        TechniqueDef techDef = new TechniqueDef(techniqueDef.getName(), sortId);
        techDef.setLightMode(techniqueDef.getLightMode());
        techDef.setShadowMode(techniqueDef.getShadowMode());
        techDef.setNoRender(techniqueDef.isNoRender());
        techDef.setShaderPrologue(techniqueDef.getShaderPrologue());
        techDef.getRequiredCaps().addAll(techniqueDef.getRequiredCaps());


//        for (Shader.ShaderType shaderType : techniqueDef.getShaderProgramLanguages().keySet()) {
//            techDef.setShaderFile(techDef.hashCode() + "", "GLSL100");
//        }

        //WorldParams
        for (UniformBinding uniformBinding : techniqueDef.getWorldBindings()) {
            techDef.addWorldParam(uniformBinding.name());
        }

        //RenderState
        if(techniqueDef.getRenderState() != null){
            techDef.setRenderState(techniqueDef.getRenderState().clone());
        }

        //ForcedRenderState
        if(techniqueDef.getForcedRenderState() != null){
            techDef.setForcedRenderState(techniqueDef.getForcedRenderState().clone());
        }

        if(techniqueDef.isUsingShaderNodes()){
            List<ShaderNode> shaderNodes = new ArrayList<>();

            for (ShaderNode shaderNode : techniqueDef.getShaderNodes()) {
                ShaderNode node = new ShaderNode(shaderNode.getName(), shaderNode.getDefinition(), shaderNode.getCondition());
                node.setInputMapping(cloneMappings(shaderNode.getInputMapping()));
                node.setOutputMapping(cloneMappings(shaderNode.getOutputMapping()));
                shaderNodes.add(node);
            }

            techDef.setShaderNodes(shaderNodes);


        } else {
            //copy defines
            //defines are painful so using reflection
            Field definesNamesField = techniqueDef.getClass().getDeclaredField("defineNames");
            definesNamesField.setAccessible(true);
            ArrayList<String> definesNames = (ArrayList<String>)definesNamesField.get(techniqueDef);
            definesNamesField.set(techDef, definesNames.clone());

            Field definesTypesField = techniqueDef.getClass().getDeclaredField("defineTypes");
            definesTypesField.setAccessible(true);
            ArrayList<VarType> definesTypes = (ArrayList<VarType>)definesTypesField.get(techniqueDef);
            definesTypesField.set(techDef, definesTypes.clone());

            Field paramToDefineIdField = techniqueDef.getClass().getDeclaredField("paramToDefineId");
            paramToDefineIdField.setAccessible(true);
            HashMap<String, Integer> paramToDefineId = (HashMap<String, Integer> )paramToDefineIdField.get(techniqueDef);
            paramToDefineIdField.set(techDef, paramToDefineId.clone());

        }


        techDef.setShaderGenerationInfo(new ShaderGenerationInfo());
        // TODO: 26/05/2016 here we should create the appropriate logic as the logic retains a reference on the techDef...
        techDef.setLogic(new DefaultTechniqueDefLogic(techDef));
        techDef.setShaderFile(techDef.hashCode() + "", techDef.hashCode() + "", "GLSL100", "GLSL100");
        return techDef;
    }

    public static List<VariableMapping> cloneMappings(List<VariableMapping> mappings) {
        List<VariableMapping> res = new ArrayList<>();
        for (VariableMapping m : mappings) {
            VariableMapping mapping = cloneVariableMapping(m);
            res.add(mapping);
        }
        return res;
    }

    public static VariableMapping cloneVariableMapping(VariableMapping m) {
        ShaderNodeVariable l = m.getLeftVariable();
        ShaderNodeVariable left = new ShaderNodeVariable(l.getType(), l.getNameSpace(), l.getName(), l.getMultiplicity());

        ShaderNodeVariable r = m.getRightVariable();
        ShaderNodeVariable right = new ShaderNodeVariable(r.getType(), r.getNameSpace(), r.getName(), r.getMultiplicity());

        return new VariableMapping(left,m.getLeftSwizzling(),right,m.getRightSwizzling(),m.getCondition());
    }


}

