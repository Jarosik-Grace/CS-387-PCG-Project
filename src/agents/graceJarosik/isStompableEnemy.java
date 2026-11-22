package agents.graceJarosik;

import engine.core.MarioForwardModel;

public class isStompableEnemy extends Task {
    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {
        return context.enemyFound && context.stompableEnemy ? TASK_SUCCESS : TASK_FAILURE;
    }
}
