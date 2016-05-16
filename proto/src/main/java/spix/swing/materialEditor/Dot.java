/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor;

import com.jme3.shader.*;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.materialEditor.nodes.*;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 *
 * @author Nehon
 */
public class Dot extends JPanel implements MouseListener {

    public static boolean pressed = false;
    protected ImageIcon img;
    protected ImageIcon prevImg;
    private String type;
    private ParamType paramType;
    protected Shader.ShaderType shaderType;
    private String text = "";
    private DraggablePanel node;
    private int index = 1;
    private Set<Dot> pairs = new HashSet<>();

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public enum ParamType {
        Input,
        Output
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public Dot() {
        super();
        setMaximumSize(new Dimension(10, 10));
        setMinimumSize(new Dimension(10, 10));
        setPreferredSize(new Dimension(10, 10));
        setSize(10, 10);
        addMouseListener(this);
       
    }
    
    public void setShaderType(Shader.ShaderType shaderType){
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
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        prevImg = img;
        img = Icons.imgOrange;
        Diagram diag = getDiagram();
        diag.draggedFrom = this;
        repaint();
        e.consume();
    }

    @Override
    public void repaint() {
        if (getNode() != null) {
            getDiagram().repaint();
        } else {
            super.repaint();
        }
    }

    public Diagram getDiagram() {
        return node.getDiagram();
    }

    public DraggablePanel getNode() {
        return node;
    }

    public void setNode(DraggablePanel node) {
        this.node = node;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Diagram diag = getDiagram();
        if (diag.draggedFrom == this && diag.draggedTo != null) {
            if (this.canConnect(diag.draggedTo)) {
                diag.notifyMappingCreation(diag.connect(this, diag.draggedTo));
            } else {
                diag.draggedTo.reset();
                this.reset();
            }
            diag.draggedFrom = null;
            diag.draggedTo = null;
        } else {
            reset();
            diag.draggedFrom = null;
        }
        e.consume();
    }

    public void reset() {
        img = prevImg;
        repaint();
    }

    public void disconnect(Connection conn) {

        getNode().removeComponentListener(conn);
        if(this == conn.getStart()) {
            pairs.remove(conn.getEnd());
        } else {
            pairs.remove(conn.getStart());
        }
        if(pairs.isEmpty()){
            img = Icons.imgGrey;
        }
        repaint();
    }

    public boolean isConnected() {
        return !pairs.isEmpty();
    }

    public Set<Dot> getConnectedDots(){
        return Collections.unmodifiableSet(pairs);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        Diagram diag = getDiagram();
        if (diag.draggedFrom != null && diag.draggedFrom != this) {
            prevImg = img;
            canConnect(diag.draggedFrom);
            diag.draggedTo = this;
            diag.draggedFrom.canConnect(this);
        }

    }

    public boolean canConnect(Dot pair) {
        
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

    protected void connect(Connection conn) {
        img = Icons.imgGreen;
        if(this == conn.getStart()) {
            pairs.add(conn.getEnd());
        } else {
            pairs.add(conn.getStart());
        }
        getNode().addComponentListener(conn);
        repaint();
    }

    public boolean isConnectedToNode(NodePanel nodePanel){
        for (Dot pair : pairs) {
            if(pair.getNode() == nodePanel){
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        Diagram diag = getDiagram();
        if (diag.draggedFrom != null) {
            diag.draggedFrom.canConnect(null);
            if (diag.draggedFrom != this) {
                reset();
            }
            if (diag.draggedTo == this) {
                diag.draggedTo = null;
            }
        }
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
}
