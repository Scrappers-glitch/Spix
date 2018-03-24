/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor;

import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.nodes.NodePanel;
import spix.swing.materialEditor.nodes.ShaderNodeGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * @author Nehon
 */
public class NodeToolBar extends javax.swing.JPanel implements ComponentListener {

    private final NodePanel node;
    private ResourceBundle labels = ResourceBundle.getBundle("Bundle", Locale.ROOT);

    /**
     * Creates new form NodeToolBar
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public NodeToolBar(NodePanel node) {
        this.node = node;
        initComponents();

        node.addComponentListener(this);
    }

    private void initComponents() {

        Dimension gapSize = new Dimension(5,5);

        setOpaque(false);
        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.rowHeights = new int[]{16};
        setLayout(layout);

        if(node instanceof ShaderNodeGroup) {
            JButton displayGroupButtton = makeButton("NodeToolBar.displayGroupButton.toolTipText", Icons.dockDown, Icons.dockDownHover, new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    displayGroupButtonActionPerformed(evt);
                }
            });
            add(displayGroupButtton, new java.awt.GridBagConstraints());
            add(new Box.Filler(gapSize, gapSize, gapSize));

            JButton ungroupButtton = makeButton("NodeToolBar.ungroupButton.toolTipText", Icons.ungroup, Icons.ungroupHover, new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    ungroupButtonActionPerformed(evt);
                }
            });
            add(ungroupButtton, new java.awt.GridBagConstraints());
            add(new Box.Filler(gapSize, gapSize, gapSize));
        }

        JButton deleteButton = makeButton("NodeToolBar.deleteButton.toolTipText", Icons.deleteNode, Icons.deleteNodeHover, new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });
        add(deleteButton, new java.awt.GridBagConstraints());
    }

    private JButton makeButton(String labelKey, Icon icon, Icon rolloverIcon, ActionListener listener) {

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

    private void ungroupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codeButtonActionPerformed
        ((ShaderNodeGroup)node).ungroup();
    }

    private void displayGroupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_codeButtonActionPerformed
        ((ShaderNodeGroup)node).expand();
    }

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        node.delete();
    }

    public void display() {
        if (getParent() == null) {
            node.getParent().add(this);
        }
        setBounds(node.getLocation().x + 5, node.getLocation().y - 18, node.getWidth() - 10, 16);
        node.getParent().setComponentZOrder(this, 0);
        setVisible(true);
    }

    public void cleanup(){
        node.removeComponentListener(this);
    }

    public void componentResized(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
        setLocation(node.getLocation().x + 5, node.getLocation().y - 18);
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        e.consume();
    }

    public void mousePressed(MouseEvent e) {
        e.consume();
    }

    public void mouseReleased(MouseEvent e) {
        e.consume();
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

}
