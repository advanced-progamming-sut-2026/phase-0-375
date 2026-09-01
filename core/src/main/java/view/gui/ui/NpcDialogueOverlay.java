package view.gui.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import model.data.level.NpcDialogueData;
import pvz.libpvz.textures.TextureBank;
import view.gui.audio.GameAudio;
import view.gui.audio.GameSfx;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-objectives NPC dialogue: portrait slides up from a bottom corner,
 * {@code IMAGE_STORE_SPEECHBUBBLE2} pops beside it, tap anywhere advances.
 * Odd-indexed NPCs enter from bottom-right with a horizontally flipped bubble.
 */
public final class NpcDialogueOverlay extends Table {
    public static final String ATLAS_GROUP = "NPC_Common";
    public static final String ATLAS_PAGE = "ATLASIMAGE_ATLAS_NPC_COMMON_768_00";
    public static final String SPEECH_BUBBLE_ID = "IMAGE_STORE_SPEECHBUBBLE2";

    private static final String DIALOGUE_FONT = "BRIANNETOD";
    private static final String CONTINUE_FONT = "FBUSV8C5EI_2";
    private static final Color TEXT_COLOR = new Color(0.15f, 0.1f, 0.05f, 1f);
    private static final Color CONTINUE_COLOR = new Color(0.5f, 0.5f, 0.5f, 1f);

    private static final float NPC_WIDTH = 260f;
    private static final float BUBBLE_WIDTH = 360f;
    private static final float BUBBLE_PAD_X = 36f;
    private static final float BUBBLE_PAD_TOP = 28f;
    private static final float BUBBLE_PAD_BOTTOM = 36f;
    private static final float CORNER_PAD_X = 28f;
    private static final float CORNER_PAD_Y = 18f;
    private static final float SLIDE_SEC = 0.55f;
    private static final float BUBBLE_POP_SEC = 0.28f;
    private static final float EXIT_BUBBLE_SEC = 0.18f;
    private static final float EXIT_SLIDE_SEC = 0.4f;
    private static final float DIALOGUE_FONT_SCALE = 2.2f;
    private static final float BOB_PX = 3.5f;
    private static final float BOB_SEC = 0.7f;

    /**
     * Seconds after the first NPC slides in before the intro voice plays.
     * Tune in-game, then set the value you like here.
     */
    public static float RISE_AND_SHINE_VOICE_DELAY_SEC = 0.45f;

    private final Skin skin;
    private final TextureBank textures;
    private final FileHandle assetsRoot;
    private final List<NpcDialogueData.NpcEntry> npcs;
    private final Runnable onComplete;
    private final GameSfx introVoice;
    private final List<Texture> ownedTextures = new ArrayList<>();

    private int currentNpcIndex;
    private Group npcRoot;
    private Group bubbleRoot;
    private boolean advancing;
    private boolean introVoicePlayed;
    private float restX;
    private float restY;
    private float offY;

    public NpcDialogueOverlay(Skin skin, TextureBank textures, FileHandle assetsRoot,
                              List<NpcDialogueData.NpcEntry> npcs, Runnable onComplete) {
        this(skin, textures, assetsRoot, npcs, null, onComplete);
    }

