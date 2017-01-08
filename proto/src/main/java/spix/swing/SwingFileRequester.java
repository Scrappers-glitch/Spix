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

import spix.core.RequestCallback;
import spix.ui.FileRequester;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 *  @author    Paul Speed
 */
public class SwingFileRequester implements FileRequester {
 
    private SwingGui swingGui;
 
    public SwingFileRequester( SwingGui swingGui ) {
        this.swingGui = swingGui;
    }

    @Override
    public void requestFile(final String title, final String typeDescription, final String extensions,
                            final File initialValue, final boolean forOpen, final boolean forImport, final boolean withPreview,
                            final RequestCallback<File> callback) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                List<FileChooserAccessory> accessories = new ArrayList<FileChooserAccessory>();
                if (withPreview) {
                    accessories.add(new PreviewFileChooserAccessory(swingGui));
                }



                File f = FileChooser.getFile(swingGui.getRootWindow(), title, typeDescription, extensions,
                        initialValue, forOpen, JFileChooser.FILES_ONLY, accessories);
                //If the user did not cancel we load the file.
                if (f != null) {
                    swingGui.getSpix().sendResponse(callback, f);
                }
            }
        });                             
    }

    @Override
    public void requestDirectory(final String title, final String typeDescription,
                                 final File initialValue, final boolean forOpen,
                                 final RequestCallback<File> callback) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                File f = FileChooser.getFile(swingGui.getRootWindow(), title, typeDescription, "",
                        initialValue, forOpen, JFileChooser.DIRECTORIES_ONLY, null);
                //If the user did not cancel we load the file.
                if (f != null) {
                    swingGui.getSpix().sendResponse(callback, f);
                }
            }
        });
    }
}


