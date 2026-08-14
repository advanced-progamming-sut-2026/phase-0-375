package view.gui.anim.projectile;

import model.projectile.Projectile;
import view.gui.anim.AnimPose;
import view.gui.assets.ProjectilePamPaths;

/**
 * Shooter projectile defaults: model → PAM path → looping fly clip.
 *
 * <p>Do not mutate the model here. Unmapped projectiles return {@code null}
 * so the lawn renderer can skip or debug-overlay them.
 */
public final class ProjectileAnimAdapter {

    public AnimPose poseFor(Projectile projectile) {
        if (projectile == null) {
            return null;
        }
        String pam = ProjectilePamPaths.pathFor(projectile);
        if (pam == null) {
            return null;
        }
        AnimPose pose = AnimPose.looping(pam, ProjectilePamPaths.CLIP_PREFERENCES[0],
                ProjectileAnimRole.FLYING);
        if (projectile.getDirection() < 0) {
            pose = pose.withFlipX(true);
        }
        return pose;
    }
}