    public NpcDialogueOverlay(Skin skin, TextureBank textures, FileHandle assetsRoot,
                              List<NpcDialogueData.NpcEntry> npcs,
                              GameSfx introVoice, Runnable onComplete) {
        this.skin = skin;
        this.textures = textures;
        this.assetsRoot = assetsRoot;
        this.npcs = npcs;
        this.introVoice = introVoice;
        this.onComplete = onComplete;

        setFillParent(true);
        setTouchable(Touchable.enabled);
        ensureAtlas(textures);

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                advanceDialogue();
            }
        });

        if (npcs != null && !npcs.isEmpty()) {
            showNpc(0);
        } else {
            finish();
        }
    }

    private void showNpc(int index) {
        if (index >= npcs.size()) {
            finish();
            return;
        }
        clearChildren();
        advancing = false;
        currentNpcIndex = index;
        playIntroVoiceIfNeeded(index);
        boolean fromRight = index % 2 == 1;
        NpcDialogueData.NpcEntry npc = npcs.get(index);
        Image npcImage = loadNpcImage(npc.getImagePath());
        if (npcImage == null) {
            finish();
            return;
        }
        layoutNpc(fromRight, npc, npcImage);
    }

    private void playIntroVoiceIfNeeded(int index) {
        if (index != 0 || introVoice == null || introVoicePlayed) {
            return;
        }
        introVoicePlayed = true;
        float delay = introVoice == GameSfx.RISE_AND_SHINE_DR_ZARRABI
                ? RISE_AND_SHINE_VOICE_DELAY_SEC
                : 0f;
        addAction(Actions.sequence(
                Actions.delay(delay),
                Actions.run(() -> GameAudio.get().playSfx(introVoice))));
    }

    private void layoutNpc(boolean fromRight, NpcDialogueData.NpcEntry npc, Image npcImage) {
        float npcW = NPC_WIDTH;
        float npcH = npcW * (npcImage.getHeight() / Math.max(1f, npcImage.getWidth()));
        npcImage.setSize(npcW, npcH);
        bubbleRoot = createSpeechBubble(npc.getDialogueLines(), npc.getContinueText(), fromRight);
        float bubbleW = bubbleRoot.getWidth();
        float bubbleLocalX = fromRight ? -bubbleW + npcW * 0.45f : npcW * 0.55f;
        float bubbleLocalY = npcH * 0.65f;
        bubbleRoot.setPosition(bubbleLocalX, bubbleLocalY);
        float minX = Math.min(0f, bubbleLocalX);
        float contentW = Math.max(npcW, bubbleLocalX + bubbleW) - minX;
        float contentH = Math.max(npcH, bubbleLocalY + bubbleRoot.getHeight());
        assembleNpcRoot(fromRight, npcImage, minX, contentW, contentH, bubbleLocalX);
        animateNpcEntrance();
    }

    private void assembleNpcRoot(boolean fromRight, Image npcImage, float minX,
                                 float contentW, float contentH, float bubbleLocalX) {
        npcRoot = new Group();
        npcRoot.setTransform(true);
        npcRoot.setTouchable(Touchable.disabled);
        npcRoot.setSize(contentW, contentH);
        npcImage.setPosition(-minX, 0f);
        bubbleRoot.setX(bubbleLocalX - minX);
        npcRoot.addActor(npcImage);
        npcRoot.addActor(bubbleRoot);
        float stageW = getWidth() > 1f ? getWidth()
            : getStage() != null ? getStage().getWidth() : 1302f;
        restY = CORNER_PAD_Y;
        restX = fromRight ? stageW - CORNER_PAD_X - contentW : CORNER_PAD_X;
        offY = -contentH - 20f;
        npcRoot.setPosition(restX, offY);
        addActor(npcRoot);
    }

    private void animateNpcEntrance() {
        bubbleRoot.setOrigin(Align.center);
        bubbleRoot.setScale(0f);
        bubbleRoot.getColor().a = 0f;
        npcRoot.addAction(Actions.sequence(
            Actions.moveTo(restX, restY, SLIDE_SEC, Interpolation.sineOut),
            Actions.run(this::popBubble),
            Actions.run(this::startBob)
        ));
    }

    private void popBubble() {
        if (bubbleRoot == null) {
            return;
        }
        bubbleRoot.clearActions();
        bubbleRoot.addAction(Actions.parallel(
            Actions.fadeIn(BUBBLE_POP_SEC * 0.6f),
            Actions.scaleTo(1f, 1f, BUBBLE_POP_SEC, Interpolation.swingOut)
        ));
    }

    private void startBob() {
        if (npcRoot == null || advancing) {
            return;
        }
        npcRoot.addAction(Actions.forever(Actions.sequence(
            Actions.moveBy(0f, BOB_PX, BOB_SEC, Interpolation.sine),
            Actions.moveBy(0f, -BOB_PX, BOB_SEC, Interpolation.sine)
        )));
    }

    private Group createSpeechBubble(List<String> dialogueLines, String continueText, boolean flipX) {
        Group root = new Group();
        root.setTransform(true);
        root.setTouchable(Touchable.disabled);

        TextureRegion region = textures != null ? textures.region(SPEECH_BUBBLE_ID) : null;
        Image bg;
        float w;
        float h;
        if (region != null) {
            TextureRegion drawn = new TextureRegion(region);
            if (flipX) {
                drawn.flip(true, false);
            }
            bg = new Image(new TextureRegionDrawable(drawn));
            w = BUBBLE_WIDTH;
            h = w * (drawn.getRegionHeight() / (float) Math.max(1, drawn.getRegionWidth()));
        } else {
            bg = new Image();
            w = BUBBLE_WIDTH;
            h = BUBBLE_WIDTH * 0.65f;
            Gdx.app.error("NpcDialogueOverlay", "Missing region " + SPEECH_BUBBLE_ID);
        }
        bg.setSize(w, h);
        root.setSize(w, h);
        root.addActor(bg);

        BitmapFont dialogueFont = SkinFonts.getScaled(skin, DIALOGUE_FONT, DIALOGUE_FONT_SCALE);
        Label.LabelStyle dialogueStyle = new Label.LabelStyle(dialogueFont, TEXT_COLOR);
        Label dialogue = new Label(joinLines(dialogueLines), dialogueStyle);
        dialogue.setAlignment(Align.center);
        dialogue.setWrap(true);

        BitmapFont continueFont = resolveFont(CONTINUE_FONT);
        Label.LabelStyle continueStyle = new Label.LabelStyle(continueFont, CONTINUE_COLOR);
        String hint = continueText != null && !continueText.isEmpty() ? continueText : "Press to continue";
        Label cont = new Label(hint, continueStyle);
        cont.setAlignment(Align.center);

        Table text = new Table();
        text.setSize(w, h);
        text.pad(BUBBLE_PAD_TOP, BUBBLE_PAD_X, BUBBLE_PAD_BOTTOM, BUBBLE_PAD_X);
        text.add(dialogue).grow().width(w - BUBBLE_PAD_X * 2f).row();
        text.add(cont).padTop(6f);
        root.addActor(text);

        return root;
    }

    private static String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private Image loadNpcImage(String imagePath) {
        try {
            FileHandle file = resolveNpcImage(imagePath);
            if (file == null || !file.exists()) {
                Gdx.app.error("NpcDialogueOverlay", "Missing NPC image: " + imagePath);
                return null;
            }
            Texture texture = new Texture(file);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            ownedTextures.add(texture);
            return new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        } catch (Exception e) {
            Gdx.app.error("NpcDialogueOverlay", "Failed to load NPC image: " + imagePath, e);
            return null;
        }
    }

    private FileHandle resolveNpcImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        if (assetsRoot != null) {
            FileHandle fromRoot = assetsRoot.child(imagePath);
            if (fromRoot.exists()) {
                return fromRoot;
            }
        }
        FileHandle local = Gdx.files.local("assets/" + imagePath);
        if (local.exists()) {
            return local;
        }
        FileHandle bare = Gdx.files.local(imagePath);
        return bare.exists() ? bare : local;
    }

    private BitmapFont resolveFont(String name) {
        try {
            BitmapFont font = SkinFonts.linear(skin, name);
            if (font != null) {
                return font;
            }
        } catch (Exception ignored) {
        }
        try {
            Label.LabelStyle style = skin.get("medium", Label.LabelStyle.class);
            if (style != null && style.font != null) {
                return SkinFonts.linear(style.font);
            }
        } catch (Exception ignored) {
        }
        for (BitmapFont f : skin.getAll(BitmapFont.class).values()) {
            if (f != null) {
                return SkinFonts.linear(f);
            }
        }
        throw new IllegalStateException("No BitmapFont found in skin");
    }

    private void advanceDialogue() {
        if (advancing || npcRoot == null) {
            return;
        }
        advancing = true;
        npcRoot.clearActions();
        if (bubbleRoot != null) {
            bubbleRoot.clearActions();
        }

        Runnable afterBubble = () -> {
            if (npcRoot == null) {
                showNpc(currentNpcIndex + 1);
                return;
            }
            npcRoot.addAction(Actions.sequence(
                Actions.moveTo(restX, offY, EXIT_SLIDE_SEC, Interpolation.sineIn),
                Actions.run(() -> showNpc(currentNpcIndex + 1))
            ));
        };

        if (bubbleRoot != null) {
            bubbleRoot.addAction(Actions.sequence(
                Actions.parallel(
                    Actions.fadeOut(EXIT_BUBBLE_SEC),
                    Actions.scaleTo(0.2f, 0.2f, EXIT_BUBBLE_SEC, Interpolation.sineIn)
                ),
                Actions.run(afterBubble)
            ));
        } else {
            afterBubble.run();
        }
    }

    private void finish() {
        disposeOwnedTextures();
        if (onComplete != null) {
            onComplete.run();
        }
        remove();
    }

    private void disposeOwnedTextures() {
        for (Texture t : ownedTextures) {
            t.dispose();
        }
        ownedTextures.clear();
    }

    private static void ensureAtlas(TextureBank textures) {
        if (textures == null) {
            return;
        }
        textures.loadSync(ATLAS_GROUP);
        textures.loadSync(ATLAS_PAGE);
    }
}
