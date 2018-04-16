package spix.swing.tools;

import com.jme3.math.FastMath;
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

public class VertexPaintingTool extends JPanel {

    private SwingGui gui;
    private ScheduledExecutorService timer = null;
    private JToggleButton[] buttons = new JToggleButton[4];

    public VertexPaintingTool(SwingGui gui) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.gui = gui;
        this.setPreferredSize(new Dimension(100, 25));
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(200, 25));
        p.setMinimumSize(new Dimension(200, 25));
        p.setMaximumSize(new Dimension(200, 25));
        p.add(new JLabel("Layer: "));
        ButtonGroup group = new ButtonGroup();
        p.add(makeButton(0, group));
        p.add(makeButton(1, group));
        p.add(makeButton(2, group));
        p.add(makeButton(3, group));
        add(p);

        gui.getSpix().getBlackboard().addListener(VertexPaintAppState.TOOLS_VERTEXPAINTING_LAYER, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != evt.getOldValue()) {
                    buttons[(int) evt.getNewValue()].setSelected(true);
                }
            }
        });

        add(makeSliderPanel("Hardness", VertexPaintAppState.TOOLS_VERTEXPAINTING_HARDNESS, 0.2f));
        add(makeSliderPanel("Brush size", VertexPaintAppState.TOOLS_VERTEXPAINTING_SIZE, 0.17f));

        Dimension minSize = new Dimension(Short.MAX_VALUE, 25);
        Dimension prefSize = new Dimension(Short.MAX_VALUE, 25);
        Dimension maxSize = new Dimension(Short.MAX_VALUE, 25);
        add(new Box.Filler(minSize, prefSize, maxSize));
    }

    public JPanel makeSliderPanel(String label, String property, float defaultValue) {
        JPanel p;
        JSlider slider;
        p = new JPanel();
        p.setPreferredSize(new Dimension(200, 25));
        p.setMinimumSize(new Dimension(200, 25));
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(new JLabel(label + ": "));
        //p.setBorder(BorderFactory.createTitledBorder(label));
        slider = new JSlider();
        slider.setPreferredSize(new Dimension(100, 25));
        slider.setPaintLabels(true);
        p.add(slider);
        slider.setValue((int) (defaultValue * 100));
        gui.getSpix().getBlackboard().set(property, defaultValue);

        gui.getSpix().getBlackboard().addListener(property, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != evt.getOldValue()) {
                    slider.setValue((int) ((float) evt.getNewValue() * 100f));
                }
            }
        });


        JTextField number = new JTextField();
        number.setText(defaultValue + "");
        number.setPreferredSize(new Dimension(30, 20));
        p.add(number);

        Dimension minSize = new Dimension(10, 25);
        Dimension prefSize = new Dimension(10, 25);
        Dimension maxSize = new Dimension(10, 25);
        p.add(new Box.Filler(minSize, prefSize, maxSize));

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                float value = slider.getValue() / 100f;
                gui.getSpix().getBlackboard().set(property, value);
                number.setText(String.valueOf(value));
                number.selectAll();
            }
        });

        number.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent ke) {
                if (timer != null) {
                    timer.shutdownNow();
                }
                timer = Executors.newScheduledThreadPool(1);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        String typed = number.getText();
                        slider.setValue(0);
                        float value = 0;
                        try {
                            value = Float.parseFloat(typed);
                            value = FastMath.clamp(value, 0, 1);
                            number.setText(value + "");
                            slider.setValue((int) (value * 100));
                        } catch (NumberFormatException e) {
                            number.setText(value + "");
                        } finally {
                            timer.shutdownNow();
                        }
                    }
                }, 600, TimeUnit.MILLISECONDS);

            }
        });
        return p;
    }

    public JToggleButton makeButton(final int layer, ButtonGroup group) {
        JToggleButton tb = new JToggleButton((layer + 1) + "");
        tb.setPreferredSize(new Dimension(25, 20));
        tb.setVerticalAlignment(SwingConstants.CENTER);
        group.add(tb);
        if (layer == 0) {
            gui.getSpix().getBlackboard().set(VertexPaintAppState.TOOLS_VERTEXPAINTING_LAYER, 0);
            tb.setSelected(true);
        }
        tb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getSpix().getBlackboard().set(VertexPaintAppState.TOOLS_VERTEXPAINTING_LAYER, layer);
            }
        });

        buttons[layer] = tb;
        return tb;
    }
}
