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

/**
 *
 *
 *  @author    Paul Speed
 */
public class TypeRegistry<V> {
 
    private Map<Type, V> map = new ConcurrentHashMap<>();
    private Map<Type, List<V>> allCache = new ConcurrentHashMap<>();
    private Map<Type, List<Type>> paths = new ConcurrentHashMap<>();  
 
    public TypeRegistry() {
    }
 
    /**
     *  Associates the specified value with the specified type.
     */    
    public void register( Type key, V value ) {
        if( !map.containsKey(key) ) {
            // We're invalidating our path cache... we could actually
            // be smarter about how we prune it but this will work 100%
            // of the time.  We can optimize later and prune only the
            // invalidated paths.
            paths.clear();
            
            // And because of that we are also invalidating the value
            // cache.  Same optimization comment applies.
            allCache.clear();
        }
        map.put(key, value);
    }
 
    /**
     *  Returns all of the registered values in the type hierarchy for the specified
     *  type.
     */
    public List<V> getAll( Type key ) {
    
        List<V> results = allCache.get(key);
        if( results != null ) {
            return results;
        }
    
        List<Type> path = getPath(key, true);

        results = new ArrayList<>();
        for( Type t : path ) {
            V value = map.get(t);
            if( value != null ) {
                // Note: leave duplicate checking up to the caller
                results.add(value);
            }
        }       
 
        // Cache it for next time
        allCache.put(key, results);
 
        return results;        
    }
    
    /**
     *  Returns an exact value for the key or the first most relevant
     *  value in the type hierarchy, depending on how the 'exact' flag is
     *  set.
     */
    public V get( Type key, boolean exact ) {
        V result = map.get(key);
        if( exact || result != null ) {
            return result;
        }
        
        // Else we need to look it up... just reuse the all
        // call which is likely cached.
        List<V> all = getAll(key);
        if( all.isEmpty() ) {
            return null;
        }       
        return all.get(0);
    }
     
    protected List<Type> buildPath( Type type ) {
    
        // Basic theory is that we can sort based on the earliest
        // place a type is assignableFrom().  That is the most
        // general place for that type in the list of types.
        // We only care about a path of the types we already have
        // and not all possible supertypes of 'type'.
        List<Type> result = new ArrayList<>();
        result.add(type);
        
        // Note: when sparsely assigned interfaces are involved then
        // one pass through the map produce inconsistent results depending 
        // on the order of the map.  I think this will always be true
        // even for multiple passes but at least we'll limit the ordering
        // inconsistencies to the interface-to-interface sorting.
        // By doing the classes first, we establish an absolute ordering
        // into which we can more accurately thread the interfaces.  Otherwise
        // if a super class was encountered after an interface then it might
        // push it earlier in the list.
        
        // Pass one for classes        
        for( Type t : map.keySet() ) {
        
            // Skip interfaces
            if( t.getJavaType().isInterface() ) {
                continue;
            }
 
            // Is it a super type of 'type' at all?
            if( !t.isAssignableFrom(type) ) {
                // Then it's not even part of the path
                continue;
            }
            
            // If it's the original type then we need to skip it
            if( Objects.equals(t, type) ) {
                continue;
            }
        
            // Else find the earliest it can exist and still be
            // a supertype of the some of the path
            for( int i = 0; i < result.size(); i++ ) {
                Type e = result.get(i);
                if( t.isAssignableFrom(e) ) {
                    result.add(i, t);
                    break;
                } 
            }
        }

        // Pass two for interfaces
        for( Type t : map.keySet() ) {
            // Skip classes
            if( !t.getJavaType().isInterface() ) {
                continue;
            }
        
            // Is it a super type of 'type' at all?
            if( !t.isAssignableFrom(type) ) {
                // Then it's not even part of the path
                continue;
            }
            
            // If it's the original type then we need to skip it
            if( Objects.equals(t, type) ) {
                continue;
            }
        
            // Else find the earliest it can exist and still be
            // a supertype of the some of the path
            for( int i = 0; i < result.size(); i++ ) {
                Type e = result.get(i);
                if( t.isAssignableFrom(e) ) {
                    result.add(i, t);
                    break;
                } 
            }
        }
        
        // Reverse the list so that it's best to worst
        Collections.reverse(result);
        
        return result;       
    }
 
    protected List<Type> getPath( Type key, boolean create ) {
        List<Type> result = paths.get(key);
        if( result == null && create ) {
            result = buildPath(key);
            paths.put(key, result);
        }
        return result; 
    }
 
}
