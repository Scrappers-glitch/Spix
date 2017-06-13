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
package spix.app.material;

import com.jme3.material.MatParam;
import com.jme3.material.plugins.ConditionParser;
import com.sun.org.apache.bcel.internal.generic.NEW;
import spix.app.material.hack.MatDefWrapper;
import spix.core.Blackboard;
import spix.core.Spix;
import spix.props.AbstractProperty;
import spix.swing.materialEditor.utils.MaterialDefUtils;
import spix.type.Type;

import java.lang.reflect.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bouquet on 05/10/16.
 */
public class ShaderNodeConditionProperty extends AbstractProperty {

    private Object parent;
    private ConditionParser parser = new ConditionParser();
    private Blackboard blackboard;

    public ShaderNodeConditionProperty(Object parent, String name, Spix spix) {
        super(name);
        this.parent = parent;
        this.blackboard = spix.getBlackboard();
    }

    @Override
    public Type getType() {
        return new Type<>(String.class);
    }

    @Override
    public void setValue(Object value) {
        Object old = getValue();

        String newVal = (String) value;
        if (newVal.equals("")) {
            newVal = null;
        } else {
            parser.extractDefines(newVal);
            newVal = parser.getFormattedExpression();
        }
        try {
            Method m = parent.getClass().getDeclaredMethod("setCondition", String.class);
            m.invoke(parent, newVal);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        firePropertyChange(old, value, false);
    }


    @Override
    public Object getValue() {
        String val = "";
        try {
            Method method = parent.getClass().getDeclaredMethod("getCondition");
            val = (String) method.invoke(parent);
            if (val == null) {
                val = "";
            } else {
                MatDefWrapper wrapper = blackboard.get("matdDefEditor.selection.matdef.singleSelect", MatDefWrapper.class);
                val = val.replaceAll("#ifdef", "").replaceAll("#if", "").replaceAll("defined", "");
                Pattern pattern = Pattern.compile("(\\(\\w+\\))");

                Matcher m = pattern.matcher(val);

                while (m.find()) {
                    String match = m.group();
                    String rep = match.replaceAll("[\\(\\)]", "");
                    MatParam param = MaterialDefUtils.findMatParam(rep, wrapper.getMaterialDef());
                    if (param == null) {
                        return val;
                    }
                    val = val.replaceAll("\\(" + rep + "\\)", param.getName());
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return val;
    }

}
