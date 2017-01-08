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

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 *  Utility methods for calling a JFileChooser in various ways.
 *  Cut-pasted-modified from original meta-jb SwingRequestHandler.
 *
 *  @author    Paul Speed
 */
public class FileChooser {

    /**
     *  Keeps track of the last directory used for different file
     *  types.
     */
    private static Map<String, File> filePaths = new HashMap<String, File>();
    private static File lastDir = null;
    static {
        lastDir = new File( "." );
        try {
            lastDir = lastDir.getCanonicalFile();
        } catch( java.io.IOException e ) {
            throw new RuntimeException("Error in CWD file retrieval", e);
        }
    }

    /**
     *  Reuse the same chooser for everyone.
     */
    private static JFileChooser fileChooser;

    /**
     *  Pops up a file dialog requesting selection of the specified type of file.
     */
    public static File getFile(Component owner, String title, String typeDescription, String extension,
                               File initialValue, boolean forOpen, int mode, List<FileChooserAccessory> accessories) {
        return getFile(owner, title, new ExtensionFileFilter(typeDescription, extension),
                initialValue, forOpen, mode, accessories);
    }

    /**
     *  Pops up a file dialog requesting selection of the specified type of file.
     */
    public static File getFile(Component owner, String title, javax.swing.filechooser.FileFilter filter,
                               File initialValue, boolean forOpen, int mode, List<FileChooserAccessory> accessories) {
        File f = (File)filePaths.get(String.valueOf(filter));
        if( f == null ) {
            f = lastDir;
        }

        String extension = "";
        if( filter instanceof ExtensionFileFilter ) {
            extension = ((ExtensionFileFilter)filter).getExtensionsString();
        }

        if( fileChooser == null ) {
            fileChooser = new JFileChooser(f);
        } else {
            fileChooser.setCurrentDirectory(f);
        }

        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode(mode);
        fileChooser.setMultiSelectionEnabled(false);


        if (accessories != null && accessories.size() > 0) {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;

            for (FileChooserAccessory accessory : accessories) {
                accessory.setFileChooser(fileChooser);
                panel.add(accessory, gbc);
                gbc.gridy++;
            }
            panel.setPreferredSize(new Dimension(250, 100));
            fileChooser.setAccessory(panel);
        }

        if( initialValue != null ) {
            fileChooser.setSelectedFile(initialValue);
        }

        int returnVal;
        if( forOpen ) {
            returnVal = fileChooser.showOpenDialog(owner);
        } else {
            returnVal = fileChooser.showSaveDialog(owner);
        }

        if( returnVal != JFileChooser.APPROVE_OPTION ) {
            return null;
        }

        File selected = fileChooser.getSelectedFile();

        lastDir = selected.getParentFile();
        filePaths.put( String.valueOf(filter), lastDir );

        // If the file is to be written... verify that it has
        // the proper extension.
        if( !forOpen && extension.length() > 0 && extension.indexOf(",") < 0 ) {
            String name = selected.getName();

            if( !name.toLowerCase().endsWith("." + extension.toLowerCase()) ) {
                name = name + "." + extension;
                selected = new File(lastDir, name);
            }
        }

        return selected;
    }

    /**
     *  Pops up a file dialog requesting selection of one or more of the specified type of file.
     */
    public static java.util.List getFiles( Component owner, String title, String typeDescription, String extension,
                                           File initialValue, boolean forOpen, int mode ) {
        return getFiles(owner, title, new ExtensionFileFilter(typeDescription, extension),
                        initialValue, forOpen, mode);
    }

    /**
     *  Pops up a file dialog requesting selection of one or more of the specified type of file.
     */
    public static java.util.List getFiles( Component owner, String title, javax.swing.filechooser.FileFilter filter,
                                           File initialValue, boolean forOpen, int mode ) {
        File f = (File)filePaths.get(String.valueOf(filter));
        if( f == null ) {
            f = lastDir;
        }

        String extension = "";
        if( filter instanceof ExtensionFileFilter ) {
            extension = ((ExtensionFileFilter)filter).getExtensionsString();
        }

        if( fileChooser == null ) {
            fileChooser = new JFileChooser(f);
        } else {
            fileChooser.setCurrentDirectory(f);
        }

        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode(mode);
        fileChooser.setMultiSelectionEnabled(true);

        if( initialValue != null ) {
            fileChooser.setSelectedFile(initialValue);
        }

        int returnVal;
        if( forOpen ) {
            returnVal = fileChooser.showOpenDialog(owner);
        } else {
            returnVal = fileChooser.showSaveDialog(owner);
        }

        if( returnVal != JFileChooser.APPROVE_OPTION ) {
            return null;
        }

        java.util.List<File> selected = Arrays.asList(fileChooser.getSelectedFiles());

        File firstFile = null;

        for( int i = 0; i < selected.size(); i++ ) {
            File s = (File)selected.get(i);

            if( firstFile == null ) {
                firstFile = s;
            }

            // If the file is to be written... verify that it has
            // the proper extension.
            if( !forOpen && extension.length() > 0 && extension.indexOf(",") < 0 ) {
                String name = s.getName();
                File dir = s.getParentFile();

                if( !name.toLowerCase().endsWith("." + extension.toLowerCase()) ) {
                    name = name + "." + extension;
                    s = new File(dir, name);

                    selected.set(i, s);
                }
            }
        }

        if( firstFile != null ) {
            lastDir = firstFile.getParentFile();
            filePaths.put(String.valueOf(filter), lastDir);
        }

        return selected;
    }


    /**
     *  Pops up a simple string entry dialog.
     */
    public static String getString( Component owner, String title, String message, String initialValue ) {
        return (String)JOptionPane.showInputDialog(owner, message, title, JOptionPane.PLAIN_MESSAGE,
                                                   null, null, initialValue);
    }

}

