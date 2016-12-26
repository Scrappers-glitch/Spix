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

import java.beans.PropertyChangeListener;
import java.util.Map;

/**
 *  Represents a unit of application-level logic that can provide
 *  additional user-visible state about itself such as name, icon,
 *  enabled-state, etc..
 *
 *  @author    Paul Speed
 */
public interface Action extends Map {
 
    public static final String NAME = "name";
    public static final String ENABLED = "enabled";
    public static final String VISIBLE = "visible";
    public static final String ACCELERATOR = "accelerator";
    public static final String SMALL_ICON = "smallIcon";
    public static final String LARGE_ICON = "largeIcon";
    public static final String TOOLTIP = "tooltip";
     
    public String getId();
    public void setEnabled( boolean b );
    public boolean isEnabled();
    public void setVisible( boolean b );
    public boolean isVisible();   
    public void performAction( Spix spix );
    
    public void addPropertyChangeListener( PropertyChangeListener l );
    public void removePropertyChangeListener( PropertyChangeListener l );
}
