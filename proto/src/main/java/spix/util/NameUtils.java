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

package spix.util;


/**
 *
 *
 *  @author    Paul Speed
 */
public class NameUtils {

    /**
     *  Converts a potentially camel-cased id to a display name
     *  by breaking it up at camel case boundaries.  Some care is
     *  taken to handle sequences of upper case letters as if they
     *  were acronymes.
     */
    public static String idToName( String id ) {
        StringBuilder result = new StringBuilder();
        boolean lastCap = false;
        for( int i = 0; i < id.length(); i++ ) {
            boolean nextCap = true;
            if( i < id.length() - 1 ) {
                nextCap = Character.isUpperCase(id.charAt(i + 1));
            }
            char c = id.charAt(i);
            if( i == 0 ) {
                c = Character.toUpperCase(c);
            }

            if( Character.isUpperCase(c) ) {
                // Is it a boundary between words?  If the
                // last char is lower case then we're starting a new word.
                // If the next char is lower case then this is also the beggining
                // of a new word at the end of an acronym run.  Like HTTPTest.
                if( i > 0 && (!nextCap || !lastCap) ) {
                    result.append(' ');
                }
                lastCap = true;
            } else {
                lastCap = false;
            }
            result.append(c);
        }

        return result.toString();
    }
}
