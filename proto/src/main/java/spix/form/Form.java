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

package spix.form;

import java.util.*;

import com.google.common.base.MoreObjects;

/**
 *  Defines a loose field ordering and layout for
 *  a particular object type.
 *
 *  @author    Paul Speed
 */
public class Form implements Iterable<Field> {
    public enum Orientation { Vertical, Horizontal };

    private Orientation orientation;
    private final List<Field> fields = new ArrayList<>();

    public Form( Field... fields ) {
        this(Orientation.Vertical, fields);
    }

    public Form( Orientation orientation, Field... fields ) {
        this.orientation = orientation;
        this.fields.addAll(Arrays.asList(fields));
    }

    public void setOrientation( Orientation orientation ) {
        this.orientation = orientation;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public <T extends Field> T add( T field ) {
        fields.add(field);
        return field;
    }

    public <T extends Field> T add( int index, T field ) {
        fields.add(index, field);
        return field;
    }

    public Field get( int index ) {
        return fields.get(index);
    }

    public int size() {
        return fields.size();
    }

    /*public boolean remove( String propertyId ) {

    }*/

    public Iterator<Field> iterator() {
        return fields.iterator();
    }

    public String debugString() {
        return debugString("    ");
    }

    protected String debugString( String indent ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Form[" + orientation + "]:\n");
        for( Field field : fields ) {
            sb.append(indent);
            sb.append(field.getName() + ":");
            if( field instanceof FormField ) {
                sb.append(((FormField)field).getForm().debugString(indent + "    "));
            } else if( field instanceof PropertyField ) {
                PropertyField pf = (PropertyField)field;
                sb.append(pf.getProperty().getValue());
                sb.append("->");
                sb.append(pf.getProperty());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("fields", fields)
                .toString();
    }
}
