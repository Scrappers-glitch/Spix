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

package spix.core;

import java.beans.*;

/**
 *
 *
 *  @author    Paul Speed
 */
public class SpixTest {

    public static void main( String... args ) {
    
        Spix spix = new Spix();
        
        Action myAction1 = new Action("foo") {
                public void performAction( final Spix spix ) {
                    System.out.println("Do it! " + this);
                }
            };

        Action myAction2 = new Action("fooBar") {
                public void performAction( final Spix spix ) {
                    System.out.println("Do it! " + this);
                }
            };

        Action myAction3 = new Action("IDTest") {
                public void performAction( final Spix spix ) {
                    System.out.println("Do it! " + this);
                }
            };
 
        Action myAction4 = new Action("testTLA") {
                public void performAction( final Spix spix ) {
                    System.out.println("Do it! " + this);
                }
            };
            
        System.out.println("My action1:" + myAction1);
        System.out.println("My action2:" + myAction2);
        System.out.println("My action3:" + myAction3);
        System.out.println("My action4:" + myAction4);
        
        myAction1.performAction(spix);
        
        myAction1.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent event ) {
                    System.out.println("propertyChange:" + event);
                }
            });
            
        myAction1.setEnabled(false);
        myAction1.setEnabled(false);
        myAction1.setEnabled(true);
        myAction1.put("custom", 123);
        
        ActionList rootActions = new ActionList("main");
        rootActions.add(myAction1);
        rootActions.add(myAction2);
        
        ActionList subActions = rootActions.add(new ActionList("Sublist"));
        subActions.add(myAction3);
        subActions.add(myAction4);
        
        ActionUtils.debugDump("", rootActions);
    }
}
