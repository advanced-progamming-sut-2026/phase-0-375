package model.game.level.special;

import model.app.App;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.RegularLevel;
import model.game.score.MyopointTracker;
import model.user.User;

/**
 * The daily "Myopoint" score game: plays like a regular level (survive every
 * wave, lose on a house breach) while a {@link MyopointTracker} scores
 * stylish play. The final score is written back to the player's profile when
 * the game ends (win or lose) and feeds the leaderboard's MyoPoint column.
 */
public class ScoreLevel extends RegularLevel {

    private final MyopointTracker tracker = new MyopointTracker();
    private boolean scored;
    private boolean newPersonalBest;
    private int previousPersonalBest;

    public ScoreLevel(LevelConfig config) {
        super(config);
    }

    public MyopointTracker getTracker() {
        return tracker;
    }

    /** True after scoring if this run beat the player's prior Myopoint best. */
    public boolean isNewPersonalBest() {
        return newPersonalBest;
    }

    /** Profile best before this run was written back (0 if never scored). */
    public int getPreviousPersonalBest() {
        return previousPersonalBest;
    }

    @Override
    public void onStart() {
        super.onStart();
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model != null) {
            model.setMyopointTracker(tracker);
        }
    }

    @Override
    public void tick(float deltaTime) {
        tracker.tick(App.getInstance().getCurrentGameModel(), deltaTime);
    }

    /** Unlike a regular level, winning does not advance any chapter progress. */
    @Override
    public void onComplete() {
        getConfig().setCompleted(true);
        finishScoring(true);
    }

    @Override
    public void onFail() {
        finishScoring(false);
    }

    /** Finalises the score and persists the player's personal best. */
    private void finishScoring(boolean won) {
        if (scored) return;
        scored = true;

        tracker.onGameFinished(App.getInstance().getCurrentGameModel(), won);

        System.out.println("== Myopoint summary ==");
        for (String line : tracker.getSummaryLines()) {
            System.out.println(line);
        }

        User user = App.getInstance().getCurrentUser();
        if (user == null) return;
        previousPersonalBest = user.getHighestMyopoint();
        App.getInstance().getUserRepository()
                .updateHighestMyopoint(user.getUsername(), tracker.getTotalPoints());
        if (tracker.getTotalPoints() > previousPersonalBest) {
            newPersonalBest = true;
            System.out.println("New personal best: " + tracker.getTotalPoints() + " Myopoints!");
        }
    }
}
