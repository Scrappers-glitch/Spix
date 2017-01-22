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

package spix.swing;

import java.io.*;
import java.util.*;


/**
 *  Implementations of the various file filters that uses a file's
 *  extension as the check.  (Copied from meta-jb directly.)
 *
 *  @author    Paul Speed
 */
public class ExtensionFileFilter extends javax.swing.filechooser.FileFilter
                                 implements FileFilter {
    private String description;
    private String[] extensions;
    private String extensionsString;

    public ExtensionFileFilter( String description, String extensions ) {
        this.description = description;
        this.extensionsString = extensions;
        StringTokenizer st = new StringTokenizer(extensions, ",");
        List<String> list = new ArrayList<String>();
        while( st.hasMoreTokens() ) {
            String extension = st.nextToken().trim();
            while( extension.startsWith(".") )
                extension = extension.substring(1);
            list.add("." + extension.toLowerCase());
        }

        this.extensions = new String[list.size()];
        this.extensions = (String[])list.toArray(this.extensions);
    }

    public ExtensionFileFilter( String extensions ) {
        this(extensions + " Files", extensions);
    }

    /**
     *  Returns true if the specified file is not a directory and if
     *  it ends with this filter's extension.
     */
    public boolean accept( File f ) {
        if( f.isDirectory() ) {
            return true;
        }

        if( extensions.length == 0 ) {
            return true;
        }

        String name = f.getName().toLowerCase();

        for( int i = 0; i < extensions.length; i++ ) {
            if( name.endsWith(extensions[i]) ) {
                return true;
            }
        }

        return false;
    }

    /**
     *  Returns the original extensions string.
     */
    public String getExtensionsString() {
        return extensionsString;
    }

    /**
     *  Returns this filter's description.
     */
    public String getDescription() {
        return description;
    }

    public String toString() {
        return "ExtensionFileFilter[" + description + ", " + Arrays.asList(extensions) + "]";
    }
}


