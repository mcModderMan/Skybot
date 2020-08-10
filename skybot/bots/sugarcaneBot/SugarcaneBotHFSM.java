package inowen.skybot.bots.sugarcaneBot;

import inowen.moduleSystem.Module;
import inowen.moduleSystem.ModuleManager;
import inowen.skybot.bots.sugarcaneBot.context.InitializationTracker;
import inowen.skybot.bots.sugarcaneBot.context.SugarcaneFarm;
import inowen.skybot.bots.sugarcaneBot.states.GotoLaneState;
import inowen.skybot.bots.sugarcaneBot.states.PickUpItemsState;
import inowen.skybot.hfsmBase.State;
import inowen.skybot.hfsmBase.StateMachine;
import inowen.utils.PlayerMovementHelper;
import net.minecraft.client.Minecraft;


public class SugarcaneBotHFSM extends StateMachine {

    public static Minecraft mc = Minecraft.getInstance();
    public static Module botModule = ModuleManager.getModule("SugarcaneBot");

    public State currentState = null;
    public SugarcaneFarm theFarm;
    public InitializationTracker tracker;

    private float lastRealYaw = 0;
    private float lastRealPitch = 0;

    @Override
    public void start() {
        // Create a tracker to follow how the initialization goes.
        tracker = new InitializationTracker();

        // Load the context / environment
        theFarm = new SugarcaneFarm();
        theFarm.init(tracker);

        // If initialization didn't go as planned, stop.
        if (!tracker.isCompletelyInit()) {
            System.out.println("Problem initially loading the farm in. Shutting off.");
            if (botModule.isToggled()) {
                botModule.onDisable();
                botModule.toggled = false;
            }
            return;
        }

        // Get information for the farm in case onEnter uses it.
        theFarm.update();

        // Set default state
        currentState = new PickUpItemsState(theFarm);
        currentState.onEnter();
    }

    @Override
    public void onTick() {

        if (!tracker.isCompletelyInit()) {
            System.out.println("OnTick: Problem, no farm. Shutting off.");
            if (botModule.isToggled()) {
                botModule.onDisable();
                botModule.toggled = false;
                return;
            }
        }

        // Shut off (and return) if outside of the farm
        if (!theFarm.zoneConstraints.contains(mc.player.getPositionVector())) {
            if (botModule.isToggled()) {
                botModule.onDisable();
                botModule.toggled = false;
                return;
            }
        }

        // Update context
        theFarm.update();

        // Propagate tick
        currentState.run();

        // Transition to next state if necessary
        State nextState = currentState.getNextState();
        if (nextState != null) {
            currentState.onExit();
            currentState = nextState;
            currentState.onEnter();
        }

        // Check Euler Angles
        preventNanEulerAngles();

        // Emergency stuff (probably won't be anything, but if it needs something
        // like the click stopper in CropFarmBot, put it here)

    }

    @Override
    public void onShutdown() {
        PlayerMovementHelper.desetAllkeybinds();
    }


    /**
     * A concatenation of the states that the bot is in - in hierarchical order:
     * TravelState - DriveState - TurnState - MoveHandState ... (from external to internal substate).
     * @return
     */
    public String getStatePath() {
        String statePath = "";
        State trackingState = currentState;
        while(trackingState != null) {
            statePath += trackingState.getName() + " -- ";
            trackingState = trackingState.getCurrentSubstate();
        }
        return statePath;
    }


    /**
     * If the rotation angles are numbers, store them.
     * If not, reset to the last found number.
     */
    public void preventNanEulerAngles() {
        if (Float.isNaN(mc.player.rotationYaw)) {
            mc.player.rotationYaw = lastRealYaw;
        }
        else {
            lastRealYaw = mc.player.rotationYaw;
        }
        if (Float.isNaN(mc.player.rotationPitch)) {
            mc.player.rotationPitch = lastRealPitch;
        }
        else {
            lastRealPitch = mc.player.rotationPitch;
        }
    }
}
