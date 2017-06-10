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

package spix.app.form;

import com.jme3.material.MaterialDef;
import spix.app.material.MatParamProperty;
import spix.app.utils.IconPath;
import spix.app.metadata.MetadataService;
import spix.core.Spix;
import spix.form.*;
import spix.props.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 *  @author    Paul Speed
 */
public class SpatialFormFactory extends DefaultFormFactory {

    private static final Logger logger = Logger.getLogger(SpatialFormFactory.class.getName());

    private static final Set<String> DEFAULT_XFORMS = new HashSet<>();
    private static final Set<String> LOCAL_XFORMS = new HashSet<>();
    private static final Set<String> WORLD_XFORMS = new HashSet<>();
    private static final Set<String> TREANSPARENCY = new HashSet<>();
    private static final Set<String> RENDER = new HashSet<>();
    private static final Set<String> DEPTH = new HashSet<>();
    private static final Set<String> POLYOFFSET = new HashSet<>();
    private static final Set<String> STENCIL = new HashSet<>();

    static {
        LOCAL_XFORMS.add("localTranslation");
        LOCAL_XFORMS.add("localScale");
        LOCAL_XFORMS.add("localRotation");
        WORLD_XFORMS.add("worldTranslation");
        WORLD_XFORMS.add("worldScale");
        WORLD_XFORMS.add("worldRotation");
        DEFAULT_XFORMS.add("name");
        DEFAULT_XFORMS.add("cullHint");
        DEFAULT_XFORMS.add("queueBucket");

        TREANSPARENCY.add("blendMode");
        TREANSPARENCY.add("blendEquation");
        TREANSPARENCY.add("blendEquationAlpha");

        RENDER.add("colorWrite");
        RENDER.add("faceCullMode");
        RENDER.add("wireframe");
        RENDER.add("lineWidth");

        DEPTH.add("depthWrite");
        DEPTH.add("depthTest");
        DEPTH.add("depthFunc");

        POLYOFFSET.add("offsetFactor");
        POLYOFFSET.add("offsetUnits");

        STENCIL.add("stencilTest");

        STENCIL.add("frontStencilFunction");
        STENCIL.add("backStencilFunction");
        STENCIL.add("frontStencilStencilFailOperation");
        STENCIL.add("frontStencilDepthFailOperation");
        STENCIL.add("frontStencilDepthPassOperation");
        STENCIL.add("backStencilStencilFailOperation");
        STENCIL.add("backStencilDepthFailOperation");
        STENCIL.add("backStencilDepthPassOperation");
        STENCIL.add("sfactorRGB");
        STENCIL.add("dfactorRGB");
        STENCIL.add("sfactorAlpha");
        STENCIL.add("dfactorAlpha");


    }

    public SpatialFormFactory() {
    }

    @Override
    public Form createForm( Spix spix, PropertySet properties, String context ) {

        // We basically want the default behavior except we will pull out the
        // world transforms into their own form and the local transforms into
        // their own form.
        
        Form localTrans = new Form();        
        Form worldTrans = new Form();
        Form materialForm = new Form();


        Form renderStateForm = new Form();
        Form transparencyForm = new Form();
        Form renderForm = new Form();
        Form depthForm = new Form();
        Form polyOffsetForm = new Form();
        Form stencilForm = new Form();
        
        Form result = new Form();

        for( Property property : properties ) {
 
            Field field = createField(spix, property, context);
                       
            if( LOCAL_XFORMS.contains(property.getId()) ) {
                localTrans.add(field);
            } else if( WORLD_XFORMS.contains(property.getId()) ) {
                worldTrans.add(field);
            } else if (DEFAULT_XFORMS.contains(property.getId())) {
                result.add(field);
            } else if (TREANSPARENCY.contains(property.getId())) {
                transparencyForm.add(field);
            } else if (RENDER.contains(property.getId())) {
                renderForm.add(field);
            } else if (DEPTH.contains(property.getId())) {
                depthForm.add(field);
            } else if (POLYOFFSET.contains(property.getId())) {
                polyOffsetForm.add(field);
            } else if (STENCIL.contains(property.getId())) {
                stencilForm.add(field);
            }

        }

        // After all of that, stick the subforms at the bottom
        result.add(new FormField("Local Transform", localTrans, IconPath.attrib));
        result.add(new FormField("World Transform", worldTrans, IconPath.world));
        Property an = properties.getProperty("matDef");
        if (an != null) {

            Field name = createField(spix, properties.getProperty("materialName"), context);
            materialForm.add(name);

            Field key = createField(spix, properties.getProperty("j3m File"), context);
            materialForm.add(key);

            Field assetName = createField(spix, an, context);
            materialForm.add(assetName);

            MaterialDef def = (MaterialDef) an.getValue();

            Map<String, Object> configMap = spix.getService(MetadataService.class).getMetadata(def);
            ArrayList params = (ArrayList) configMap.get("ParamsLayout");

            if (params == null) {
                //no meta data config, let's try to sort things up a bit.
                //let's sort by type, and by names then make a group for each type.
                handleNoConfig(properties, materialForm, spix, context);

            } else {
                try {
                    handleConfig(params, properties, materialForm, null, spix, context, def.getAssetName());
                } catch (Exception e) {
                    //catch anything that could go wrong (this might be a user input at some point) and fall back to the noConfig layout.
                    logger.log(Level.SEVERE, "Failed to setup property layout for materialForm definition " + an.getValue());
                    e.printStackTrace();
                    handleNoConfig(properties, materialForm, spix, context);
                }
            }
            result.add(new FormField("Material", materialForm, IconPath.material));

            //renderstate
            renderStateForm.add(new FormField("Render", renderForm));
            renderStateForm.add(new FormField("Depth", depthForm));
            renderStateForm.add(new FormField("Transparency", transparencyForm));
            renderStateForm.add(new FormField("Polygon Offset", polyOffsetForm));
            //renderStateForm.add(new FormField("Stencil", stencilForm));
            result.add(new FormField("Render State", renderStateForm));
        }

        return result;
    }

