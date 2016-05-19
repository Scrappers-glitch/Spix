package spix.swing.materialEditor.controller;

import spix.swing.materialEditor.Dot;

/**
 * Created by Nehon on 19/05/2016.
 */
public class DragController {

    private Dot draggedFrom;
    private Dot draggedTo;


    public Dot getDraggedFrom() {
        return draggedFrom;
    }

    public Dot getDraggedTo() {
        return draggedTo;
    }

    public void setDraggedFrom(Dot draggedFrom) {
        this.draggedFrom = draggedFrom;
    }

    public void setDraggedTo(Dot draggedTo) {
        this.draggedTo = draggedTo;
    }
}
