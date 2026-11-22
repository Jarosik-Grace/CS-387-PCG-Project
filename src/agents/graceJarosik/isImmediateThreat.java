package agents.graceJarosik;

import engine.core.MarioForwardModel;

public class isImmediateThreat extends Task {

    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {
        return context.danger ? TASK_SUCCESS : TASK_FAILURE;
    }
}