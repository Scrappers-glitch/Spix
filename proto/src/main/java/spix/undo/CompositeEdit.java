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

import java.util.*;

import org.slf4j.*;

import spix.core.Spix;

/**
 *  A list of Edits that are run together in order.
 *
 *  @author    Paul Speed
 */
public class CompositeEdit implements Edit {

    static Logger log = LoggerFactory.getLogger(CompositeEdit.class);

    private List<Edit> edits = new ArrayList<>();
    
    public CompositeEdit( Edit... edits ) {
        this.edits.addAll(Arrays.asList(edits));
    }
 
    public void addEdit( Edit edit ) {
        edits.add(edit);
    }
 
    @Override   
    public void undo( Spix spix ) {
        
        // If the edits were added in order then we need to undo them in 
        // reverse order... just in case.
        for( int i = edits.size() - 1; i >= 0; i-- ) {
            Edit edit = edits.get(i);
            log.debug("undo:" + edit);
            edit.undo(spix);
        }
    }
    
    @Override   
    public void redo( Spix spix ) {
        for( Edit edit : edits ) {
            log.debug("redo:" + edit);
            edit.redo(spix);
        }
    }
 
    @Override   
    public String toString() {
        return "CompositeEdit" + edits;
    }
}