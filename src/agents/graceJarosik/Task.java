package agents.graceJarosik;

import engine.core.MarioForwardModel;

public abstract class Task {

    public static final int TASK_SUCCESS = 1;
    public static final int TASK_FAILURE = 2;
    public static final int TASK_EXECUTING = 3;

    public static Task create() {
        return new AStar();
    }

    public abstract int run(MarioForwardModel model, MarioContext context, boolean[] actions);

    public void reset() {
    }
}
