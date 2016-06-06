package spix.swing.materialEditor.panels;

import spix.swing.materialEditor.icons.Icons;

import java.awt.*;

/**
 * Created by Nehon on 05/06/2016.
 */
public class PropPanel extends DockPanel {

    public PropPanel(Container container) {
        super(Slot.East, container);
        setTitle("Properties");
        setIcon(Icons.tech);
        button.setIcon(Icons.properties);
        button.setRolloverIcon(Icons.propertieseHover);

    }
}
