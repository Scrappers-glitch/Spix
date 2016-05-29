package spix.swing.materialEditor.errorlog;

import com.jme3.renderer.opengl.GLRenderer;
import spix.app.material.MaterialService;
import spix.swing.materialEditor.icons.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.*;

import static java.awt.Color.gray;

/**
 * Created by Nehon on 28/05/2016.
 */
public class ErrorLog  extends JPanel{

    private JButton errorLogButton;
    private JList<String> errorList = new JList<>();
    private MaterialService.CompilationError lastError;

    public ErrorLog(){

        setLayout(new BorderLayout());

        errorLogButton = new JButton("Error log");
        errorLogButton.setToolTipText("Toggle error log");
        errorLogButton.setVerticalAlignment(SwingConstants.CENTER);

        errorLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(!isVisible());
            }
        });

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

        add(new JScrollPane(errorList), BorderLayout.CENTER);

        noError();
    }




    public void noError(){
        errorLogButton.setText("Error Log");
        errorLogButton.setForeground(new Color(50,50,50));
        errorLogButton.setIcon(Icons.errorGray);
        setVisible(false);
    }

    public void error(MaterialService.CompilationError error){

        String[] lines = error.getShaderSource().split("\\n");
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String line : lines) {
            model.addElement(line);
        }
        lastError = error;
        errorList.setModel(model);

        errorLogButton.setText("Error Log - " + error.getErrors().get(0));
        errorLogButton.setForeground(new Color(250,250,250));
        errorLogButton.setIcon(Icons.errorSmall);
    }

    public JButton getErrorLogButton() {
        return errorLogButton;
    }
}
