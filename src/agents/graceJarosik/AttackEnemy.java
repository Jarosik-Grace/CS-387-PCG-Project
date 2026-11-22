package agents.graceJarosik;

import engine.core.MarioForwardModel;
import engine.helper.GameStatus;
import engine.helper.MarioActions;

public class AttackEnemy extends Task {

    private static boolean[] act(boolean jump) {
        boolean[] a = new boolean[MarioActions.numberOfActions()];
        a[MarioActions.RIGHT.getValue()] = true;
        a[MarioActions.SPEED.getValue()] = true;
        a[MarioActions.JUMP.getValue()] = jump;
        return a;
    }

    private static final boolean[] JUMP = act(true);
    private static final boolean[] RUN = act(false);

    private static boolean safe(MarioForwardModel model, int hold) {
        MarioForwardModel s = model.clone();
        int mode = s.getMarioMode();
        float startY = s.getMarioFloatPos()[1];
        for (int i = 0; i < 30; i++) {
            s.advance(i < hold ? JUMP : RUN);
            if (s.getGameStatus() == GameStatus.LOSE) return false;
            if (s.getMarioMode() < mode) return false;
            if (!s.isMarioOnGround() && s.getMarioFloatPos()[1] > startY + 48) return false;
        }
        return true;
    }

    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {
        if (!context.enemyFound || !context.stompableEnemy) return TASK_FAILURE;

        if (context.marioMode == 2) {
            actions[MarioActions.SPEED.getValue()] = true;
            return TASK_SUCCESS;
        }

        if (!(model.isMarioOnGround() && model.mayMarioJump())) return TASK_FAILURE;

        for (int hold : new int[]{4, 7, 10, 13}) {
            if (safe(model, hold)) {
                actions[MarioActions.JUMP.getValue()] = true;
                actions[MarioActions.RIGHT.getValue()] = true;
                actions[MarioActions.SPEED.getValue()] = true;
                return TASK_SUCCESS;
            }
        }
        return TASK_FAILURE;
    }
}
