package rout.world.ui;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.ui.Styles;
import rout.world.campaign.DialogueObjective;

/** Bottom-left dialogue bar: slides in, types its text letter by letter, shows
 *  options, and slides out left when advancing. Driven every tick. */
public class DialogueBar {
    private static final float charRate = 12f;
    private static final float enterDur = 0.25f;
    private static final float exitDur = 0.3f;
    private static final float basePad = 12f;

    private final Table root = new Table();
    private final Table bar = new Table();
    private final Image portrait = new Image();
    private final Label speaker = new Label("");
    private final Label text = new Label("");
    private final Table options = new Table();
    private Cell<Table> barCell;

    private DialogueObjective shown;
    private float typeTimer;
    private int charCount;
    private boolean entering, exiting;
    private float enterT, exitT, slideOffset;
    private boolean built;

    public void build(){
        if(built) return;
        built = true;

        bar.setBackground(Styles.black6);
        bar.margin(14f);
        bar.defaults().left();

        portrait.setScaling(Scaling.fit);
        bar.add(portrait).size(64f).padRight(12f);

        bar.table(info -> {
            info.add(speaker).left().padBottom(4f);
            info.row();
            info.add(text).left().width(700f).wrap();
            info.row();
            info.add(options).left().padTop(10f);
        });

        text.setFontScale(1.6f);
        speaker.setFontScale(1.4f);

        root.setFillParent(true);
        root.bottom().left();
        //only the bar/options are clickable; clicks elsewhere pass through
        root.touchable = Touchable.childrenOnly;
        root.visible = false;
        barCell = root.add(bar).bottom().left().pad(basePad);

        Vars.ui.hudGroup.addChild(root);

        bar.clicked(() -> {
            if(!exiting && shown != null && charCount >= shown.text.length() && shown.options.length == 0){
                DialogueSystem.advance();
                beginExit();
            }
        });

        Events.run(EventType.Trigger.update, () -> update());
    }

    private float slideOffset(){
        return Math.max(bar.getPrefWidth() + 60f, 700f);
    }

    private void beginExit(){
        exiting = true;
        exitT = 0f;
        slideOffset = slideOffset();
    }

    private void update(){
        if(DialogueSystem.active != null && DialogueSystem.active.isFinished()){
            if(shown == DialogueSystem.active && !exiting){
                beginExit();
            }
            DialogueSystem.active = null;
        }

        //pick up the next available objective
        if(DialogueSystem.active == null){
            DialogueObjective next = DialogueSystem.findActive();
            if(next != null){
                DialogueSystem.active = next;
            }
        }

        //while exiting, keep animating the last-shown objective
        DialogueObjective node = DialogueSystem.active != null ? DialogueSystem.active : shown;
        if(node == null){
            root.visible = false;
            return;
        }

        if(shown != node){
            shown = node;
            typeTimer = 0f;
            charCount = 0;
            entering = true;
            exiting = false;
            enterT = 0f;
            exitT = 0f;
            slideOffset = slideOffset();
            barCell.padLeft(basePad - slideOffset);

            speaker.setText(node.speaker);
            portrait.visible = node.icon != null && !node.icon.isEmpty();
            if(portrait.visible){
                portrait.setDrawable(new TextureRegionDrawable(Core.atlas.find(node.icon)));
            }
            text.setText("");
            options.clearChildren();
            if(node.options != null){
                options.defaults().height(44f);
                for(DialogueOption opt : node.options){
                    TextButton b = new TextButton(opt.text, Styles.cleart);
                    b.clicked(() -> {
                        DialogueSystem.choose(opt);
                        beginExit();
                    });
                    b.getLabel().setFontScale(1.4f);
                    b.getLabel().setWrap(false);
                    options.add(b).left().padRight(28f);
                }
            }
            root.visible = true;
        }

        //slide in on begin, slide out on end
        if(entering){
            enterT += Time.delta / 60f;
            float p = Mathf.clamp(enterT / enterDur);
            barCell.padLeft(basePad - slideOffset * (1f - p));
            if(p >= 1f){
                entering = false;
                barCell.padLeft(basePad);
            }
        }else if(exiting){
            exitT += Time.delta / 60f;
            float p = Mathf.clamp(exitT / exitDur);
            barCell.padLeft(basePad - slideOffset * p);
            if(p >= 1f){
                exiting = false;
                barCell.padLeft(basePad);
                root.visible = false;
                shown = null;
            }
            return;
        }

        //typewriter: reveal the text one character at a time
        if(charCount < node.text.length()){
            typeTimer += Time.delta / 60f;
            int target = Math.min((int)(typeTimer * charRate), node.text.length());
            if(target > charCount){
                charCount = target;
                text.setText(node.text.substring(0, charCount));
            }
        }
    }
}