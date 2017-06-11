package spix.app.metadata;

/**
 * Created by Nehon on 10/06/2017.
 */
public class ShaderNodeMetadata {

    private int x = -1;
    private int y = -1;
    private boolean collapsed = false;
    private String group;


    public ShaderNodeMetadata(int x, int y, boolean collapsed, String group) {
        this.x = x;
        this.y = y;
        this.collapsed = collapsed;
        this.group = group;
    }

    public ShaderNodeMetadata(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public ShaderNodeMetadata() {
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShaderNodeMetadata shaderNodeMetadata = (ShaderNodeMetadata) o;

        if (x != shaderNodeMetadata.x) return false;
        if (y != shaderNodeMetadata.y) return false;
        if (collapsed != shaderNodeMetadata.collapsed) return false;
        return group.equals(shaderNodeMetadata.group);

    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + (collapsed ? 1 : 0);
        result = 31 * result + group.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ShaderNodeMetadata{" +
                "x=" + x +
                ", y=" + y +
                ", collapsed=" + collapsed +
                ", group='" + group + '\'' +
                '}';
    }
}