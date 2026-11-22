package agents.graceJarosik;

import engine.core.MarioForwardModel;
import engine.helper.GameStatus;
import engine.helper.MarioActions;

import java.util.HashSet;
import java.util.PriorityQueue;

public class AStar extends Task {

    private static final int MAX_DEPTH = 60;
    private static final float LOOKAHEAD_PX = 176f;
    private static final float MAX_SPEED = 10.9f;
    private static final float DAMAGE_PENALTY = 40f;
    private static final float H_WEIGHT = 1.0f;
    private float lastGroundY = Float.NaN;
    private static final int MAX_EXPANSIONS = 780;

    private static final boolean[][] COMBOS = {
            combo(false, true, true, false),
            combo(false, true, true, true),
            combo(false, true, false, true),
            combo(false, false, false, false),
            combo(true, false, false, false)
    };

    private static boolean[] combo(boolean left, boolean right, boolean speed, boolean jump) {
        boolean[] a = new boolean[MarioActions.numberOfActions()];
        a[MarioActions.LEFT.getValue()] = left;
        a[MarioActions.RIGHT.getValue()] = right;
        a[MarioActions.SPEED.getValue()] = speed;
        a[MarioActions.JUMP.getValue()] = jump;
        return a;
    }

    private static class Node implements Comparable<Node> {
        final MarioForwardModel model;
        final Node parent;
        final int action;
        final int depth;
        final float g;
        final float f;
        final float x;
        final boolean ground;

        Node(MarioForwardModel model, Node parent, int action, int depth, float g, float goalX) {
            this.model = model;
            this.parent = parent;
            this.action = action;
            this.depth = depth;
            this.g = g;
            this.x = model.getMarioFloatPos()[0];
            this.ground = model.isMarioOnGround();
            float h = Math.max(0f, goalX - x) / MAX_SPEED;
            this.f = g + H_WEIGHT * h;
        }

        @Override
        public int compareTo(Node o) {
            return Float.compare(f, o.f);
        }
    }

    private int[] plan = null;
    private int planPos = 0;
    private int frame = 0;

    private static long key(MarioForwardModel s) {
        float[] p = s.getMarioFloatPos();
        float[] v = s.getMarioFloatVelocity();
        long k = (int) (p[0] * 2);
        k = k * 1000003L + (int) (p[1] * 2);
        k = k * 1000003L + (int) (v[0] * 8);
        k = k * 1000003L + (int) (v[1] * 8);
        k = k * 31 + (s.isMarioOnGround() ? 1 : 0);
        k = k * 31 + (s.mayMarioJump() ? 1 : 0);
        k = k * 31 + (s.getMarioCanJumpHigher() ? 1 : 0);
        return k;
    }

    private Node search(MarioForwardModel root) {
        float goalX = root.getMarioFloatPos()[0] + LOOKAHEAD_PX;

        PriorityQueue<Node> open = new PriorityQueue<>();
        HashSet<Long> seen = new HashSet<>();
        open.add(new Node(root.clone(), null, -1, 0, 0f, goalX));
        seen.add(key(root));

        Node goal = null;
        Node bestGround = null;
        int expanded = 0;

        outer:
        while (!open.isEmpty() && expanded < MAX_EXPANSIONS) {
            Node n = open.poll();
            if (n.depth >= MAX_DEPTH) continue;
            expanded++;

            for (int a = 0; a < COMBOS.length; a++) {
                MarioForwardModel s = n.model.clone();
                s.advance(COMBOS[a]);

                GameStatus status = s.getGameStatus();
                if (status == GameStatus.LOSE) continue;

                float penalty = s.getMarioMode() < n.model.getMarioMode() ? DAMAGE_PENALTY : 0f;
                Node c = new Node(s, n, a, n.depth + 1, n.g + 1f + penalty, goalX);

                if (status == GameStatus.WIN || (c.x >= goalX && c.ground)) {
                    goal = c;
                    break outer;
                }

                if (!seen.add(key(s))) continue;
                open.add(c);

                boolean undamaged = c.g <= c.depth;
                if (c.ground && undamaged && (bestGround == null || c.x > bestGround.x)) {
                    bestGround = c;
                }
            }
        }

        return goal != null ? goal : bestGround;
    }

    private static int[] pathOf(Node end) {
        int[] p = new int[end.depth];
        Node n = end;
        for (int i = end.depth - 1; i >= 0; i--) {
            p[i] = n.action;
            n = n.parent;
        }
        return p;
    }

    @Override
    public int run(MarioForwardModel model, MarioContext context, boolean[] actions) {
        frame++;

        if (model.isMarioOnGround() || Float.isNaN(lastGroundY)) {
            lastGroundY = model.getMarioFloatPos()[1];
        }

        Node result = search(model);
        int action = 0;

        if (result != null) {
            plan = pathOf(result);
            planPos = 1;
            action = plan[0];
        } else if (plan != null && planPos < plan.length) {
            action = plan[planPos++];
        }

        boolean[] a = COMBOS[action];
        for (int i = 0; i < a.length; i++) {
            if (a[i]) actions[i] = true;
        }

        return TASK_EXECUTING;
    }

    @Override
    public void reset() {
        plan = null;
        planPos = 0;
    }
}