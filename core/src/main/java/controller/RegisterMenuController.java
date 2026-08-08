package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.greenhouse.Greenhouse;
import model.news.NewsFactory;
import model.user.PasswordHasher;
import model.user.User;
import model.user.persistance.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class RegisterMenuController extends AppMenuController {
    private static RegisterMenuController instance = null;

    // Pending registration data (step 1 → step 2)
    private String pendingUsername;
    private String pendingPassword;
    private String pendingNickname;
    private String pendingEmail;
    private String pendingGender;

    private RegisterMenuController() {}

    public static RegisterMenuController getInstance() {
        if (instance == null) instance = new RegisterMenuController();
        return instance;
    }

    private UserRepository getRepo() {
        return App.getInstance().getUserRepository();
    }

    // ──────────────────────────────────────────────
    // Abstract overrides
    // ──────────────────────────────────────────────

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        if (menuName.equalsIgnoreCase("login")) {
            if (pendingUsername != null) {
                return CommandResult.error(
                        "Complete your security question first, or start over with a new register command."
                );
            }
            App.getInstance().setCurrentMenu(MenuType.LOGIN);
            return CommandResult.success("Entered login menu.");
        }
        return CommandResult.error("Cannot go to '" + menuName + "' from register menu.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        return CommandResult.success("Exiting application.");
    }

    // ──────────────────────────────────────────────
    // Registration: step 1 — validate fields
    // ──────────────────────────────────────────────

    public CommandResult<Void> register(String username, String password, String passwordConfirm,
                                         String nickname, String email, String gender) {
        // Username
        if (username == null || username.trim().isEmpty())
            return CommandResult.error("Username cannot be empty.");
        if (!username.matches("^[a-zA-Z0-9-]+$"))
            return CommandResult.error("Invalid username. Only letters, numbers, and hyphens allowed.");
        if (getRepo().existsByUsername(username))
            return CommandResult.error("Username '" + username + "' is already taken.");

        // Password
        if (password == null || password.isEmpty())
            return CommandResult.error("Password cannot be empty.");
        String pwErr = validatePassword(password);
        if (pwErr != null)
            return CommandResult.error("Weak password: " + pwErr);
        if (!password.equals(passwordConfirm))
            return CommandResult.error("Password and confirmation do not match.");

        // Nickname
        if (nickname == null || nickname.trim().length() < 3 || nickname.trim().length() > 30)
            return CommandResult.error("Nickname must be between 3 and 30 characters.");

        // Email
        if (email == null || email.trim().isEmpty())
            return CommandResult.error("Email cannot be empty.");
        String emailErr = validateEmail(email);
        if (emailErr != null)
            return CommandResult.error("Invalid email: " + emailErr);
        if (getRepo().existsByEmail(email))
            return CommandResult.error("Email '" + email + "' is already in use.");

        // Gender
        String g = gender != null ? gender.toLowerCase() : "";
        if (!g.equals("male") && !g.equals("female"))
            return CommandResult.error("Gender must be 'male' or 'female'.");

        // All good — store for step 2
        this.pendingUsername = username.trim();
        this.pendingPassword = password;
        this.pendingNickname = nickname.trim();
        this.pendingEmail = email.trim();
        this.pendingGender = g;

        return CommandResult.success("All fields validated. Now choose a security question.");
    }

    // ──────────────────────────────────────────────
    // Registration: step 2 — security question + save
    // ──────────────────────────────────────────────

    public CommandResult<Void> pickQuestion(int questionNumber, String answer, String answerConfirm) {
        if (pendingUsername == null)
            return CommandResult.error("No registration in progress. Start with register command first.");

        if (questionNumber < 1 || questionNumber > 5)
            return CommandResult.error("Question number must be 1-5.");
        if (answer == null || answer.trim().isEmpty())
            return CommandResult.error("Answer cannot be empty.");
        if (!answer.equals(answerConfirm))
            return CommandResult.error("Answers do not match.");

        // Build user
        User user = new User();
        user.setUsername(pendingUsername);
        user.setPasswordHash(PasswordHasher.hash(pendingPassword));
        user.setNickname(pendingNickname);
        user.setEmail(pendingEmail);
        user.setGender(pendingGender);
        user.setSecurityQuestionNumber(questionNumber);
        user.setSecurityAnswer(answer.trim());

        user.setCoins(0);
        user.setGems(0);
        user.setDifficultyLevel(3);
        user.setStayLoggedIn(false);
        user.setGamesPlayed(0);
        user.setHighestMyopoint(0);
        user.setPlantFoodCount(0);
        user.setUnlockedPots(Greenhouse.DEFAULT_UNLOCKED_POTS);

        user.setChapterProgress(new HashMap<>());
        user.setUnlockedPlants(new HashSet<>(User.STARTER_PLANTS));
        user.setUnlockedZombies(new HashSet<>());
        user.setUnlockedMiniGames(new HashSet<>());
        user.setSeedPackets(new HashMap<>());
        user.setPlantLevels(new HashMap<>());
        user.setPlantBoosts(new HashMap<>());
        user.setGreenhousePots(new HashMap<>());
        user.setGreenhousePlantTimestamps(new HashMap<>());
        user.setReadNews(new ArrayList<>());
        user.setNewsPublishDates(new HashMap<>());
        for (String plant : User.STARTER_PLANTS) {
            user.rememberNewsPublishDate(NewsFactory.plantNewsId(plant));
        }
        user.setQuestStatus(new HashMap<>());
        user.setPurchasedDailyDeals(new HashMap<>());

        getRepo().save(user);
        clearPending();

        App.getInstance().setCurrentMenu(MenuType.LOGIN);
        return CommandResult.success("Registration successful! Redirecting to login.");
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private void clearPending() {
        pendingUsername = null;
        pendingPassword = null;
        pendingNickname = null;
        pendingEmail = null;
        pendingGender = null;
    }

    private String validatePassword(String pw) {
        if (pw.length() < 8) return "minimum 8 characters.";
        if (!pw.matches(".*[a-z].*")) return "must include a lowercase letter.";
        if (!pw.matches(".*[A-Z].*")) return "must include an uppercase letter.";
        if (!pw.matches(".*\\d.*")) return "must include a digit.";
        if (!pw.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*"))
            return "must include a special character.";
        return null;
    }

    private String validateEmail(String email) {
        int at = email.indexOf('@');
        int lastAt = email.lastIndexOf('@');
        if (at == -1 || at != lastAt) return "must have exactly one '@'.";

        String local = email.substring(0, at);
        String domain = email.substring(at + 1);

        if (local.isEmpty()) return "local part cannot be empty.";
        if (!local.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]*[a-zA-Z0-9]$") && local.length() > 1)
            return "invalid local part.";
        if (local.length() == 1 && !local.matches("[a-zA-Z0-9]"))
            return "invalid local part.";
        if (local.contains("..")) return "local part cannot have consecutive dots.";

        if (domain.isEmpty()) return "domain cannot be empty.";
        if (!domain.matches("^[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$"))
            return "invalid domain.";
        if (domain.contains("..")) return "domain cannot have consecutive dots.";

        if (email.matches(".*[?><, \"';:\\\\/|\\[\\]{}()+*&^%$#!].*"))
            return "contains forbidden characters.";
        return null;
    }
}
