package spix.swing.texture;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Nehon on 05/01/2017.
 */
public class RollOverTexturePanel extends JPanel {
    JLabel image;
    Popup popup;

    public RollOverTexturePanel() {
        super(new BorderLayout());
        image = new JLabel();
        image.setPreferredSize(new Dimension(128, 128));
        image.setMaximumSize(new Dimension(128, 128));
        image.setMinimumSize(new Dimension(128, 128));
        add(image, BorderLayout.CENTER);
    }

    public void update(ImageIcon icon) {
        image.setIcon(icon);
    }

    public void popupFrom(JButton button) {
        PopupFactory factory = PopupFactory.getSharedInstance();
        Point pos = button.getLocationOnScreen();
        popup = factory.getPopup(button, this, (int) pos.getX() - 130, (int) pos.getY());
        popup.show();
    }

    public void close() {
        popup.hide();
        popup = null;
    }
}
