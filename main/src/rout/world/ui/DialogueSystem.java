package rout.world.ui;

import rout.world.campaign.DialogueObjective;

import static mindustry.Vars.state;

/** Bridges the dialogue bar and the map objective system. */
public class DialogueSystem {
    /** The objective currently being shown. */
    public static DialogueObjective active;

    /** Clears the active objective (call on world load so dialogue doesn't carry over). */
    public static void reset(){
        active = null;
    }

    /** The first qualified, unfinished dialogue objective. */
    public static DialogueObjective findActive(){
        DialogueObjective[] out = {null};
        state.rules.objectives.eachRunning(
            o -> o instanceof DialogueObjective d && !d.isFinished(),
            (DialogueObjective d) -> {
                if(out[0] == null) out[0] = d;
            }
        );
        return out[0];
    }

    public static boolean flag(String flag){
        return flag == null || flag.isEmpty() || state.rules.objectiveFlags.contains(flag);
    }

    public static void setFlag(String flag){
        if(flag != null && !flag.isEmpty() && !state.rules.objectiveFlags.contains(flag)){
            state.rules.objectiveFlags.add(flag);
        }
    }

    /** Finish the current dialogue (no options). */
    public static void advance(){
        if(active != null){
            active.finish();
            active = null;
        }
    }

    /** Choose an option: set its output flags and finish the dialogue. */
    public static void choose(DialogueOption option){
        if(active == null) return;
        for(String f : option.setFlags) setFlag(f);
        active.finish();
        active = null;
    }
}