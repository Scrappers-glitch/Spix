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

package spix.app.action;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.SkinningControl;
import com.jme3.animation.*;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import spix.core.*;

import java.util.*;

/**
 *  A (maybe temporary) action list that autoconfigures itself with
 *  the available animations for a spatial.
 *
 *  @author    Paul Speed
 */
public class AnimationActionList extends DefaultActionList {

    public static final String STOP_ANIM = "Stop";
    private Spatial selected;
    private ListMultimap<String, Control> controls = ArrayListMultimap.create();

    public AnimationActionList( String id ) {
        super(id);
        setEnabled(false);
    }

    public AnimationActionList( String id, String name ) {
        super(id, name);
        setEnabled(false);
    }

    public void setSelection( Object o ) {
        if( o instanceof Spatial ) {
            setSelectedSpatial((Spatial)o);
        } else {
            setSelectedSpatial(null);
        }
    }

    public void setSelectedSpatial( Spatial s ) {
        if( selected == s ) {
            return;
        }
        this.selected = s;
        setEnabled(selected != null);
        getChildren().clear();

        System.out.println("----Selected spatial:" + s);
        if( selected == null ) {
            return;
        }

        // We need to traverse up and find the spatial that was actually loaded...
        // the 'node' that contained us.  This is a bit of a hack but we keep
        // going up until we find a node with an asset key.
        for( Spatial parent = s.getParent(); parent != null; parent = parent.getParent() ) {
            if( parent.getKey() != null ) {
                s = parent;
                break;
            }
        }

        System.out.println("----Selected model:" + s);
        controls.clear();
        s.depthFirstTraversal(new AnimCollector());

        add(new AnimationAction(STOP_ANIM));
        for( String name : new TreeSet<String>(controls.keySet()) ) {
            add(new AnimationAction(name));
        }
    }

    private class AnimCollector implements SceneGraphVisitor {

        @Override
        public void visit( Spatial spatial ) {
            System.out.println("-- visiting:" + spatial);

            /*for( int i = 0; i < spatial.getNumControls(); i++ ) {
                System.out.println(" ** control:" + spatial.getControl(i));
            }*/

            Control anim = spatial.getControl(AnimComposer.class);
            if( anim == null ) {
                anim = spatial.getControl(AnimControl.class);
                if(anim == null) {
                    return;
                }
            }
            Control skel = spatial.getControl(SkinningControl.class);
            if( skel == null ) {
                skel = spatial.getControl(SkeletonControl.class);
            }
            if( skel != null ) {
                // This makes me uncomfortable because we are forcing a modification
                // to the user's loaded scene.  On the other hand, this value is currently
                // not saved with the control anyway.
                if(skel instanceof SkinningControl) {
                    ((SkinningControl)skel).setHardwareSkinningPreferred(false);
                } else {
                    ((SkeletonControl)skel).setHardwareSkinningPreferred(false);
                }
            }
            System.out.println(" *** Anim:" + anim);
            Collection<String> names = null;
            if(anim instanceof AnimComposer){
                names =((AnimComposer) anim).getAnimClipsNames();
            } else {
                names =((AnimControl) anim).getAnimationNames();
            }
            for( String s : names ) {
                System.out.println("   animation:" + s);
                if (s.trim().toLowerCase().contains("idle")) {
                    if(anim instanceof AnimComposer){
                        ((AnimComposer) anim).setCurrentAction(s);
                    } else {
                       ((AnimControl) anim).createChannel().setAnim(s);
                    }

                }
                controls.put(s, anim);
            }
        }
    }

    private class AnimationAction extends AbstractAction {
        private String animationName;

        public AnimationAction( String id ) {
            super(id);
            this.animationName = id;
        }

        public void performAction( Spix spix ) {
            System.out.println("**** Run animation:" + animationName);
            if (animationName.equals(STOP_ANIM)) {
                for (Control control : controls.values()) {
                    if(control instanceof AnimComposer){
                        ((AnimComposer) control).reset();
                    } else {
                        ((AnimControl) control).clearChannels();
                    }
                }
            }

            // Go through all of the controls that have the animation
            for( Control control : controls.get(animationName) ) {
                if(control instanceof AnimComposer){
                    AnimComposer composer = (AnimComposer) control;
                    composer.setCurrentAction(animationName);
                } else {
                    AnimControl anim = (AnimControl)control;
                    int count = anim.getNumChannels();
                    AnimChannel channel;
                    if (count > 0) {
                        // Reuse the channel from before
                        channel = anim.getChannel(0);
                    } else {
                        channel = anim.createChannel();
                    }
                    channel.setAnim(animationName);
                }
            }
        }
    }
}
