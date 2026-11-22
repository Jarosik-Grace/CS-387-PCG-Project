package agents.graceJarosik;

import engine.core.MarioForwardModel;

public class isEnemyReachable extends Task {
    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {
        return context.enemyFound && context.stompableEnemy && Math.abs(context.enemyDX) <= 3 && Math.abs(context.enemyDY) <= 3 ? TASK_SUCCESS : TASK_FAILURE;
    }
}