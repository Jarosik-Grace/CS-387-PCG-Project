package agents.graceJarosik;

import engine.core.MarioForwardModel;

public class AvoidEnemy extends Task {

    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {
        if (!context.enemyFound) {
            return TASK_FAILURE;
        }

        if (context.stompableEnemy) {
            return TASK_FAILURE;
        }

        int dx = context.enemyDX;
        int dy = context.enemyDY;

        if (dx < 0 || dx > 4 || Math.abs(dy) > 3) {
            return TASK_FAILURE;
        }

        if (context.onGround() || model.getMarioCanJumpHigher()) {
            context.jump(actions);
            actions[MarioContext.RIGHT] = true;
            actions[MarioContext.SPEED] = true;

            return TASK_EXECUTING;
        }

        actions[MarioContext.RIGHT] = true;
        actions[MarioContext.SPEED] = true;

        return TASK_EXECUTING;
    }
}

