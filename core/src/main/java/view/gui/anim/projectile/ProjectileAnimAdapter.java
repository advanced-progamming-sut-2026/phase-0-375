package view.gui.anim.projectile;

import model.projectile.FumeCloud;
import model.projectile.Projectile;
import view.gui.anim.AnimPose;
import view.gui.assets.PlantSpritesheetCatalog;
import view.gui.assets.ProjectilePamPaths;

/**
 * Projectile defaults: model → PAM path → looping fly clip (or PNG spritesheet).
 *
 * <p>Do not mutate the model here. Unmapped projectiles return {@code null}
 * so the lawn renderer can skip or debug-overlay them.
 */
public final class ProjectileAnimAdapter {
    private final PlantSpritesheetCatalog sheets;

    public ProjectileAnimAdapter() {
        this(null);
    }

    public ProjectileAnimAdapter(PlantSpritesheetCatalog sheets) {
        this.sheets = sheets;
    }

    public AnimPose poseFor(Projectile projectile) {
        if (projectile == null) {
            return null;
        }
        String pam = ProjectilePamPaths.pathFor(projectile);
        if (pam == null) {
            return null;
        }
        if (ProjectilePamPaths.isSpritesheetPath(pam)) {
            return sheetPose(pam, projectile);
        }
        if (projectile instanceof FumeCloud) {
            return AnimPose.once(pam, "special", ProjectileAnimRole.FLYING);
        }
        if (ProjectilePamPaths.ELECTRIC_BLUEBERRY.equals(pam)) {
            return AnimPose.looping(pam, ProjectilePamPaths.ELECTRIC_BLUEBERRY_CLIP,
                    ProjectileAnimRole.FLYING);
        }
        if (ProjectilePamPaths.GRAPESHOT.equals(pam)) {
            return AnimPose.looping(pam, ProjectilePamPaths.GRAPESHOT_CLIP,
                    ProjectileAnimRole.FLYING);
        }
        if (ProjectilePamPaths.GOO_PEA.equals(pam)) {
            AnimPose goo = AnimPose.looping(pam, ProjectilePamPaths.GOO_PEA_FLY_CLIP,
                    ProjectileAnimRole.FLYING);
            return projectile.getDirection() < 0 ? goo.withFlipX(true) : goo;
        }
        String clip = projectile.isButter() && ProjectilePamPaths.KERNEL_PULT.equals(pam)
                ? ProjectilePamPaths.KERNEL_BUTTER_CLIP
                : ProjectilePamPaths.CLIP_PREFERENCES[0];
        AnimPose pose = AnimPose.looping(pam, clip, ProjectileAnimRole.FLYING);
        if (projectile.getDirection() < 0) {
            pose = pose.withFlipX(true);
        }
        return pose;
    }

    private AnimPose sheetPose(String assetPath, Projectile projectile) {
        if (sheets == null) {
            return null;
        }
        PlantSpritesheetCatalog.ClipSpec spec =
                sheets.resolveAssetPath(assetPath, ProjectilePamPaths.CLIP_PREFERENCES);
        if (spec == null) {
            return null;
        }
        AnimPose pose = AnimPose.sheetLooping(spec.relativePath(), spec.cacheKey(),
                ProjectileAnimRole.FLYING);
        if (projectile.getDirection() < 0) {
            pose = pose.withFlipX(true);
        }
        return pose;
    }
}
