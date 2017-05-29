package spix.swing.materialEditor.panels;

import org.fife.ui.rsyntaxtextarea.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * Created by Nehon on 04/06/2016.
 */
public class ShaderCodeEditor extends RSyntaxTextArea {

    private int textWidth;
    private boolean textChanged;


    public ShaderCodeEditor() {
        super(20, 60);
        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        setCodeFoldingEnabled(true);
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
            theme.apply(this);
        } catch (IOException ioe) { // Never happens
            ioe.printStackTrace();
        }
        setEditable(false);
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
