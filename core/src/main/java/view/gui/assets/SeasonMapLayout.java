package view.gui.assets;

import model.enums.Chapter;

/**
 * Per-season Season Map layout: node anchors, platform sizes, platform offsets.
 * Each chapter has its own class so tuning one never touches the others.
 */
public abstract class SeasonMapLayout {
    public float mapWidth = 2600f;
    public float mapHeight = 1080f;

    /** Marker lift above the node anchor. */
    public float orbLift = 40f;

    /**
     * Marker / path anchors. Index {@code i} = level {@code i + 1}.
     * {@code [0]=X}, {@code [1]=Y}. Moving a row moves the orb (and path joints).
     */
    public float[][] nodeXy;

    /**
     * Platform draw size per level: {@code [i][0]=W}, {@code [i][1]=H}.
     */
    public float[][] platformSizeWh;

    /**
     * Platform offset relative to {@link #nodeXy} for the same index.
     * {@code +X} right, {@code +Y} up — slides the island under the marker.
     */
    public float[][] platformOffsetXy;

    protected SeasonMapLayout(float[][] nodeXy, float[][] platformSizeWh, float[][] platformOffsetXy) {
        this.nodeXy = nodeXy;
        this.platformSizeWh = platformSizeWh;
        this.platformOffsetXy = platformOffsetXy;
    }

    public float platformW(int index) {
        if (platformSizeWh == null || index < 0 || index >= platformSizeWh.length) {
            return 240f;
        }
        return platformSizeWh[index][0];
    }

    public float platformH(int index) {
        if (platformSizeWh == null || index < 0 || index >= platformSizeWh.length) {
            return 180f;
        }
        return platformSizeWh[index][1];
    }

    public float platformOffsetX(int index) {
        if (platformOffsetXy == null || index < 0 || index >= platformOffsetXy.length) {
            return 0f;
        }
        return platformOffsetXy[index][0];
    }

    public float platformOffsetY(int index) {
        if (platformOffsetXy == null || index < 0 || index >= platformOffsetXy.length) {
            return 0f;
        }
        return platformOffsetXy[index][1];
    }

    public static SeasonMapLayout forChapter(Chapter chapter) {
        if (chapter == Chapter.FROSTBITE_CAVES) {
            return new FrostbiteSeasonMapLayout();
        }
        if (chapter == Chapter.BIG_WAVE_BEACH) {
            return new BeachSeasonMapLayout();
        }
        if (chapter == Chapter.DARK_AGES) {
            return new DarkAgesSeasonMapLayout();
        }
        return new EgyptSeasonMapLayout();
    }
}
