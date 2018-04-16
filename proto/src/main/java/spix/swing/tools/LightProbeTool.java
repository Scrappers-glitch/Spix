package spix.swing.tools;

import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.math.FastMath;
import spix.app.light.LightProbeService;
import spix.app.painting.VertexPaintAppState;
import spix.swing.SwingGui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TimerTask;
import java.util.concurrent.*;

public class LightProbeTool extends JPanel {

    private SwingGui gui;
    private final static int HEIGHT = 25;

    public LightProbeTool(SwingGui gui) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setMinimumSize(new Dimension(250, HEIGHT));
        JButton button = new JButton("Render");
        button.setToolTipText("Render Light Probe");
        add(button);

        JLabel label = new JLabel("Quality: ");
        add(label);
        final JComboBox<EnvMapUtils.GenerationType> box = new JComboBox(EnvMapUtils.GenerationType.values());
        add(box);
        box.setPreferredSize(new Dimension(50, 20));

        Dimension minSize = new Dimension(Short.MAX_VALUE, HEIGHT);
        Dimension prefSize = new Dimension(Short.MAX_VALUE, HEIGHT);
        Dimension maxSize = new Dimension(Short.MAX_VALUE, HEIGHT);
        add(new Box.Filler(minSize, prefSize, maxSize));

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getSpix().getService(LightProbeService.class).render((EnvMapUtils.GenerationType) box.getSelectedItem());
            }
        });
    }

}
