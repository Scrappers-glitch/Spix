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

import com.jme3.asset.TextureKey;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
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
public class TexturePanel extends AbstractPropertyPanel<Component> {

    private SwingGui gui;
    private ImageIcon icon;

    public TexturePanel(SwingGui gui, Property prop) {
        super(prop);
        this.gui = gui;

        Texture texture = (Texture) prop.getValue();
        //  JCheckBox checkBox = new JCheckBox();

        JPanel panel = new JPanel(new BorderLayout());
        RollOverTexturePanel rollOverTexturePanel = new RollOverTexturePanel();


        // panel.setPreferredSize(new Dimension(120, 20));
        JButton textureButton = new JButton("Add Texture") {
            @Override
            protected void paintComponent(Graphics g) {
                if (icon != null) {
                    g.drawImage(icon.getImage(), 1, 1, this.getWidth() - 2, this.getWidth(), null);
                }
                super.paintComponent(g);
            }
        };
        textureButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (texture.getKey() != null) {
                    textureButton.setContentAreaFilled(true);
                    rollOverTexturePanel.popupFrom(textureButton);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (texture.getKey() != null) {
                    textureButton.setContentAreaFilled(false);
                    rollOverTexturePanel.close();
                }
            }
        });

        textureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getSpix().getService(FileLoadingService.class).requestTexture(new RequestCallback<Texture>() {
                    @Override
                    public void done(Texture result) {
                        System.err.println("Got it... " + result.toString());
                        prop.setValue(result);
                    }
                });
            }
        });
        textureButton.setFont(textureButton.getFont().deriveFont(Font.BOLD));
        textureButton.setHorizontalTextPosition(JButton.CENTER);
        textureButton.setVerticalTextPosition(JButton.CENTER);

        if (texture.getKey() != null) {
            String assetText = texture.getKey().getName();
            textureButton.setText(assetText.substring(assetText.lastIndexOf("/") + 1));
            textureButton.setToolTipText(assetText);
            textureButton.setContentAreaFilled(false);
            rollOverTexturePanel.update(Icons.test);
            gui.getSpix().getService(MaterialService.class).requestTexturePreview((TextureKey) texture.getKey(), new RequestCallback<MaterialService.PreviewResult>() {
                @Override
                public void done(MaterialService.PreviewResult result) {
                    icon = new ImageIcon(MaterialPreviewRenderer.convert(result.imageData, result.width, result.height));
                    rollOverTexturePanel.update(icon);
                    textureButton.repaint();
                }
            });
        }


        textureButton.setPreferredSize(new Dimension(120, 20));
        panel.add(textureButton, BorderLayout.CENTER);


        List<Property> mainProps = new ArrayList<>();
        List<Property> wrapProps = new ArrayList<>();

        if (texture.getKey() != null) {
            mainProps.add(BeanProperty.create(texture.getKey(), "flipY", "Vertical Flip", false, null));
        }

        mainProps.add(BeanProperty.create(texture, "minFilter"));
        mainProps.add(BeanProperty.create(texture, "magFilter"));


        wrapProps.add(new TexturePanel.WrapModeProperty("wrapS", "Horizontal (u)", Texture.WrapAxis.S, texture));
        wrapProps.add(new TexturePanel.WrapModeProperty("wrapT", "Vertical (v)", Texture.WrapAxis.T, texture));
        if (texture instanceof Texture3D) {
            wrapProps.add(new TexturePanel.WrapModeProperty("wrapR", "Depth (w)", Texture.WrapAxis.R, texture));
        }


        TextureAttributesPopupPanel texturePopup = new TextureAttributesPopupPanel(gui, new DefaultPropertySet(texture, mainProps), new DefaultPropertySet(texture, wrapProps));
        JButton menu = new JButton();
        menu.setIcon(Icons.menu);
        menu.setRolloverIcon(Icons.menuHover);
        menu.setPreferredSize(new Dimension(20, 20));
        menu.setMaximumSize(new Dimension(20, 20));
        menu.setMinimumSize(new Dimension(20, 20));
        menu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                texturePopup.popupFrom(menu);
            }
        });

        panel.add(menu, BorderLayout.EAST);
        setView(panel);
    }


    protected void updateView(Component component, Object value) {

    }

    private class WrapModeProperty extends AbstractProperty {

        Texture.WrapAxis axis;
        Type type = new Type(Texture.WrapMode.class);
        Texture texture;

        public WrapModeProperty(String id, String name, Texture.WrapAxis axis, Texture texture) {
            super(id, name);
            this.axis = axis;
            this.texture = texture;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public void setValue(Object value) {
            texture.setWrap(axis, (Texture.WrapMode) value);
        }

        @Override
        public Object getValue() {
            return texture.getWrap(axis);
        }
    }
}
