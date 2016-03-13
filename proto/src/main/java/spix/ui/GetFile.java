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

package spix.ui;

import java.io.File;

import com.google.common.base.MoreObjects;

/**
 *  Contains the parameters for requesting a file from the user.
 *
 *  @author    Paul Speed
 */
public class GetFile implements UserRequest<File> {
    public String title;
    public String typeDescription;
    public String extensions;
    public boolean forOpen;
    public File initialValue;
       
    public GetFile( String title, String typeDescription, String extensions,
                    boolean forOpen ) {
        this(title, typeDescription, extensions, null, forOpen);
    }
    
    public GetFile( String title, String typeDescription, String extensions, File initialValue,
                    boolean forOpen ) {
        this.title = title;
        this.typeDescription = typeDescription;
        this.extensions = extensions;
        this.forOpen = forOpen;
        this.initialValue = initialValue;                    
    }
 
    @Override   
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getName())
                .add("title", title)
                .add("typeDescription", typeDescription)
                .add("extensions", extensions)
                .add("forOpen", forOpen)
                .add("initialValue", initialValue)
                .toString();                
    }
}

