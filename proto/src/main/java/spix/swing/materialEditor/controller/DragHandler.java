package spix.swing.materialEditor.controller;

import spix.swing.materialEditor.Dot;

import javax.swing.*;

/**
 * Created by Nehon on 19/05/2016.
 */
public class DragHandler {

    private Dot draggedFrom;
    private JComponent draggedTo;


    public Dot getDraggedFrom() {
        return draggedFrom;
    }

    public JComponent getDraggedTo() {
        return draggedTo;
    }

    public void setDraggedFrom(Dot draggedFrom) {
        this.draggedFrom = draggedFrom;
    }

    public void setDraggedTo(JComponent draggedTo) {
        this.draggedTo = draggedTo;
    }
}
