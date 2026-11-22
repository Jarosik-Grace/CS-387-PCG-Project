package agents.graceJarosik;

import engine.core.MarioForwardModel;

public class isCoinNearby extends Task {

    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {
        return context.coinFound  && Math.abs(context.coinDX) <= 6  && Math.abs(context.coinDY) <= 5 ? TASK_SUCCESS : TASK_FAILURE;
    }
}


