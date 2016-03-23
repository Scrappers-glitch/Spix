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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.*;

import groovy.util.ObservableList;

/**
 *  A special kind of observable list that exposes single
 *  selection as a special state of multi-selection.
 *
 *  @author    Paul Speed
 */
public class SelectionModel extends ObservableList {

    private Object singleSelection;

    // Until we can bind to bean properties through blackboard... we'll
    // do this cheat.
    private Blackboard blackboard;
    private String target;

    public SelectionModel() {
        super(new CopyOnWriteArrayList());
    }

    public void setSingleSelection( Object single ) {
        if( Objects.equals(singleSelection, single) ) {
            return;
        }

        if( size() == 1 ) {
            // Just swap it out
            set(0, single);
        } else if( size() == 0 ) {
            // Just add it
            add(single);
        } else {
            clear();
            add(single);
        }

        // The singleSelection field gets updated as a side-effect of the list change.
    }

    public Object getSingleSelection() {
        return singleSelection;
    }

    protected void fireSizeChangedEvent( int oldValue, int newValue ) {
        super.fireSizeChangedEvent(oldValue, newValue);
        updateSingleSelection(newValue == 1 ? get(0) : null);
    }

    protected void fireElementUpdatedEvent( int index, Object oldValue, Object newValue ) {
        super.fireElementUpdatedEvent(index, oldValue, newValue);
        updateSingleSelection(size() == 1 ? get(0) : null);
        // Note: this means that general listeners get an extra event.
    }

    protected void updateSingleSelection( Object o ) {
        Object old = singleSelection;
        this.singleSelection = o;
        firePropertyChange("singleSelection", old, o);
        blackboard.set(target, getSingleSelection());
    }

    public void setupHack( Blackboard bb, String target ) {
        // A hack that will hook into the single selection stuff to
        // set an external property.  This is a hack to work around the
        // fact that blackboard won't (yet) let you listen to nested bean
        // properties... only its own properties.
        this.blackboard = bb;
        this.target = target;
        blackboard.set(target, getSingleSelection());
    }

    protected void firePropertyChange( String name, Object oldValue, Object newValue ) {

        if( Objects.equals(oldValue, newValue) ) {
            return;
        }

        PropertyChangeListener[] all = getPropertyChangeListeners();

        PropertyChangeEvent event = new PropertyChangeEvent(this, name, oldValue, newValue);
        for( PropertyChangeListener l : all ) {
            l.propertyChange(event);
        }

        /*
        It seems that a proxy is registered with the 'all' list that will
        automatically delegate to the specific listeners.
        PropertyChangeListener[] specific = getPropertyChangeListeners(name);
        for( PropertyChangeListener l : specific ) {
            l.propertyChange(event);
        }*/
    }

    @Override
    public String toString() {
        return String.valueOf(getDelegateList());
    }
}
