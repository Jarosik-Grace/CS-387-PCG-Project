package agents.graceJarosik;

import engine.core.MarioForwardModel;

public class isGapAhead extends Task {
    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {
        return context.gapAhead ? TASK_SUCCESS : TASK_FAILURE;
    }
}
