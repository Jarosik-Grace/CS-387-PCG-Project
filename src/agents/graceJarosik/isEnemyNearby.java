package agents.graceJarosik;

import engine.core.MarioForwardModel;

public class isEnemyNearby extends Task {
    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {
        return context.enemyFound && Math.abs(context.enemyDX) <= 2 && Math.abs(context.enemyDY) <= 2 ? TASK_SUCCESS : TASK_FAILURE;
    }
}
