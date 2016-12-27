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

import java.util.concurrent.ConcurrentHashMap;
import groovy.util.ObservableMap;

import com.google.common.base.MoreObjects;

import spix.util.NameUtils;

/**
 *  Represents a unit of application-level logic that can provide
 *  additional user-visible state about itself such as name, icon,
 *  enabled-state, etc..
 *
 *  @author    Paul Speed
 */
public abstract class AbstractAction extends ObservableMap
                                     implements Action {

    private final String id;

    protected AbstractAction( String id ) {
        this(id, NameUtils.idToName(id));
    }

    protected AbstractAction( String id, String name ) {
        super(new ConcurrentHashMap());
        this.id = id;
        put(NAME, name);
        put(ENABLED, true);
        put(VISIBLE, true);
    }
    protected AbstractAction( String id, String name, String accelerator ) {
        this(id, name);
        put(ACCELERATOR, accelerator);
    }

    public String getId() {
        return id;
    }

    public void setEnabled( boolean b ) {
        put(ENABLED, b);
    }

    public boolean isEnabled() {
        return Boolean.TRUE == get(ENABLED);
    }

    public void setVisible( boolean b ) {
        put(VISIBLE, b);
    }

    public boolean isVisible() {
        return Boolean.TRUE == get(VISIBLE);
    }

    public abstract void performAction( Spix spix );

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getName())
                .add("id", id)
                .add("properties", entrySet())
                .toString();
    }
}
