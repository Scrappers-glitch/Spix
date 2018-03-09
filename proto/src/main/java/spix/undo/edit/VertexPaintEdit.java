package spix.undo.edit;

import com.jme3.scene.Geometry;
import com.jme3.util.IntMap;
import spix.app.painting.VertexPaintAppState;
import spix.core.Spix;
import spix.undo.Edit;

public class VertexPaintEdit implements Edit {

    private IntMap<StrokeUnit> entries = new IntMap<>();
    private VertexPaintAppState state;
    private Geometry geom;

    public VertexPaintEdit(VertexPaintAppState state, Geometry geom) {
        this.state = state;
        this.geom = geom;
    }

    public boolean isEmpty(){
        return entries.size() == 0;
    }

    @Override
    public void undo(Spix spix) {
        float[] buffer = state.startBulkPaintSession(geom);
        for (IntMap.Entry<StrokeUnit> entry : entries) {
            buffer[entry.getKey()] = entry.getValue().oldValue;
        }
        state.endBulkPaintSession(geom, buffer);
    }

    @Override
    public void redo(Spix spix) {
        float[] buffer = state.startBulkPaintSession(geom);
        for (IntMap.Entry<StrokeUnit> entry : entries) {
            buffer[entry.getKey()] = entry.getValue().newValue;
        }
        state.endBulkPaintSession(geom, buffer);
    }

    public void addEntry(int index, float oldValue, float newValue) {
        StrokeUnit unit = entries.get(index);
        if (unit != null) {
            unit.newValue = newValue;
        } else {
            entries.put(index, new StrokeUnit(oldValue, newValue));
        }
    }

    private static class StrokeUnit {
        float oldValue;
        float newValue;

        public StrokeUnit(float oldValue, float newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
