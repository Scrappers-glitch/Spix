package spix.swing.tools;

import spix.app.DefaultConstants;
import spix.swing.FileChooser;
import spix.swing.SwingGui;
import spix.ui.MessageRequester;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;

public class TexturePackerDialog extends JDialog {
    private SwingGui gui;
    private JFileChooser chooser = new JFileChooser();
    private String defaultFolder;
    private ImagePacker packer = new ImagePacker();

    public TexturePackerDialog(SwingGui gui) {
        super((JFrame) gui.getRootWindow(), true);
        this.gui = gui;
        defaultFolder = gui.getSpix().getBlackboard().get(DefaultConstants.MAIN_ASSETS_FOLDER, String.class);
        setSize(358, 627);
        JPanel mainPanel = new JPanel();
        this.setContentPane(mainPanel);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JFrame f = (JFrame) gui.getRootWindow();
        setTitle("Texture Packer");
        setLocationRelativeTo(f);

        mainPanel.add(makeChannelPanel("R", 0));
        mainPanel.add(makeHorizontalSeparator());
        mainPanel.add(makeChannelPanel("G", 1));
        mainPanel.add(makeHorizontalSeparator());
        mainPanel.add(makeChannelPanel("B", 2));
        mainPanel.add(makeHorizontalSeparator());
        mainPanel.add(makeChannelPanel("A", 3));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        buttonPanel.add(closeButton);
        buttonPanel.add(makeHorizontalFiller(50, 20));
        JButton packButton = new JButton("Pack");
        packButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File file = FileChooser.getFile(TexturePackerDialog.this,
                        "Save as image",
                        "Image File",
                        "png",
                        new File(defaultFolder),
                        false, JFileChooser.FILES_ONLY,
                        null);

                if (file == null) {
                    return;
                }
                BufferedImage image = packer.pack();
                try {
                    ImageIO.write(image, "png", file);
                    gui.getSpix().getService(MessageRequester.class).showMessage("Success", "The image has been saved successfully", MessageRequester.Type.Information);
                } catch (IOException e1) {
                    e1.printStackTrace();
                    gui.getSpix().getService(MessageRequester.class).showMessage("Error saving", "An error occurred while saving the file", MessageRequester.Type.Error);
                }
            }
        });
        buttonPanel.add(packButton);
        mainPanel.add(buttonPanel);

        pack();
    }

    public JSeparator makeHorizontalSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        setSize(sep, 300, 15);
        return sep;
    }

    public JPanel makeChannelPanel(String channel, int index) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        JButton button = new JButton("Browse");
        button.setHorizontalTextPosition(JButton.CENTER);
        button.setVerticalTextPosition(JButton.CENTER);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File file = FileChooser.getFile(TexturePackerDialog.this,
                        "Choose an image",
                        "Image File",
                        "jpg, png",
                        new File(defaultFolder),
                        true, JFileChooser.FILES_ONLY,
                        null);
                if (file == null) {
                    return;
                }
                try {
                    BufferedImage img = ImageIO.read(file);
                    packer.setImage(img, index);
                    BufferedImage thumb = packer.getThumb(img);
                    packer.setThumb(thumb, index);
                    button.setIcon(new ImageIcon(thumb));
                } catch (IOException e1) {
                    e1.printStackTrace();
                    gui.getSpix().getService(MessageRequester.class).showMessage("Error loading", "An error occurred while loading the file", MessageRequester.Type.Error);
                }


            }
        });
        setSize(button, 128, 128);
        p.add(button);

        JLabel label = new JLabel(channel);
        label.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        setSize(label, 128, 128);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setHorizontalTextPosition(JButton.CENTER);
        label.setVerticalTextPosition(JButton.CENTER);

        JPanel vp = new JPanel();
        vp.setLayout(new BoxLayout(vp, BoxLayout.Y_AXIS));
        ButtonGroup group = new ButtonGroup();
        vp.add(makeChannelButton("R", group, label, index, 0));
        vp.add(makeChannelButton("G", group, label, index, 1));
        vp.add(makeChannelButton("B", group, label, index, 2));
        vp.add(makeChannelButton("A", group, label, index, 3));
        setSize(vp, 32, 128);
        p.add(vp);

        p.add(makeHorizontalFiller(50, 128));


        p.add(label);
        return p;
    }

    public Box.Filler makeHorizontalFiller(int width, int height) {
        Dimension d = new Dimension(width, height);
        Box.Filler filler = new Box.Filler(d, d, d);
        filler.setAlignmentY(Component.TOP_ALIGNMENT);
        return filler;
    }

    public void setSize(JComponent vp, int width, int height) {
        vp.setMinimumSize(new Dimension(width, height));
        vp.setPreferredSize(new Dimension(width, height));
        vp.setMaximumSize(new Dimension(width, height));
        vp.setAlignmentY(Component.TOP_ALIGNMENT);
    }

    public JToggleButton makeChannelButton(String r, ButtonGroup group, JLabel label, int index, int channel) {
        JToggleButton b1 = new JToggleButton(r);
        b1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Image im = packer.preview(index, channel);
                if (im == null) {
                    return;
                }
                label.setIcon(new ImageIcon(im));

            }
        });
        setSize(b1, 32, 32);
        group.add(b1);
        return b1;
    }
}
