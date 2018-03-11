package spix.swing.tools;

import spix.app.DefaultConstants;
import spix.app.light.DebugLightsState;
import spix.core.Spix;
import spix.swing.materialEditor.icons.Icons;
import spix.swing.texture.RollOverTexturePanel;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProbeList extends JPanel {

    private RollOverTexturePanel rollover = new RollOverTexturePanel(256, 128);
    private int currentIndex = -1;
    private ImageIcon[] thumbs;

    public ProbeList(Spix spix) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        setPreferredSize(new Dimension(300, 25));
        add(new JLabel("debug probe: "));
        JComboBox<String> list = new JComboBox<>(DebugLightsState.probes);
        list.setRenderer(new MyComboBoxRenderer());
        list.setPreferredSize(new Dimension(150, 25));
        list.setMaximumSize(new Dimension(250, 25));
        add(list);
        rollover.setSide(RollOverTexturePanel.Side.Right);
        thumbs = new ImageIcon[DebugLightsState.probes.length];
        for (int i = 0; i < thumbs.length; i++) {
            thumbs[i] = new ImageIcon(this.getClass().getResource("/probeThumbs/" + DebugLightsState.probes[i] + ".jpg"));
        }

        list.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                spix.getBlackboard().set(DebugLightsState.PROBE_INDEX, list.getSelectedIndex());
                rollover.close();
            }
        });

        spix.getBlackboard().bind(DefaultConstants.VIEW_DEBUG_LIGHTS, list, "enabled");
    }


    class MyComboBoxRenderer extends BasicComboBoxRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JComponent c = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (isSelected && currentIndex != index && index > -1) {
                rollover.close();
                rollover.update(thumbs[index]);
                currentIndex = index;
                rollover.popupFrom(ProbeList.this);
            }
            return c;
        }
    }
}
