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

package spix.swing.texture;

import com.jme3.asset.*;
import com.jme3.texture.Texture;
import com.jme3.util.clone.Cloner;
import spix.app.FileLoadingService;
import spix.app.material.MaterialService;
import spix.core.RequestCallback;
import spix.props.*;
import spix.swing.AbstractPropertyPanel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.preview.MaterialPreviewRenderer;
import spix.type.Type;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A Texture Property view that uses custom swing components
 *
 * @author Rémy Bouquet
 */
public class MaterialKeyPanel extends AbstractPropertyPanel<Component> {

    public static final String ASSET_BUTTON_TEXT = "Load j3m";
    public static final String ASSET_BUTTON_TOOLTIP = "Click to load a j3m file for this material ";
    public static final String NEW_BUTTON_TOOLTIP = "Create a new j3m file for this material";
    private SwingGui gui;

    private JButton assetButton;
    private JButton newButton;

    private MaterialKey lastValue = null;
    private Cloner cloner = new Cloner();

    public MaterialKeyPanel(SwingGui gui, Property prop) {
        super(prop);
        this.gui = gui;

        MaterialKey key = (MaterialKey) prop.getValue();

        JPanel panel = new JPanel(new BorderLayout());

        createJ3mButton(gui, prop, panel);

        newButton = new JButton();
        newButton.setIcon(Icons.plus);
        newButton.setToolTipText(NEW_BUTTON_TOOLTIP);
        newButton.setRolloverIcon(Icons.plusHover);
        newButton.setPreferredSize(new Dimension(20, 20));
        newButton.setMaximumSize(new Dimension(20, 20));
        newButton.setMinimumSize(new Dimension(20, 20));
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //create a J3M
                gui.getSpix().getService(FileLoadingService.class).createJ3mForSelection();
            }
        });

        panel.add(newButton, BorderLayout.EAST);
        setView(panel);
    }

    public void createJ3mButton(final SwingGui gui, final Property prop, JPanel panel) {
        assetButton = new JButton(ASSET_BUTTON_TEXT);

        assetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Load a J3M
                gui.getSpix().getService(FileLoadingService.class).loadJ3mForSelection();
            }
        });
        assetButton.setToolTipText(ASSET_BUTTON_TOOLTIP);
        assetButton.setFont(assetButton.getFont().deriveFont(Font.BOLD));
        assetButton.setPreferredSize(new Dimension(120, 20));

        panel.add(assetButton, BorderLayout.CENTER);

        JPopupMenu buttonPopup = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Unbind");
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                int result = JOptionPane.showConfirmDialog(gui.getRootWindow(),
                        "This will unbind the material from the j3m file, but won't delete de file.\nThe material will be saved in the j3o file.",
                        "Unbind j3m",
                        JOptionPane.WARNING_MESSAGE,
                        JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    prop.setValue(null);
                }
            }
        });
        buttonPopup.add(deleteItem);
        assetButton.setComponentPopupMenu(buttonPopup);
    }


    protected void updateView(Component component, Object value) {

        if (value == null) {
            assetButton.setText(ASSET_BUTTON_TEXT);
            assetButton.setToolTipText(ASSET_BUTTON_TOOLTIP);
            assetButton.setIcon(null);
        } else {
            AssetKey key = (AssetKey) value;
            String path = key.getName();
            String name = path.substring(path.lastIndexOf("/") + 1);
            assetButton.setText(name);
            assetButton.setToolTipText(path);
            assetButton.setIcon(Icons.material);
        }

    }
}