    private boolean isMatParamProperty(Property prop) {
        if (prop instanceof PropertyWrapper) {
            PropertyWrapper wrapper = (PropertyWrapper) prop;
            if (wrapper.getDelegateProperty() instanceof MatParamProperty) {
                return true;
            }
            return false;
        } else if (prop instanceof MatParamProperty) {
            return true;
        }
        return false;
    }

    public void handleNoConfig(PropertySet properties, Form materialForm, Spix spix, String context) {

        List<Property> matParams = new ArrayList<>();
        for (Property property : properties) {
            if (isMatParamProperty(property)) {
                matParams.add(property);
            }
        }

        matParams.sort(new Comparator<Property>() {
            @Override
            public int compare(Property o1, Property o2) {
                String name1 = o1.getType().getJavaType().getSimpleName();
                String name2 = o2.getType().getJavaType().getSimpleName();

                int result = name1.compareTo(name2);
                if (result == 0) {
                    return o1.getName().compareTo(o2.getName());
                }
                return result;
            }
        });

        Class curType = null;
        String groupName = "";
        Form currForm = new Form();
        for (Property matParam : matParams) {
            if (matParam.getType().getJavaType() != Object[].class) {
                if (curType == null) {
                    curType = matParam.getType().getJavaType();
                    groupName = curType.getSimpleName();
                }
                if (matParam.getType().getJavaType() != curType) {
                    FormField group = new FormField(groupName, currForm);
                    materialForm.add(group);
                    currForm = new Form();
                    curType = matParam.getType().getJavaType();
                    groupName = curType.getSimpleName();
                }
                Field field = createField(spix, matParam, context);
                currForm.add(field);
            }
        }
        if (currForm.size() > 0) {
            FormField group = new FormField(groupName, currForm);
            materialForm.add(group);
        }
    }

    private void handleConfig(ArrayList list, PropertySet properties, Form materialForm, String name, Spix spix, String context, String matDef) {

        Form currForm = name == null ? materialForm : new Form();
        for (Object entry : list) {
            String field;
            if (entry instanceof String) {
                field = (String) entry;
                Property prop = properties.getProperty(field);
                if (prop != null) {
                    Field propField = createField(spix, prop, context);
                    currForm.add(propField);
                } else {
                    logger.log(Level.WARNING, "Material parameter {0} doesn't exist for material definition {1}", new Object[]{
                            field, matDef
                    });
                }
            } else {

                if (entry instanceof Map) {
                    //we encounter a named group let's layout existing props
                    if (currForm.size() > 0 && currForm != materialForm) {
                        FormField group = new FormField(name, currForm);
                        materialForm.add(group);
                    }
                    name = null;

                    Map group = (Map) entry;
                    for (Object o : group.keySet()) {
                        Object obj = group.get(o);
                        if (obj instanceof ArrayList) {
                            handleConfig((ArrayList) obj, properties, materialForm, o.toString(), spix, context, matDef);
                        }
                    }
                }
            }
        }
        if (currForm.size() > 0 && currForm != materialForm) {
            FormField group = new FormField(name, currForm);
            materialForm.add(group);
        }
    }
}
