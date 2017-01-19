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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
    private RollOverTexturePanel rollOverTexturePanel;
    private JButton textureButton;
    private TextureAttributesPopupPanel texturePopup;
    private JButton menuButton;
    private RolloverObserver rolloverObserver = new RolloverObserver();

    public TexturePanel(SwingGui gui, Property prop) {
        super(prop);
        this.gui = gui;

        Texture texture = (Texture) prop.getValue();
        //  JCheckBox checkBox = new JCheckBox();

        JPanel panel = new JPanel(new BorderLayout());
        rollOverTexturePanel = new RollOverTexturePanel();

        createTextureButton(gui, prop, panel);

        menuButton = new JButton();
        menuButton.setIcon(Icons.menu);
        menuButton.setRolloverIcon(Icons.menuHover);
        menuButton.setPreferredSize(new Dimension(20, 20));
        menuButton.setMaximumSize(new Dimension(20, 20));
        menuButton.setMinimumSize(new Dimension(20, 20));
        menuButton.setEnabled(texture.getKey() != null);
        menuButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (texturePopup != null) {
                    texturePopup.popupFrom(menuButton);
                }
            }
        });

        panel.add(menuButton, BorderLayout.EAST);
        setView(panel);
    }

    public void createTextureButton(final SwingGui gui, final Property prop, JPanel panel) {
        textureButton = new JButton("Add Texture") {
            @Override
            protected void paintComponent(Graphics g) {
                if (icon != null) {
                    g.drawImage(icon.getImage(), 1, 1, this.getWidth() - 2, this.getWidth(), null);
                }
                super.paintComponent(g);
            }
        };
        textureButton.addMouseListener(rolloverObserver);

        textureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getSpix().getService(FileLoadingService.class).requestTexture(new RequestCallback<Texture>() {
                    @Override
                    public void done(Texture result) {
                        prop.setValue(result);
                    }
                });
            }
        });
        textureButton.setToolTipText("Click to add a texture");
        textureButton.setFont(textureButton.getFont().deriveFont(Font.BOLD));
        textureButton.setHorizontalTextPosition(JButton.CENTER);
        textureButton.setVerticalTextPosition(JButton.CENTER);
        textureButton.setPreferredSize(new Dimension(120, 20));
        panel.add(textureButton, BorderLayout.CENTER);

        JPopupMenu buttonPopup = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prop.setValue(null);
            }
        });
        buttonPopup.add(deleteItem);
        textureButton.setComponentPopupMenu(buttonPopup);
    }

    private void createTextureAttributePopup() {
        Property prop = getProperty();
        Texture texture = (Texture) prop.getValue();

        List<Property> mainProps = new ArrayList<>();
        List<Property> wrapProps = new ArrayList<>();

        Property flipY = BeanProperty.create(texture.getKey(), "flipY", "Vertical Flip", false, null);
        flipY.addPropertyChangeListener(new ChangeObserver());
        mainProps.add(flipY);

        mainProps.add(BeanProperty.create(texture, "minFilter"));
        mainProps.add(BeanProperty.create(texture, "magFilter"));

        wrapProps.add(new WrapModeProperty("wrapS", "Horizontal (u)", Texture.WrapAxis.S, texture));
        wrapProps.add(new WrapModeProperty("wrapT", "Vertical (v)", Texture.WrapAxis.T, texture));
        if (texture instanceof Texture3D) {
            wrapProps.add(new WrapModeProperty("wrapR", "Depth (w)", Texture.WrapAxis.R, texture));
        }

        texturePopup = new TextureAttributesPopupPanel(gui, new DefaultPropertySet(texture, mainProps), new DefaultPropertySet(texture, wrapProps), prop);
        menuButton.setEnabled(texture.getKey() != null);
    }

    private void updatePreview( Texture texture) {
        gui.getSpix().getService(MaterialService.class).requestTexturePreview((TextureKey) texture.getKey(), new RequestCallback<MaterialService.PreviewResult>() {
            @Override
            public void done(MaterialService.PreviewResult result) {
                icon = new ImageIcon(MaterialPreviewRenderer.convert(result.imageData, result.width, result.height));
                rollOverTexturePanel.update(icon);
                textureButton.repaint();
            }
        });
    }

    protected void updateView(Component component, Object value) {
        System.err.println("update me");
        Texture tex = (Texture)value;
        if (tex == null) {

            textureButton.setContentAreaFilled(true);
            textureButton.setText("Add Texture");
            textureButton.setToolTipText("Click to add a texture");
            //       textureButton.removeMouseListener(rolloverObserver);
            menuButton.setEnabled(false);
            return;
        }

        if(tex.getKey() != null){
            String assetText = tex.getKey().getName();
            textureButton.setText(assetText.substring(assetText.lastIndexOf("/") + 1));
            textureButton.setToolTipText(assetText);
            textureButton.setContentAreaFilled(false);
            rollOverTexturePanel.update(Icons.test);
            //       textureButton.addMouseListener(rolloverObserver);
            updatePreview(tex);
            if(texturePopup == null){
                createTextureAttributePopup();
            }

        }

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

    private class ChangeObserver implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Property textureProp = getProperty();
            if(evt.getPropertyName().equals("Vertical Flip")){
                textureProp.setValue(textureProp.getValue());
            }
        }
    }

    private class RolloverObserver extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            Texture tex = (Texture) getProperty().getValue();
            if (tex != null && tex.getKey() != null) {
                textureButton.setContentAreaFilled(true);
                rollOverTexturePanel.popupFrom(textureButton);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Texture tex = (Texture) getProperty().getValue();
            if (rollOverTexturePanel != null && rollOverTexturePanel.isVisible()
                    && tex != null && tex.getKey() != null) {
                textureButton.setContentAreaFilled(false);
                rollOverTexturePanel.close();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (rollOverTexturePanel != null && rollOverTexturePanel.isVisible()) {
                textureButton.setContentAreaFilled(false);
                rollOverTexturePanel.close();
            }
        }
    }
}
