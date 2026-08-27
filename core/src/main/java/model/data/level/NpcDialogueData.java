package model.data.level;

import java.util.List;

/** Raw JSON DTO for NPC dialogue configuration per level. */
public class NpcDialogueData {
    private List<NpcEntry> npcs;

    public List<NpcEntry> getNpcs() {
        return npcs;
    }

    public void setNpcs(List<NpcEntry> npcs) {
        this.npcs = npcs;
    }

    public static class NpcEntry {
        private String imagePath;
        private List<String> dialogueLines;
        private String continueText = "Press to continue";

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public List<String> getDialogueLines() {
            return dialogueLines;
        }

        public void setDialogueLines(List<String> dialogueLines) {
            this.dialogueLines = dialogueLines;
        }

        public String getContinueText() {
            return continueText;
        }

        public void setContinueText(String continueText) {
            this.continueText = continueText;
        }
    }
}
