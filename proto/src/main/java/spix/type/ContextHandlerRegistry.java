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

import java.util.*;
import java.util.concurrent.*;

import org.slf4j.*;

/**
 *  A handler registry that also supports a hierarchy of 
 *  string-based 'contexts'.  In other words, instead of asking
 *  for a global handler for some type, a caller can ask for the
 *  handler for 'lists' or the handler for 'tables' and potentially
 *  get a more specific handler.  A dotted notation can be used to
 *  create a hierarchy with "" being the root, or default.  So
 *  "list.actions" is more specific than "lists", and so on.
 *
 *  @author    Paul Speed
 */
public class ContextHandlerRegistry<V> {
 
    static Logger log = LoggerFactory.getLogger(ContextHandlerRegistry.class);
 
    public static final String DEFAULT = "(default)";
       
    private Map<String, HandlerRegistry<V>> registries = new ConcurrentHashMap<>();

    public ContextHandlerRegistry() {
    }
    
    public HandlerRegistry<V> getRegistry( String context ) {
        return getRegistry(context, true);
    }
    
    public HandlerRegistry<V> getRegistry( String context, boolean create ) {
        if( context == null ) {
            context = DEFAULT;
        }
        HandlerRegistry<V> result = registries.get(context);
        if( result == null && create ) {
            synchronized( this ) {
                // Make sure we only create one new registry... and handle 
                // the case where we already created one.  Double checked locking
                // is fine here because the data structure itself is thread safe.
                result = registries.get(context);
                if( result == null ) {
                    result = new HandlerRegistry<>();
                    registries.put(context, result);
                }
            }   
        }
        return result;   
    }
    
    public V getHandler( String context, Class type, boolean exact ) {
        return getHandler(context, new Type(type), exact);
    }
    
    public V getHandler( String context, Type type, boolean exact ) {
        if( log.isTraceEnabled() ) {
            log.trace("getHandler(" + context + ", " + type + ", " + exact + ")");    
        }
        for( String s : getSearchList(context) ) {
            if( log.isTraceEnabled() ) {
                log.trace("   trying context:" + s);
            }        
            HandlerRegistry<V> registry = getRegistry(s, false);
            if( registry == null ) {
                continue;
            }
            V result = registry.get(type, exact);
            if( result != null ) {
                return result;
            }            
        }
        return null;   
    }
    
    protected List<String> getSearchList( String context ) {
        List<String> results = new ArrayList<>();
        
        // The full path is always the first to search
        results.add(context);
        
        for( int split = context.lastIndexOf("."); split > 0; split = context.lastIndexOf(".", split-1) ) {
            results.add(context.substring(0, split));
        }
        
        results.add(DEFAULT);
        return results; 
    }    
}
