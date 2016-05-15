package spix.swing.materialEditor.nodes;

/**
 * Created by bouquet on 15/05/16.
 */
public interface Editable {

    void setEditionAllowed(boolean allowed);
    boolean isEditionAllowed();
    void edit();
}
