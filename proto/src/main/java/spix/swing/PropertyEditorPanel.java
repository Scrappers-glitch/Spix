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

import java.awt.Dimension;
import javax.swing.JPanel;

import com.google.common.base.MoreObjects;

import spix.core.Spix;
import spix.form.*;
import spix.props.*;

/**
 *  A UI that presents property value editors for a particular
 *  object.
 *
 *  @author    Paul Speed
 */
public class PropertyEditorPanel extends JPanel {

    private Spix spix;
    private PropertySet properties;
    private Form form;

    public PropertyEditorPanel( Spix spix ) {
        this.spix = spix;
        setPreferredSize(new Dimension(100, 100));
    }

    public void setObject( PropertySet properties ) {
System.out.println("PropertyEditorPanel.setObject(" + properties + ")");

        if( this.properties == properties ) {
            return;
        }

        // Clear out any old setup
        if( this.properties != null ) {
            // for now we'll just clear the properties but there will
            // be more work to do when there is a real editor
            this.properties = null;
            this.form = null;
        }

        if( properties == null ) {
            return;
        }
        this.form = spix.createForm(properties);
System.out.println(form.debugString());
    }

    public PropertySet getObject() {
        return properties;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("properties", properties)
                .add("form", form)
                .toString();
    }
}
