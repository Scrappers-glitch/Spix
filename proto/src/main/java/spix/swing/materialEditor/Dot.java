/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor;

import com.jme3.shader.*;
import spix.swing.materialEditor.controller.*;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.nodes.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * @author Nehon
 */
public class Dot extends JPanel {

    private ImageIcon img;
    private ImageIcon prevImg;
    private String type;
    private ParamType paramType;
    private Shader.ShaderType shaderType;
    private String text = "";
    private NodePanel node;
    private int index = 1;
    private Set<Dot> pairs = new HashSet<>();

    private MatDefEditorController controller;

    public enum ParamType {
        Input,
        Output
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Dot(MatDefEditorController controller) {
        super();
        this.controller = controller;
        setMaximumSize(new Dimension(10, 10));
        setMinimumSize(new Dimension(10, 10));
        setPreferredSize(new Dimension(10, 10));
        setSize(10, 10);
        addMouseListener(new DotMouseListener());
    }

    public void setShaderType(Shader.ShaderType shaderType) {
        this.shaderType = shaderType;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (img == null) {

            img = Icons.imgGrey;
        }
        g.drawImage(img.getImage(), 0, 0, this);
    }

    @Override
    public void repaint() {
        if (getNode() != null) {
            getNode().repaint();
        } else {
            super.repaint();
        }
    }

    public NodePanel getNode() {
        return node;
    }

    public void setNode(NodePanel node) {
        this.node = node;
    }

    private void reset() {
        img = prevImg;
        repaint();
    }

    public boolean isConnected() {
        return !pairs.isEmpty();
    }

    public Set<Dot> getConnectedDots() {
        return Collections.unmodifiableSet(pairs);
    }

    private boolean canConnect(Dot pair) {

        if (pair == null || paramType == ParamType.Input ||
                ((pair.getNode() instanceof InOutPanel || node instanceof InOutPanel) && shaderType != pair.shaderType)) {
            img = Icons.imgOrange;
            repaint();
            return false;
        }


        if (matches(pair.getType(), type) && (pair.getParamType() != paramType)
                || ShaderUtils.isSwizzlable(pair.getType()) && ShaderUtils.isSwizzlable(type)) {
            img = Icons.imgGreen;
            repaint();
            return true;
        }


        img = Icons.imgRed;
        repaint();
        return false;
    }

    private boolean matches(String type1, String type2) {
        String[] s1 = type1.split("\\|");
        String[] s2 = type2.split("\\|");
        for (String string : s1) {
            for (String string1 : s2) {
                if (string.equals(string1)) {
                    return true;
                }
            }
        }
        return false;

    }

    public void connect(Connection conn) {
        img = Icons.imgGreen;
        if (this == conn.getStart()) {
            pairs.add(conn.getEnd());
        } else {
            pairs.add(conn.getStart());
        }
        getNode().addComponentListener(conn.getComponentListener());
        repaint();
    }

    public void disconnect(Connection conn) {

        getNode().removeComponentListener(conn.getComponentListener());
        if (this == conn.getStart()) {
            pairs.remove(conn.getEnd());
        } else {
            pairs.remove(conn.getStart());
        }
        if (pairs.isEmpty()) {
            img = Icons.imgGrey;
        }
        repaint();
    }

    public boolean isConnectedToNode(NodePanel nodePanel) {
        for (Dot pair : pairs) {
            if (pair.getNode() == nodePanel) {
                return true;
            }
        }
        return false;
    }

    public Point getStartLocation() {
        Point p = getLocation();
        Component parent = getParent();
        while (parent != getNode()) {
            p.x += parent.getLocation().x;
            p.y += parent.getLocation().y;
            parent = parent.getParent();
        }
        p.x += 10 + getNode().getLocation().x;
        p.y += 5 + getNode().getLocation().y;
        return p;
    }

    public Point getEndLocation() {
        Point p = getLocation();
        Component parent = getParent();
        while (parent != getNode()) {
            p.x += parent.getLocation().x;
            p.y += parent.getLocation().y;
            parent = parent.getParent();
        }
        p.x += getNode().getLocation().x + 2;
        p.y += 5 + getNode().getLocation().y;
        return p;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ParamType getParamType() {
        return paramType;
    }

    public void setParamType(ParamType paramType) {
        this.paramType = paramType;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }


    private class DotMouseListener extends MouseAdapter {

        private DragHandler dragHandler = controller.getDragHandler();

        @Override
        public void mousePressed(MouseEvent e) {
            prevImg = img;
            img = Icons.imgOrange;
            dragHandler.setDraggedFrom(Dot.this);
            repaint();
            e.consume();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Dot from = dragHandler.getDraggedFrom();
            Dot to = dragHandler.getDraggedTo();
            if ( from == Dot.this && to != null) {
                if (Dot.this.canConnect(to)) {
                    controller.connect(Dot.this, to);
                } else {
                    to.reset();
                    Dot.this.reset();
                }
                dragHandler.setDraggedFrom(null);
                dragHandler.setDraggedTo(null);
            } else {
                reset();
                dragHandler.setDraggedFrom(null);
            }
            e.consume();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            Dot from = dragHandler.getDraggedFrom();
            if (from != null && from != Dot.this) {
                prevImg = img;
                canConnect(from);
                dragHandler.setDraggedTo(Dot.this);
                from.canConnect(Dot.this);
            }

        }

        @Override
        public void mouseExited(MouseEvent e) {
            Dot from = dragHandler.getDraggedFrom();
            Dot to = dragHandler.getDraggedTo();
            if (from != null) {
                from.canConnect(null);
                if (from != Dot.this) {
                    reset();
                }
                if (to == Dot.this) {
                    dragHandler.setDraggedTo(null);
                }
            }
        }

    }
}
