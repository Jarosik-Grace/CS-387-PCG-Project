package agents.graceJarosik;

import engine.core.MarioForwardModel;

public class MarioContext {
    public MarioForwardModel model;

    public float marioX;
    public float marioY;
    public float velocityX;
    public float velocityY;
    public int marioMode;

    public int[][] scene;
    public int[][] enemies;

    public boolean coinFound;
    public int coinDX;
    public int coinDY;
    public boolean coinReachable;

    public boolean enemyFound;
    public boolean stompableEnemy;
    public int enemyDX;
    public int enemyDY;

    public boolean obstacle;

    public boolean gapAhead;
    public int gapDistance;
    public boolean landingReachable;

    public boolean danger;

    public static final int RIGHT = 1;
    public static final int SPEED = 3;
    public static final int JUMP = 4;
    public boolean emergencyStop;

    public void update(MarioForwardModel model) {
        this.model = model;
        emergencyStop = gapAhead && gapDistance <= 5 && !landingReachable;

        float[] position = model.getMarioFloatPos();
        float[] velocity = model.getMarioFloatVelocity();

        marioX = position[0];
        marioY = position[1];
        velocityX = velocity[0];
        velocityY = velocity[1];
        marioMode = model.getMarioMode();
        scene = model.getMarioSceneObservation();
        enemies = model.getMarioEnemiesObservation();
        emergencyStop = gapAhead && gapDistance <= 5 && !landingReachable;
    }

    public boolean canJump() {
        return model.mayMarioJump();
    }

    public boolean onGround() {
        return model.isMarioOnGround();
    }

    public void jump(boolean[] actions) {
        if (canJump() || model.getMarioCanJumpHigher()) {
            actions[JUMP] = true;
        }
    }
}
