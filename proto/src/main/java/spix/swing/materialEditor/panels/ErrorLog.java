package spix.swing.materialEditor.panels;

import spix.app.material.MaterialService;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by Nehon on 28/05/2016.
 */
public class ErrorLog  extends DockPanel{


    private JList<String> errorList = new JList<>();
    private MaterialService.CompilationError lastError;

    public ErrorLog(Container container){
        super(Slot.South, container);

        button.setText("Error log");
        button.setToolTipText("Toggle error log");
        button.setVerticalAlignment(SwingConstants.CENTER);

        errorList.setOpaque(true);
        errorList.setBackground(new Color(50,50,50));
        errorList.setCellRenderer(new ListCellRenderer<String>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
                String err = lastError.getErrors().get(index);
                value = value.replaceAll("\\s{4}", "\t\t");

                JLabel ln = new JLabel();
                if(err != null && index > 0) {
                    value = "<html><pre style=\"margin:0\"><i>"  + err.trim() + "</i><br>" + value + "</pre></html>";
                    ln.setBackground(new Color(150, 0, 0));
                    ln.setOpaque(true);
                    ln.setToolTipText(err);
                } else {
                    value = "<html><pre style=\"margin:0\">" + value + "</pre></html>";
                }
                ln.setText(value);
                return ln;
            }
        });

        setComponent(new JScrollPane(errorList));

        noError();
    }




    public void noError(){
        setTitle("Error Log");
        button.setText("Error Log");
        button.setForeground(new Color(50,50,50));
        setIcon(Icons.errorGray);
        button.setIcon(Icons.errorGray);
        dock();
    }

    public void error(MaterialService.CompilationError error){

        String[] lines = error.getShaderSource().split("\\n");
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String line : lines) {
            model.addElement(line);
        }
        lastError = error;
        errorList.setModel(model);

        setTitle("Error Log - " + error.getErrors().get(0));
        button.setText("Error Log - " + error.getErrors().get(0));
        button.setForeground(new Color(250,250,250));
        button.setIcon(Icons.errorSmall);
        setIcon(Icons.errorSmall);
    }

}
