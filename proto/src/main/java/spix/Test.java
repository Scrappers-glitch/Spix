/*
 * $Id$
 * 
 * Copyright (c) 2015, Simsilica, LLC
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

package spix;

import com.jme3.math.*;
import com.jme3.scene.*;

/**
 *  Just playing around with some prototypes.
 *
 *  @author    Paul Speed
 */
public class Test {

    public static void main( String... args ) {
 
        //Node test = new Node();
        
        Node node = new Node();
        Vector3f v = node.getLocalTranslation();
        
        Property localTrans = BeanProperty.create(node, "localTranslation");
        
        Property prop1 = BeanProperty.create(v, "x");
        Property prop2 = BeanProperty.create(v, "y");
        Property prop3 = BeanProperty.create(v, "z");
 
        System.out.println("prop1:" + prop1);
        
        System.out.println("v:" + v + "   world:" + node.getWorldTranslation());
        prop1.setValue(123);
        System.out.println("v:" + v + "   world:" + node.getWorldTranslation());
        prop2.setValue(456);
        prop3.setValue(78.9f);
        System.out.println("v:" + v + "   world:" + node.getWorldTranslation());

        PropertySet props = new DefaultPropertySet(localTrans, v, prop1, prop2, prop3);
        System.out.println("props:" + props);
        
        props.getProperty("x").setValue(1);                
        props.getProperty("y").setValue(2);                
        props.getProperty("z").setValue(3);                
        System.out.println("v:" + v + "   world:" + node.getWorldTranslation());
        
        Property localRotation = BeanProperty.create(node, "localRotation");
        Quaternion quat = node.getLocalRotation();
        PropertySet rotation = ImmutableObjectPropertySet.create(localRotation,
                                        new DefaultProperty("x", Float.TYPE, quat.getX()),
                                        new DefaultProperty("y", Float.TYPE, quat.getY()),
                                        new DefaultProperty("z", Float.TYPE, quat.getZ()),
                                        new DefaultProperty("w", Float.TYPE, quat.getW()));
        System.out.println("local rot:" + node.getLocalRotation());
        rotation.getProperty("x").setValue(1);
        rotation.getProperty("y").setValue(2);
        rotation.getProperty("z").setValue(3);
        rotation.getProperty("w").setValue(4);
        System.out.println("local rot:" + node.getLocalRotation());
        
        // Try it without having to recreate the quaternion
        rotation = CompositePropertySet.create(localRotation,
                                        MethodCompositeMutator.create(Quaternion.class, "set",
                                                              Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE),
                                        new DefaultProperty("x", Float.TYPE, quat.getX()),
                                        new DefaultProperty("y", Float.TYPE, quat.getY()),
                                        new DefaultProperty("z", Float.TYPE, quat.getZ()),
                                        new DefaultProperty("w", Float.TYPE, quat.getW()));

        System.out.println("local rot:" + node.getLocalRotation());
        rotation.getProperty("x").setValue(5);
        rotation.getProperty("y").setValue(6);
        rotation.getProperty("z").setValue(7);
        rotation.getProperty("w").setValue(8);
        System.out.println("local rot:" + node.getLocalRotation());
 
        quat.set(0, 0, 0, 1);
        System.out.println("local rot:" + node.getLocalRotation());
 
        System.out.println("Testing setting values as euler angles...");                                          
        PropertySet euler = CompositePropertySet.create(localRotation,
                                        MethodCompositeMutator.create(Quaternion.class, "fromAngles",
                                                              Float.TYPE, Float.TYPE, Float.TYPE),
                                        new DefaultProperty("x", Float.TYPE, quat.toAngles(null)[0]),
                                        new DefaultProperty("y", Float.TYPE, quat.toAngles(null)[1]),
                                        new DefaultProperty("z", Float.TYPE, quat.toAngles(null)[2]));
        System.out.println("local rot:" + node.getLocalRotation());
        euler.getProperty("x").setValue(1);
        System.out.println("local rot:" + node.getLocalRotation());
        euler.getProperty("y").setValue(2);
        System.out.println("local rot:" + node.getLocalRotation());
        euler.getProperty("z").setValue(3);
        System.out.println("local rot:" + node.getLocalRotation());

        // Well... one problem we still need to solve with the composite sets is
        // refreshing their values from the original object.

    }
}
