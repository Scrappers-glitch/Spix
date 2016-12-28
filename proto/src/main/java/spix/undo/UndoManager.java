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

package spix.undo;

import com.google.common.collect.MapMaker;
import com.jme3.util.clone.Cloner;
import org.slf4j.*;
import spix.core.*;
import spix.props.*;

import java.beans.*;
import java.util.*;

/**
 *  Manages the undo/redo stacks.
 *
 *  @author    Paul Speed
 */
public class UndoManager {

    static Logger log = LoggerFactory.getLogger(UndoManager.class);
    public static final String LAST_EDIT = "undoManager.lastEdit";

    private Spix spix;
    private SpixObserver spixListener = new SpixObserver();
    private EditListener editListener = new EditListener();
    private long frame;
    private long lastEditFrame;
 
    private long frameDelay = 15; // presuming we are called at 60 hz then this is .25 of a second.
                // But note the above is kind of a hack to work around the fact that the app
                // doesn't know 'dragging' state.  I mean, we'll always probably want some small
                // value depending on what we do but it would also be nice to hold the transaction
                // completely during drags.
 
    private CompositeEdit transaction = null;
 
    private LinkedList<Edit> undoStack = new LinkedList<>();
    private LinkedList<Edit> redoStack = new LinkedList<>();
 
    private Map<Property, Object> beanIndex;
 
    // Keep a cloner around to do straight-up Java clones of property values.
    private Cloner cloner = new Cloner();
 
    private boolean ignoreEvents = false;
 
    public UndoManager( Spix spix ) {
        this.spix = spix;
        this.spix.addSpixListener(spixListener);
        
        // Create a map that will let us find the original objects
        // that a property belongs to for when we receive events
        beanIndex = new MapMaker().weakKeys().weakValues().makeMap();
    }
 
    protected CompositeEdit getTransaction( boolean create ) {
        if( transaction == null && create ) {
            transaction = new CompositeEdit();
            undoStack.addFirst(transaction);
        }
        return transaction;
    }
 
    public void addEdit( Edit edit ) {
        getTransaction(true).addEdit(edit);
        lastEditFrame = frame;
        redoStack.clear();
        spix.getBlackboard().set(LAST_EDIT, transaction);
    }
 
    public void undo() {
        log.info("undo()");    
        if( undoStack.isEmpty() ) {
            log.info("...undo stack empty");    
            return;
        }
        Edit edit = undoStack.removeFirst();
        if( edit == transaction ) {
            // Then we can't continue that transaction, regardless
            // of frame states
            transaction = null;            
        }
        
        // We don't want to record the events from running the edits
        // as we've already recorded them.
        ignoreEvents = true;
        try {
            edit.undo(spix);
            //That's bad...but that's to force the change and trigger the events when the edit is the same as before...
            spix.getBlackboard().set(LAST_EDIT, null);
            spix.getBlackboard().set(LAST_EDIT, edit);
        } finally { 
            ignoreEvents = false;
        }
        redoStack.addFirst(edit);
    }
    
    public void redo() {
        log.info("redo()");    
        if( redoStack.isEmpty() ) {
            log.info("...redo stack empty");    
            return;
        }
        Edit edit = redoStack.removeFirst();
        // We don't want to record the events from running the edits
        // as we've already recorded them.
        ignoreEvents = true;
        try {
            edit.redo(spix);
            //That's bad...but that's to force the change and trigger the events when the edit is the same as before...
            spix.getBlackboard().set(LAST_EDIT, null);
            spix.getBlackboard().set(LAST_EDIT, edit);
        } finally {
            ignoreEvents = false;
        }
        undoStack.addFirst(edit);
    }

    /**
     *  Advances the internal frame counter and starts a new 
     *  transaction context if needed. 
     */   
    public void nextFrame() {
        frame++;
        if( frame > lastEditFrame + frameDelay ) {
            // The last transaction is 'done'
            transaction = null;
        }
    }

    protected Object clonePropertyValue( Object value ) {
        try {
            return cloner.javaClone(value);
        } catch( CloneNotSupportedException e ) {
            return value; // don't clone it then
        }
    } 

    protected void addEdit( Object source, String property, Object oldValue, Object newValue ) {
        log.info("addEdit(" + source + ", " + property + ", " + oldValue + ", " + newValue + ")");
        Object original = beanIndex.get(source);
        oldValue = clonePropertyValue(oldValue);        
        newValue = clonePropertyValue(newValue);        
        log.info("original:" + original);
        if( original != null ) {
            // If the old value and the new value are equivalent then we should
            // not create an edit because it doesn't mean anything
            if( Objects.equals(oldValue, newValue) ) {
                log.info("ignoring edit:" + original + ", " + property + ", " + oldValue + ", " + newValue); 
            } else {
                addEdit(new PropertyEdit(original, property, oldValue, newValue));
            }
        }    
    }

    private class SpixObserver implements SpixListener {
        
        public PropertySet propertySetCreated( Object wrapped, PropertySet newSet ) {
 
            log.info("propertySetCreated(" + wrapped + ", " + newSet + ")");
            
            // For now, let's try to just use listeners.
            for( Property prop : newSet ) {
                prop.addPropertyChangeListener(editListener);
                beanIndex.put(prop, wrapped);
            }
        
            return newSet;
        }        
    }
    
    private class EditListener implements PropertyChangeListener {
        public void propertyChange( PropertyChangeEvent event ) {
            if( ignoreEvents ) {
                return;
            }
            addEdit(event.getSource(), event.getPropertyName(), event.getOldValue(), event.getNewValue());
        }
    }
    
}
