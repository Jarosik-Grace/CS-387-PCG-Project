package agents.graceJarosik;

import engine.core.MarioForwardModel;

public class CollectCoin extends Task {

    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {

        if (!context.coinFound || !context.coinReachable) {
            return TASK_FAILURE;
        }

        actions[MarioContext.RIGHT] = true;
        actions[MarioContext.SPEED] = true;

        if (context.coinDY < -1 && context.onGround()) {
            context.jump(actions);
        }

        return TASK_EXECUTING;
    }
}
