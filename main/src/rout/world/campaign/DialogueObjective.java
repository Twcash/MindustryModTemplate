package rout.world.campaign;

import arc.util.Time;
import mindustry.game.MapObjectives.MapObjective;
import rout.world.ui.DialogueOption;

import static mindustry.Vars.state;

/**
 * A map objective that IS a dialogue: it shows its text/options in the bottom-left
 * dialogue bar once qualified, and completes when the player makes an option — or,
 * if {@link #timer} is set, automatically once that many seconds pass. Its
 * {@code flagsAdded} can unlock further objectives/dialogue.
 */
public class DialogueObjective extends MapObjective {
    /** Only becomes available once this flag is set. Empty = always. */
    public String requiresFlag = "";
    public String speaker = "";
    /** Texture region name for the speaker portrait (e.g. "router"). */
    public String icon = "";
    public String text = "";
    public DialogueOption[] options = {};
    /** Seconds before the dialogue auto-ends. 0 = only ends via an option. */
    public float timer = 0f;
    /** Ends the dialogue once this flag is set. Empty = never ends via flag. */
    public String endFlag = "";

    /** Set once the player finishes this dialogue. */
    private transient boolean finished;
    /** How long this dialogue has been active, in seconds. */
    private transient float elapsed;

    @Override
    public boolean update(){
        if(finished) return true;
        //end when the configured flag gets set
        if(!endFlag.isEmpty() && state.rules.objectiveFlags.contains(endFlag)){
            finished = true;
            return true;
        }
        if(timer > 0f){
            elapsed += Time.delta / 60f;
            if(elapsed >= timer){
                finished = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean qualified(){
        return super.qualified() && (requiresFlag.isEmpty() || state.rules.objectiveFlags.contains(requiresFlag));
    }

    public void finish(){
        finished = true;
    }

    public boolean isFinished(){
        return finished;
    }
}