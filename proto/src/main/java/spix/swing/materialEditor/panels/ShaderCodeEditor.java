package spix.swing.materialEditor.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by Nehon on 04/06/2016.
 */
public class ShaderCodeEditor extends JEditorPane {

    private int textWidth;
    private boolean textChanged;


    public ShaderCodeEditor() {
        super();
        setFont(new Font("Monospaced", Font.PLAIN, 14));
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                textChanged = true;
                fitContent();
            }
        });
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        textChanged = true;
    }

    public void fitContent() {
        if(textChanged) {
            computeTextWidth(getGraphics());
            textChanged = false;
        }
        int width = Math.max(textWidth, getParent().getWidth());
        setSize(width, getHeight());
    }

    protected void computeTextWidth(Graphics g) {
        // get metrics from the graphics
        FontMetrics metrics = g.getFontMetrics(getFont());
        String s[] = getText().split("\n");
        textWidth = 0;
        for (String s1 : s) {
            int w = metrics.stringWidth(s1);
            if(w> textWidth){
                textWidth = w;
            }
        }
        textWidth += 25;
    }


}
