package agents.graceJarosik;

import engine.core.MarioForwardModel;
import java.util.List;

public class Sequence extends Composite {

    private int current = 0;

    public Sequence(List<Task> children) {
        super(children);
    }

    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {

        for (int i = current; i < children.size(); i++) {
            Task child = children.get(i);

            int result = child.run(model, context, actions);

            if (result == TASK_FAILURE) {
                reset();
                return TASK_FAILURE;
            }

            if (result == TASK_EXECUTING) {
                current = i;
                return TASK_EXECUTING;
            }

            current = i + 1;
        }

        reset();
        return TASK_SUCCESS;
    }

    @Override
    public void reset() {
        current = 0;
        super.reset();
    }
}

