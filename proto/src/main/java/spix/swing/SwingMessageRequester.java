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
import spix.ui.MessageRequester;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 *
 *
 *  @author    Paul Speed
 */
public class SwingMessageRequester implements MessageRequester {
 
    private SwingGui swingGui;
    private Map<String, Popup> popups = new HashMap<>();
 
    public SwingMessageRequester( SwingGui swingGui ) {
        this.swingGui = swingGui;
    } 
       
    public void showMessage( final String title, final String message, final Type type ) {
 
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int msgType = JOptionPane.PLAIN_MESSAGE;
                switch( type == null ? Type.Information : type ) {
                    case Information:
                        msgType = JOptionPane.INFORMATION_MESSAGE;
                        break;
                    case Error:
                        msgType = JOptionPane.ERROR_MESSAGE;
                        break;
                }
                JOptionPane.showMessageDialog(swingGui.getRootWindow(), message, title, msgType);
            }
        });
    }

    @Override
    public void confirm(String title, String message, RequestCallback<Boolean> callback) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int result = JOptionPane.showConfirmDialog(swingGui.getRootWindow(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                swingGui.getSpix().sendResponse(callback, result == JOptionPane.YES_OPTION);
            }
        });
    }

    @Override
    public String displayLoading(String message) {
        String id = UUID.randomUUID().toString();
        swingGui.runOnSwing(new Runnable() {
            @Override
            public void run() {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
                JLabel label = new JLabel(message);
                panel.add(label);
                JProgressBar bar = new JProgressBar();
                bar.setPreferredSize(new Dimension(75, 15));
                bar.setIndeterminate(true);
                panel.add(bar);
                Rectangle bound = swingGui.getRootWindow().getBounds();
                double x = bound.getX() + bound.getWidth() - panel.getPreferredSize().getWidth();
                double y = bound.getY() + bound.getHeight() - panel.getPreferredSize().getHeight() - popups.size() * panel.getPreferredSize().getHeight();
                Popup p = PopupFactory.getSharedInstance().getPopup(swingGui.getRootWindow(), panel, (int) x, (int) y);
                p.show();
                popups.put(id, p);
                System.err.println(id);
            }
        });
        return id;
    }

    @Override
    public void hideLoading(String key) {
        swingGui.runOnSwing(new Runnable() {
            @Override
            public void run() {
                Popup p = popups.get(key);
                if (p != null) {
                    p.hide();
                    popups.remove(key);
                }
            }
        });

    }
}
