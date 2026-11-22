package agents.graceJarosik;

import java.util.List;

public class Selector extends Composite {

    private String lastTask;
    private Task activeTask;

    public Selector(List<Task> children) {
        super(children);
    }

    @Override
    public int run(engine.core.MarioForwardModel model, MarioContext context, boolean[] actions) {

        for (Task child : children) {

            int result = child.run(model, context, actions);

            if (result == TASK_FAILURE) {
                continue;
            }

            if (activeTask != null && activeTask != child) {
                activeTask.reset();
            }

            activeTask = child;
            String name = child.getClass().getSimpleName();

            if (!name.equals(lastTask)) {
                lastTask = name;
            }

            if (result == TASK_SUCCESS) {
                activeTask = null;
                lastTask = null;
            }

            return result;
        }

        if (activeTask != null) {
            activeTask.reset();
            activeTask = null;
        }

        lastTask = null;

        return TASK_FAILURE;
    }

    @Override
    public void reset() {
        if (activeTask != null) {
            activeTask.reset();
        }

        activeTask = null;
        lastTask = null;

        super.reset();
    }
}



