package spix.swing.texture;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Nehon on 05/01/2017.
 */
public class RollOverTexturePanel extends JPanel {
    private JLabel image;
    private Popup popup;
    private int width;
    private Side side = Side.Left;

    public enum Side {
        Left(),
        Right()
    }


    public RollOverTexturePanel() {
        this(128,128);
    }

    public RollOverTexturePanel(int width, int height) {
        super(new BorderLayout());
        image = new JLabel();
        image.setPreferredSize(new Dimension(width, height));
        image.setMaximumSize(new Dimension(width, height));
        image.setMinimumSize(new Dimension(width, height));
        add(image, BorderLayout.CENTER);
        this.width = width;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public void update(ImageIcon icon) {
        image.setIcon(icon);
    }

    public void popupFrom(JComponent button) {
        PopupFactory factory = PopupFactory.getSharedInstance();
        Point pos = button.getLocationOnScreen();
        int x = (int) pos.getX() - (width + 2);
        if( side == Side.Right){
            x = (int) pos.getX() + button.getWidth() + 2;
        }
        popup = factory.getPopup(button, this, x, (int) pos.getY());
        popup.show();
    }

    public void close() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }
    }
}
