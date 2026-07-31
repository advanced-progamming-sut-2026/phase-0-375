package controller.result;

public class CommandResult<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String nextMenu;

    private CommandResult(boolean success, String message, T data, String nextMenu) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.nextMenu = nextMenu;
    }

    // --- Static Factory Methods ---

    public static CommandResult<Void> success(String message) {
        return new CommandResult<>(true, message, null, null);
    }

    public static CommandResult<Void> success(String message, String nextMenu) {
        return new CommandResult<>(true, message, null, nextMenu);
    }

    public static <T> CommandResult<T> successWithData(String message, T data) {
        return new CommandResult<>(true, message, data, null);
    }

    public static CommandResult<Void> error(String errorMessage) {
        return new CommandResult<>(false, errorMessage, null, null);
    }

    public static <T> CommandResult<T> errorTyped(String errorMessage) {
        return new CommandResult<>(false, errorMessage, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getNextMenu() {
        return nextMenu;
    }
}