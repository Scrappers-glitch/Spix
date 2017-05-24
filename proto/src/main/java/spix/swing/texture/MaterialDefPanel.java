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

import com.jme3.material.MaterialDef;
import spix.app.FileLoadingService;
import spix.props.Property;
import spix.swing.AbstractPropertyPanel;
import spix.swing.SwingGui;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A Texture Property view that uses custom swing components
 *
 * @author Rémy Bouquet
 */
public class MaterialDefPanel extends AbstractPropertyPanel<Component> {
    public static final String EDIT_BUTTON_TOOLTIP = "Edit this material definition";
    private SwingGui gui;

    private JButton newButton;
    private JComboBox<Entry> matDefsCombo;

    private static final Entry more = new Entry("More...", "");
    private static final Entry newMD = new Entry("New...", "");
    private static final Entry separator = new Entry("", "separator");

    public MaterialDefPanel(SwingGui gui, Property prop) {
        super(prop);
        this.gui = gui;

        MaterialDef def = (MaterialDef) prop.getValue();

        JPanel panel = new JPanel(new BorderLayout());

        createMatDefCombo(gui, prop, panel, def);

        newButton = new JButton();
        newButton.setIcon(Icons.edit);
        newButton.setToolTipText(EDIT_BUTTON_TOOLTIP);
        newButton.setRolloverIcon(Icons.editHover);
        newButton.setPreferredSize(new Dimension(20, 20));
        newButton.setMaximumSize(new Dimension(20, 20));
        newButton.setMinimumSize(new Dimension(20, 20));
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //popup the editor window
                gui.getMatDefEditorWindow().setVisible(true);
            }
        });

        panel.add(newButton, BorderLayout.EAST);
        setView(panel);
    }

    public void createMatDefCombo(final SwingGui gui, final Property prop, JPanel panel, MaterialDef def) {
        matDefsCombo = new JComboBox<>();
        DefaultComboBoxModel<Entry> model = new DefaultComboBoxModel<>();
        Entry selected = new Entry(def.getName(), def.getAssetName());
        model.addElement(selected);
        model.addElement(separator);
        model.addElement(new Entry("Unshaded", "Common/MatDefs/Misc/Unshaded.j3md"));
        model.addElement(new Entry("Phong Lighting", "Common/MatDefs/Light/Lighting.j3md"));
        model.addElement(new Entry("PBR Lighting", "Common/MatDefs/Light/PBRLighting.j3md"));
        model.addElement(separator);
        model.addElement(more);
        model.addElement(newMD);

        matDefsCombo.setModel(model);
        matDefsCombo.setSelectedItem(selected);

        BasicComboBoxRenderer r = new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Entry e = (Entry) value;
                if (e == separator) {
                    return new JSeparator(JSeparator.HORIZONTAL);
                }

                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                JLabel l = (JLabel) c;
                l.setText(e.label);
                l.setToolTipText(e.value);
                return l;
            }
        };
        matDefsCombo.setRenderer(r);

        matDefsCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Entry selEntry = (Entry) matDefsCombo.getSelectedItem();
                System.out.println(selEntry.label);
                if (selEntry == separator) {
                    matDefsCombo.setSelectedItem(selected);
                } else if (selEntry == more) {
                    gui.getSpix().getService(FileLoadingService.class).loadJ3mdForSelection();
                } else if (selEntry == newMD) {
                    gui.getSpix().getService(FileLoadingService.class).createJ3mdForSelection();
                } else {
                    gui.getSpix().getService(FileLoadingService.class).loadStockJ3mdForSelection(selEntry.value);
                }

            }
        });


        panel.add(matDefsCombo, BorderLayout.CENTER);

    }


    protected void updateView(Component component, Object value) {

//        if (value == null) {
//            assetButton.setText(ASSET_BUTTON_TEXT);
//            assetButton.setToolTipText(ASSET_BUTTON_TOOLTIP);
//            assetButton.setIcon(null);
//        } else {
//            AssetKey key = (AssetKey) value;
//            String path = key.getName();
//            String name = path.substring(path.lastIndexOf("/") + 1);
//            assetButton.setText(name);
//            assetButton.setToolTipText(path);
//            assetButton.setIcon(Icons.material);
//        }

    }

    private static class Entry {
        String label;
        String value;

        public Entry(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}
