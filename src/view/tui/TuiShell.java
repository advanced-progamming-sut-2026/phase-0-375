package view.tui;

import model.app.App;
import model.enums.MenuType;
import model.game.core.GameModel;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Full-screen terminal UI shell built on JLine 3.
 *  Layout, top to bottom:
 *   header/status bar (menu, tick, sun, plant food)
 *   live map panel (only while in a game)
 *   scrolling log of command output
 *   input box (always present at the bottom)
 */
public final class TuiShell {

    private static final int LOG_CAPACITY = 500;
    private static final int MIN_LOG_ROWS = 4;
    private static final String PROMPT = "\u276F ";

    private static TuiShell active;

    /** @return the running shell, or {@code null} when in plain CLI mode. */
    public static TuiShell getActive() {
        return active;
    }

    /**
     * Starts the TUI on the system terminal.
     */
    public static boolean tryStart() {
        if (active != null) return true;
        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();
            if (Terminal.TYPE_DUMB.equals(terminal.getType())
                    || Terminal.TYPE_DUMB_COLOR.equals(terminal.getType())) {
                terminal.close();
                return false;
            }
            active = new TuiShell(terminal);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** Stops the TUI and restores the normal terminal screen. */
    public static void stop() {
        if (active != null) {
            active.close();
            active = null;
        }
    }

    private final Terminal terminal;
    private final LineReader lineReader;
    private final Display display;
    private final Deque<AttributedString> log = new ArrayDeque<>();

    private TuiShell(Terminal terminal) {
        this.terminal = terminal;
        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .option(LineReader.Option.ERASE_LINE_ON_FINISH, true)
                .build();
        this.display = new Display(terminal, true);
        terminal.puts(InfoCmp.Capability.enter_ca_mode); // alternate screen buffer
        terminal.flush();
    }

    private void close() {
        try {
            terminal.puts(InfoCmp.Capability.exit_ca_mode);
            terminal.flush();
            terminal.close();
        } catch (IOException e) {
            // Shutting down; nothing sensible left to do.
        }
    }

    // Log panel

    /** Appends an informational message to the scrolling log. */
    public void log(String message) {
        append(message, AttributedStyle.DEFAULT);
    }

    /** Appends an error message (red) to the scrolling log. */
    public void logError(String message) {
        append(message, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
    }

    private void logCommand(String command) {
        append(PROMPT + command, AttributedStyle.DEFAULT.faint());
    }

    private void append(String message, AttributedStyle style) {
        if (message == null) return;
        for (String line : message.split("\\R")) {
            log.addLast(new AttributedString(line, style));
            if (log.size() > LOG_CAPACITY) log.removeFirst();
        }
    }

    // Input

    /**
     * Repaints the frame, then blocks in the bottom input box until the user
     * submits one command.
     *
     * @return the raw command line, or {@code null} on Ctrl-C / Ctrl-D.
     */
    public String readCommand() {
        renderFrame();
        try {
            String line = lineReader.readLine(PROMPT);
            if (line != null && !line.isBlank()) {
                logCommand(line.trim());
            }
            return line;
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        } finally {
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.flush();
            display.reset();
        }
    }

    // Rendering

    /** Repaints the whole screen from the current App / GameModel state. */
    public void renderFrame() {
        Size size = terminal.getSize();
        int rows = Math.max(size.getRows(), 10);
        int cols = Math.max(size.getColumns(), 40);

        // Fixed rows: header, log divider, input box top border, input row, bottom border.
        int contentRows = rows - 5;

        GameModel model = currentGameModel();
        List<AttributedString> mapLines = model != null ? MapView.render(model) : List.of();

        int mapRows = Math.min(mapLines.size(), Math.max(contentRows - MIN_LOG_ROWS, 0));
        int logRows = contentRows - mapRows;

        List<AttributedString> lines = new ArrayList<>(rows);
        lines.add(headerLine(cols));
        for (int i = 0; i < mapRows; i++) {
            lines.add(clip(mapLines.get(i), cols));
        }
        lines.add(divider(" Log ", cols));
        List<AttributedString> tail = lastLogLines(logRows);
        for (int i = 0; i < logRows; i++) {
            lines.add(i < tail.size() ? clip(tail.get(i), cols) : AttributedString.EMPTY);
        }
        lines.add(divider("", cols));       // input box top border
        lines.add(AttributedString.EMPTY);  // input row: the line reader draws here
        lines.add(divider("", cols));       // input box bottom border

        int inputRow = rows - 2;
        display.resize(rows, cols);
        display.update(lines, size.cursorPos(inputRow, 0));
    }

    private GameModel currentGameModel() {
        App app = App.getInstance();
        if (app.getCurrentMenu() != MenuType.IN_GAME) return null;
        return app.getCurrentGameModel();
    }

    private AttributedString headerLine(int cols) {
        App app = App.getInstance();
        StringBuilder sb = new StringBuilder(" PvZ2 \u2502 ");
        sb.append(app.getCurrentMenu().name().replace('_', ' '));
        GameModel model = currentGameModel();
        if (model != null) {
            sb.append(" \u2502 Tick ").append(model.getTick())
                    .append(" \u2502 Sun ").append(model.getSunAmount())
                    .append(" \u2502 Food ").append(model.getPlantFoodCount())
                    .append(" \u2502 ").append(model.getState());
        }
        while (sb.length() < cols) sb.append(' ');
        return new AttributedString(sb.substring(0, cols), AttributedStyle.DEFAULT.inverse());
    }

    private AttributedString divider(String label, int cols) {
        StringBuilder sb = new StringBuilder("\u2500");
        sb.append(label);
        while (sb.length() < cols) sb.append('\u2500');
        return new AttributedString(sb.substring(0, cols), AttributedStyle.DEFAULT.faint());
    }

    private AttributedString clip(AttributedString line, int cols) {
        return line.columnLength() > cols ? line.columnSubSequence(0, cols) : line;
    }

    private List<AttributedString> lastLogLines(int n) {
        List<AttributedString> result = new ArrayList<>(Math.min(Math.max(n, 0), log.size()));
        int skip = Math.max(log.size() - n, 0);
        int i = 0;
        for (AttributedString line : log) {
            if (i++ >= skip) result.add(line);
        }
        return result;
    }
}
