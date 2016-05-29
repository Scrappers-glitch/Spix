/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor;

import spix.app.material.MaterialAppState;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.nodes.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * @author Nehon
 */
public class OutToolBar extends javax.swing.JPanel {

    private final OutPanel node;
    private ResourceBundle labels = ResourceBundle.getBundle("Bundle", Locale.ROOT);
    private ComponentListener listener;

    private JButton quadButton;
    private JButton boxButton;
    private JButton sphereButton;

    /**
     * Creates new form NodeToolBar
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public OutToolBar(OutPanel node) {
        initComponents();
        this.node = node;

        listener = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                setLocation(node.getLocation().x + 5, node.getLocation().y + node.getHeight());
            }
        };
        node.addComponentListener(listener);
    }

    private void initComponents() {

        setOpaque(false);
        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.rowHeights = new int[]{16};
        setLayout(layout);
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 10;

        quadButton = makeButton("OutToolBar.quadButton.toolTipText", Icons.quad, Icons.quadHover, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                node.setDisplayType(MaterialAppState.DisplayType.Quad);
            }
        });
        add(quadButton, c);
        boxButton = makeButton("OutToolBar.boxButton.toolTipText", Icons.cube, Icons.cubeHover, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                node.setDisplayType(MaterialAppState.DisplayType.Box);
            }
        });
        add(boxButton, c);
        sphereButton = makeButton("OutToolBar.sphereButton.toolTipText", Icons.sphere, Icons.sphereHover, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                node.setDisplayType(MaterialAppState.DisplayType.Sphere);
            }
        });
        add(sphereButton, c);


    }



    private JButton makeButton(String labelKey, Icon icon, Icon rolloverIcon, ActionListener listener) {
        //"NodeToolBar.button.toolTipText",Icons.code, Icons.codeHover
        JButton button = new JButton();
        button.setBackground(new java.awt.Color(255, 255, 255));
        button.setIcon(icon); // NOI18N
        button.setToolTipText(labels.getString(labelKey)); // NOI18N
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        button.setFocusable(false);
        button.setIconTextGap(0);
        button.setMaximumSize(new java.awt.Dimension(24, 24));
        button.setMinimumSize(new java.awt.Dimension(24, 24));
        button.setPreferredSize(new java.awt.Dimension(16, 16));
        button.setRolloverIcon(rolloverIcon); // NOI18N
        button.addActionListener(listener);
        return button;
    }


    public void display() {
        if (getParent() == null) {
            node.getParent().add(this);
        }
        setBounds(node.getLocation().x + 5,  node.getLocation().y + node.getHeight(), node.getWidth() - 10, 16);
        node.getParent().setComponentZOrder(this, 0);
        setVisible(true);
    }

    public void cleanup() {
        node.removeComponentListener(listener);
    }


}
