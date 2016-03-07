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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import groovy.util.ObservableList;

import com.google.common.base.MoreObjects;

/**
 *
 *
 *  @author    Paul Speed
 */
public class DefaultActionList extends AbstractAction 
                               implements ActionList {
    
    private final ObservableList children = new ObservableList(new CopyOnWriteArrayList()); 
    
    public DefaultActionList( String id ) {
        super(id);
    }
    
    public DefaultActionList( String id, String name ) {
        super(id, name);
    }
 
    public void addPropertyChangeListener( PropertyChangeListener l ) {
        super.addPropertyChangeListener(l);
        children.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener( PropertyChangeListener l ) {
        super.removePropertyChangeListener(l);
        children.removePropertyChangeListener(l);
    }
 
    public <T extends Action> T add( T child ) {
        children.add(child);
        return child;
    }
    
    public void remove( Action child ) {
        children.remove(child);
    }
    
    public Iterator<Action> iterator() {
        return (Iterator<Action>)children.iterator();
    }
    
    public void performAction( Spix spix ) {
        // Do nothing by default
    }
    
    public List<Action> getChildren() {
        return (List<Action>)children;
    }
    
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getName())
                .add("id", getId())
                .add("properties", entrySet())
                .add("children", getChildren())
                .toString();       
    }
}
