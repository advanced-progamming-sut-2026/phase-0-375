package model.data.level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads NPC dialogue configurations from npcs.json.
 */
public final class NpcDialogueRegistry {
    private static NpcDialogueRegistry instance;
    private final Map<String, NpcDialogueData> dialogues = new HashMap<>();
    private boolean loaded = false;

    private NpcDialogueRegistry() {}

    public static NpcDialogueRegistry getInstance() {
        if (instance == null) {
            instance = new NpcDialogueRegistry();
        }
        return instance;
    }

    public void load() {
        if (loaded) {
            return;
        }
        
        try {
            FileHandle file = Gdx.files.internal("data/levels/npcs.json");
            if (file.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, NpcDialogueData> data = mapper.readValue(
                    file.readString(), 
                    mapper.getTypeFactory().constructMapType(HashMap.class, String.class, NpcDialogueData.class)
                );
                dialogues.putAll(data);
            }
            loaded = true;
        } catch (Exception e) {
            Gdx.app.error("NpcDialogueRegistry", "Failed to load NPC dialogues", e);
            loaded = true; // Mark as loaded even on failure to avoid repeated attempts
        }
    }

    public NpcDialogueData getDialogue(String chapter, int levelId) {
        if (!loaded) {
            load();
        }
        
        String key = chapter + "_" + levelId;
        return dialogues.get(key);
    }

    public boolean hasDialogue(String chapter, int levelId) {
        if (!loaded) {
            load();
        }
        
        String key = chapter + "_" + levelId;
        return dialogues.containsKey(key);
    }
}
