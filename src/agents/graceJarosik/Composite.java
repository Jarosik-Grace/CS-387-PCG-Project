package agents.graceJarosik;

import java.util.List;
import java.util.ArrayList;

public abstract class Composite extends Task {

    protected List<Task> children;

    public Composite(List<Task> children) {
        this.children = new ArrayList<>(children);
    }

    @Override
    public void reset() {
        for (Task child : children) {
            child.reset();
        }
    }
}