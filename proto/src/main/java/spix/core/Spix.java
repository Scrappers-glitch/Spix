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

import spix.type.*;
import spix.ui.*;

/**
 *
 *
 *  @author    Paul Speed
 */
public class Spix {

    private final Blackboard blackboard = new DefaultBlackboard();
    
    private final TypeRegistry<UserRequestHandler> requestHandlers = new TypeRegistry();
    
    public Spix() {
    }
    
    public Blackboard getBlackboard() {
        return blackboard;
    }
 
    public <T, R extends UserRequest<T>> void registerRequestHandler( Class<R> type, UserRequestHandler<T, R> handler ) {
        requestHandlers.register(type, handler);     
    } 
  
    protected <T, R extends UserRequest<T>> UserRequestHandler<T, R> getHandler( Class<R> type ) {
        return (UserRequestHandler<T, R>)requestHandlers.get(type, false);
    } 
    
    public <T, R extends UserRequest<T>> void request( R request, RequestCallback<T> callback ) {
        UserRequestHandler<T, R> handler = getHandler(request.getClass());
        handler.handleRequest(this, request, callback);
    }
    
    public <T> void sendResponse( RequestCallback<T> callback, T result ) {
        callback.done(result);
    } 
}
