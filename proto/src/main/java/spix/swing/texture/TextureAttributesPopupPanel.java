package spix.swing.texture;

import com.jme3.asset.AssetKey;
import com.jme3.texture.Texture;
import com.jme3.util.clone.Cloner;
import spix.props.BeanProperty;
import spix.props.Property;
import spix.props.PropertySet;
import spix.swing.PropertyEditorPanel;
import spix.swing.SwingGui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A popup Panel containing fileds to update sub attributes of a texture
 * Created by Nehon on 05/01/2017.
 */
public class TextureAttributesPopupPanel extends JPanel {

    private Popup popup;
    private Component lastLostFocusComponent;
    private Property textureProp;

    public TextureAttributesPopupPanel(SwingGui gui, PropertySet mainProps, PropertySet wrapProps, Property textureProp) {
        super();

        this.textureProp = textureProp;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(40, 40, 40)), BorderFactory.createEmptyBorder(3, 3, 3, 3)));

        PropertyEditorPanel panel = new PropertyEditorPanel(gui);
        panel.setObject(mainProps);
        panel.setBorder(BorderFactory.createEmptyBorder());
        this.add(panel);

        panel = new PropertyEditorPanel(gui);
        panel.setObject(wrapProps);
        panel.setBorder(BorderFactory.createTitledBorder("Wrap Mode"));
        this.add(panel);

        addFocusListener(new FocusObserver());

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                //ESC
                if (e.getKeyCode() == 0) {
                    close();
                }
            }
        });

    }

    public void popupFrom(JButton button) {
        lastLostFocusComponent = button;
        if (popup != null) {
            close();
            return;
        }
        int x = (int) button.getLocationOnScreen().getX() - (int) getPreferredSize().getWidth() + button.getWidth();
        int y = (int) button.getLocationOnScreen().getY() - (int) getPreferredSize().getHeight() - 3;
        popup = PopupFactory.getSharedInstance().getPopup(button, TextureAttributesPopupPanel.this, x, y);
        popup.show();
        requestFocus();
        JViewport vp = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, button);
        if (vp != null) {
            vp.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    close();
                    vp.removeChangeListener(this);
                }
            });
        }
    }

    public void close() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }
    }

    //pops out the panel when loosing focus or if the property panel is scrolled,
    //but also gives back the focus to the panel when inside components are done with the focus
    private class FocusObserver extends FocusAdapter {
        @Override
        public void focusLost(FocusEvent e) {
            if (e.getOppositeComponent() == null) {
                close();
                return;
            }
            if (lastLostFocusComponent == e.getOppositeComponent()) {
                return;
            }
            if (SwingUtilities.isDescendingFrom(e.getOppositeComponent(), TextureAttributesPopupPanel.this)) {
                Component c = e.getOppositeComponent();

                try {
                    //I don't want to have to cast to every single component where I can add an ActionListener so I cheat.
                    Method add = c.getClass().getMethod("addActionListener", ActionListener.class);
                    Method remove = c.getClass().getMethod("removeActionListener", ActionListener.class);

                    add.invoke(c, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            requestFocus();
                            try {
                                remove.invoke(c, this);
                            } catch (IllegalAccessException | InvocationTargetException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e1) {
                    e1.printStackTrace();
                    requestFocus();
                }

                return;
            }
            lastLostFocusComponent = e.getOppositeComponent();
            close();
        }
    }


}
