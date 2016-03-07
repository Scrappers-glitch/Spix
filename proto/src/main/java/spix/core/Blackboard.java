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
import java.util.*;

import groovy.util.ObservableMap;

import spix.type.*;

/**
 *
 *
 *  @author    Paul Speed
 */
public class Blackboard {
 
    //private TypeRegistry<ObjectHandler> handlers = new TypeRegistry<>();
 
    private ObservableMap root = new ObservableMap();
    
    public Blackboard() {
        //root.addPropertyChangeListener(new NodeListener(""));
        
        //handlers.register(ObservableMap.class, new ObservableMapHandler());
        //handlers.register(ActionList.class, new ActionListHandler());
    }
        
    public void set( String path, Object value ) {
        root.put(path, value);   
    }
    
    public Object get( String path ) {
        return root.get(path);
    }
 
    /*private class Node implements PropertyChangeListener {
        private String path;
        private Object object;
        private ObjectHandler handler;
        private Map<String, Node> children;
        
    }*/
 
/*    
    private class NodeListener implements PropertyChangeListener {
        private String path;
        private Map<Object, NodeListener> childListeners = new HashMap<>();        
        
        public NodeListener( String path ) {
            this.path = path;
        }
 
        protected void detach( String name, Object o ) {
            if( o == null ) {
                return;
            }
            // Removal here is actually bad if the object exists multiple
            // times as a child. 
            NodeListener l = childListeners.remove(o);
            if( l == null ) {
                return;
            }
            ObjectHandler handler = handlers.get(o.getClass(), false);
            if( handler == null ) {
                throw new RuntimeException("Somehow have a node listener for an object with no handler.");                
            }
            handler.removePropertyChangeListener(o, l);
        }
        
        protected void attach( String name, Object o ) {
            if( o == null ) {
                return;
            }
            ObjectHandler handler = handlers.get(o.getClass(), false);
            if( handler == null ) {
                return;
            }
            NodeListener l = childListeners.get(o);
            if( l == null ) {
                l = new NodeListener(path + "." + name);
            }
            handler.addPropertyChangeListener(o, l);
        }
        
        public void propertyChange( PropertyChangeEvent event ) {
            System.out.println( "change at path:" + path + " " + event );
            
            detach(event.getPropertyName(), event.getOldValue());
            attach(event.getPropertyName(), event.getNewValue());
        }
    }
    
    private interface ObjectHandler<T> {
        public void addPropertyChangeListener( T object, PropertyChangeListener l );
        public void removePropertyChangeListener( T object, PropertyChangeListener l );
        public Object getChild( T object, String name );
        public Iterable getChildren( T object );
    }
    
    private class ObservableMapHandler implements ObjectHandler<ObservableMap> {
        public void addPropertyChangeListener( ObservableMap object, PropertyChangeListener l ) {
            object.addPropertyChangeListener(l);   
        }
        
        public void removePropertyChangeListener( ObservableMap object, PropertyChangeListener l ) {
            object.removePropertyChangeListener(l);   
        }
        
        public Object getChild( ObservableMap object, String name ) {
            return null;
        }
        
        public Iterable getChildren( ObservableMap object ) {
            return Collections.emptyList();
        } 
    }
    
    private class ActionListHandler implements ObjectHandler<ActionList> {
        public void addPropertyChangeListener( ActionList object, PropertyChangeListener l ) {
            object.addPropertyChangeListener(l);   
        }
        
        public void removePropertyChangeListener( ActionList object, PropertyChangeListener l ) {
            object.removePropertyChangeListener(l);   
        }
        
        public Object getChild( ActionList object, String name ) {
            return null;
        }
        
        public Iterable getChildren( ActionList object ) {
            return object.getChildren();
        } 
    }*/
}
