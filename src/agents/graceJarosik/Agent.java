package agents.graceJarosik;

import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

public class Agent implements MarioAgent {

    private MarioContext context;
    private Task root;

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        context = new MarioContext();
        root = BTInterpreter.load("src/agents/graceJarosik/BehaviorTree.xml");
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        boolean[] actions = new boolean[MarioActions.numberOfActions()];
        context.update(model);
        root.run(model, context, actions);

        return actions;
    }

    @Override
    public String getAgentName() {
        return "GraceJarosikAgent";
    }
}
