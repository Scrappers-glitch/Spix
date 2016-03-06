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

package spix.type;

/**
 *
 *
 *  @author    Paul Speed
 */
public class TypeRegistryTest {

    public static void main( String... args ) {
        System.out.println("Type registry test...");
 
        Type myInterface = new Type(MyInterface.class);       
        Type myOtherInterface = new Type(MyOtherInterface.class);
        Type parent = new Type(Parent.class);
        Type child = new Type(Child.class);
        Type sibling = new Type(Sibling.class);
        Type grandchild = new Type(Grandchild.class);
        Type cloneable = new Type(Cloneable.class);
        
        TypeRegistry<String> registry = new TypeRegistry<>();
        registry.register(child, "born");
        registry.register(myInterface, "mine");
        registry.register(myOtherInterface, "myOther");
        registry.register(cloneable, "cloneable");
        
        System.out.println("grandchild single exact match:" + registry.get(grandchild, true));
        System.out.println("grandchild single:" + registry.get(grandchild, false));
        System.out.println("grandchild all:" + registry.getAll(grandchild));

        System.out.println("child single exact match:" + registry.get(child, true));
        System.out.println("child single:" + registry.get(child, false));
        System.out.println("child all:" + registry.getAll(child));
 
        System.out.println("sibling single exact match:" + registry.get(sibling, true));
        System.out.println("sibling single:" + registry.get(sibling, false));
        System.out.println("sibling all:" + registry.getAll(sibling));
 
        System.out.println("---- adding a parent value...");
        registry.register(parent, "parent");
        
        System.out.println("grandchild single exact match:" + registry.get(grandchild, true));
        System.out.println("grandchild single:" + registry.get(grandchild, false));
        System.out.println("grandchild all:" + registry.getAll(grandchild));
 
        System.out.println("child single exact match:" + registry.get(child, true));
        System.out.println("child single:" + registry.get(child, false));
        System.out.println("child all:" + registry.getAll(child));
        
        System.out.println("sibling single exact match:" + registry.get(sibling, true));
        System.out.println("sibling single:" + registry.get(sibling, false));
        System.out.println("sibling all:" + registry.getAll(sibling));
    }
    
    static interface MyInterface {
    } 

    static interface MyOtherInterface {
    } 

    static class Parent {
    }
 
    static class Child extends Parent 
                        implements MyInterface {
    }

    static class Sibling extends Parent 
                         implements Cloneable {
    }
    
    static class Grandchild extends Child 
                            implements MyOtherInterface, Cloneable {
    }
    
}
