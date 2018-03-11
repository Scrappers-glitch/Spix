package spix.swing;

import spix.swing.tools.TexturePackerDialog;
import spix.ui.TexturePacker;

import javax.swing.*;
import java.awt.*;

public class SwingTexturePacker implements TexturePacker {

    private SwingGui gui;

    public SwingTexturePacker(SwingGui gui) {
        this.gui = gui;
    }

    @Override
    public void show() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TexturePackerDialog d = new TexturePackerDialog(gui);
                d.setVisible(true);

                JButton button = new JButton("Browse");
                button.setPreferredSize(new Dimension(128,128));


            }
        });
    }
}
