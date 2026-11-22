package agents.graceJarosik;

import engine.core.MarioForwardModel;

public class isAirborne extends Task {
    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {
        return model.isMarioOnGround() ? TASK_FAILURE : TASK_SUCCESS;
    }
}
