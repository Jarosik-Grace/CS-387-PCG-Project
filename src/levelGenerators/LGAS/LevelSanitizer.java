package levelGenerators.LGAS;

import engine.core.MarioLevelModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * LevelSanitizer (Person 3)
 *
 * Cleans up a level produced by the Markov / n-gram generator so it is
 * playable. Markov chains stitch columns together without knowing that some
 * objects span several columns, which causes two common bugs:
 *
 *   1. Orphan pipe halves - a pipe is 2 tiles wide, so if the chain picks the
 *      "left half" column and then a column without the right half, the pipe
 *      is cut in half.
 *   2. Impossible pits - two "gap" columns-sequences chained together can make
 *      a pit wider than Mario can jump.
 *
 * Fixes, each done as a left-to-right pass over the columns:
 *
 *   - Pits:  any run of more than MAX_PIT_WIDTH pit columns is filled with
 *            ground ('X'), using the same ground depth as the column to its left.
 *   - Pipes: pipe tiles are paired up row by row ('t'/'T' tiles in pairs, and
 *            '<' + '>' / '[' + ']'). Any tile without a partner is replaced
 *            with '-'. Then any pipe tile left floating with nothing under it
 *            is removed too.
 *
 * The pipe pass is repeated until nothing changes (usually 1-2 times), because
 * removing a floating pipe tile can leave a new orphan behind.
 *
 * Usage from the generator:
 *     LevelSanitizer.sanitize(model);            // MarioLevelModel, in place
 *     String clean = LevelSanitizer.sanitize(s); // or on a level string
 *
 * Usage from the command line (for testing):
 *     java -cp src levelGenerators.LGAS.LevelSanitizer levels/LGAS/lvl-1.txt
 */
public class LevelSanitizer {

    /** Widest pit (in columns) that we allow. Wider pits get bridged. */
    public static final int MAX_PIT_WIDTH = 4;

    /**
     * A column is a pit if none of its bottom PIT_SCAN_ROWS rows has anything
     * solid Mario could land on (ground, pipe, block, or a '%' platform).
     * We look at more than just the bottom row because sky levels (e.g.
     * original lvl-6) have no ground at all - you walk on raised platforms.
     * Blocks very high up (top rows) don't count, Mario can't use them to
     * cross a gap.
     */
    private static final int PIT_SCAN_ROWS = 10;

    /** Ground depth to use when we can't copy it from a neighbouring column. */
    private static final int DEFAULT_GROUND_DEPTH = 2;

    private static final char EMPTY = '-';
    private static final char GROUND = 'X';

    /** Counts of what the sanitizer changed, handy for the eval script. */
    public static class Report {
        public int pitsBridged = 0;
        public int pitColumnsFilled = 0;
        public int orphanPipeTilesRemoved = 0;
        public int floatingPipeTilesRemoved = 0;

        public boolean changedAnything() {
            return pitsBridged + orphanPipeTilesRemoved + floatingPipeTilesRemoved > 0;
        }

        @Override
        public String toString() {
            return "pits bridged: " + pitsBridged
                    + " (" + pitColumnsFilled + " columns filled)"
                    + ", orphan pipe tiles removed: " + orphanPipeTilesRemoved
                    + ", floating pipe tiles removed: " + floatingPipeTilesRemoved;
        }
    }

    // ------------------------------------------------------------------
    // Public entry points
    // ------------------------------------------------------------------

    /** Sanitize a MarioLevelModel in place. */
    public static Report sanitize(MarioLevelModel model) {
        int w = model.getWidth();
        int h = model.getHeight();
        char[][] grid = new char[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                grid[y][x] = model.getBlock(x, y);
            }
        }
        Report report = sanitize(grid);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                model.setBlock(x, y, grid[y][x]);
            }
        }
        return report;
    }

    /** Sanitize a level string (rows separated by newlines). Returns the clean level. */
    public static String sanitize(String level) {
        char[][] grid = toGrid(level);
        sanitize(grid);
        return toLevelString(grid);
    }

    /**
     * Sanitize a grid in place. grid[y][x], y = 0 is the top row,
     * every row must have the same length.
     */
    public static Report sanitize(char[][] grid) {
        Report report = new Report();
        if (grid.length == 0 || grid[0].length == 0) {
            return report;
        }
        // Pits first: bridging adds ground, which can give support to pipes.
        bridgeWidePits(grid, report);

        boolean changed = true;
        while (changed) {
            int before = report.orphanPipeTilesRemoved + report.floatingPipeTilesRemoved;
            removeOrphanPipeHalves(grid, report);
            removeFloatingPipes(grid, report);
            changed = (report.orphanPipeTilesRemoved + report.floatingPipeTilesRemoved) != before;
        }
        return report;
    }

    // ------------------------------------------------------------------
    // Pit bridging
    // ------------------------------------------------------------------

    private static void bridgeWidePits(char[][] grid, Report report) {
        int w = grid[0].length;
        int x = 0;
        while (x < w) {
            if (!isPitColumn(grid, x)) {
                x++;
                continue;
            }
            // Found the start of a pit; walk to its end.
            int start = x;
            while (x < w && isPitColumn(grid, x)) {
                x++;
            }
            int end = x - 1; // inclusive
            int width = end - start + 1;

            if (width > MAX_PIT_WIDTH) {
                int depth = groundDepthNear(grid, start, end);
                for (int col = start; col <= end; col++) {
                    fillGround(grid, col, depth);
                }
                report.pitsBridged++;
                report.pitColumnsFilled += width;
            }
        }
    }

    /** True if the bottom PIT_SCAN_ROWS rows of this column have nothing to stand on. */
    private static boolean isPitColumn(char[][] grid, int x) {
        int h = grid.length;
        for (int y = h - 1; y >= Math.max(0, h - PIT_SCAN_ROWS); y--) {
            if (isSolid(grid[y][x])) {
                return false;
            }
        }
        return true;
    }

    /** Ground depth of the column just left of the pit (or right, if the pit starts at x=0). */
    private static int groundDepthNear(char[][] grid, int pitStart, int pitEnd) {
        int w = grid[0].length;
        int depth = 0;
        if (pitStart > 0) {
            depth = groundDepth(grid, pitStart - 1);
        }
        if (depth == 0 && pitEnd < w - 1) {
            depth = groundDepth(grid, pitEnd + 1);
        }
        if (depth == 0) {
            depth = DEFAULT_GROUND_DEPTH;
        }
        // Don't copy huge stacks (e.g. a tall ground pillar) into the bridge.
        return Math.min(depth, 4);
    }

    /** Number of consecutive 'X' tiles counted up from the bottom of a column. */
    private static int groundDepth(char[][] grid, int x) {
        int depth = 0;
        for (int y = grid.length - 1; y >= 0 && grid[y][x] == GROUND; y--) {
            depth++;
        }
        return depth;
    }

    /** Put 'X' in the bottom `depth` rows of a column (only replaces non-solid tiles). */
    private static void fillGround(char[][] grid, int x, int depth) {
        int h = grid.length;
        for (int y = h - 1; y >= Math.max(0, h - depth); y--) {
            if (!isSolid(grid[y][x])) {
                grid[y][x] = GROUND;
            }
        }
    }

    // ------------------------------------------------------------------
    // Pipe repair
    // ------------------------------------------------------------------

    /**
     * One left-to-right pass per row that pairs pipe tiles up.
     *
     * 't'/'T' pipes: the engine draws a run of these as left, right, left,
     * right, ... so a valid run has an even length. A lone tile, or the last
     * tile of an odd run, is an orphan half.
     *
     * Bracket pipes: '<' needs a '>' right after it, and '[' needs a ']'.
     * Any '<', '>', '[' or ']' without its partner is an orphan.
     */
    private static void removeOrphanPipeHalves(char[][] grid, Report report) {
        int h = grid.length;
        int w = grid[0].length;
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                char c = grid[y][x];

                if (isTPipe(c)) {
                    int start = x;
                    while (x < w && isTPipe(grid[y][x])) {
                        x++;
                    }
                    int runLength = x - start;
                    if (runLength % 2 == 1) {
                        // Drop the last tile so the run pairs up evenly.
                        grid[y][x - 1] = EMPTY;
                        report.orphanPipeTilesRemoved++;
                    }
                    continue;
                }

                if (c == '<' || c == '[') {
                    char partner = (c == '<') ? '>' : ']';
                    if (x + 1 < w && grid[y][x + 1] == partner) {
                        x += 2; // valid pair, skip both
                    } else {
                        grid[y][x] = EMPTY;
                        report.orphanPipeTilesRemoved++;
                        x++;
                    }
                    continue;
                }

                if (c == '>' || c == ']') {
                    // A right half we reach here never had a left half before it.
                    grid[y][x] = EMPTY;
                    report.orphanPipeTilesRemoved++;
                }
                x++;
            }
        }
    }

    /**
     * Remove pipe tiles with nothing under them (for example, the top half of
     * a pipe whose bottom was removed as an orphan, or a pipe that hung over a
     * pit). Scans each column bottom-up so a whole floating pipe gets removed.
     */
    private static void removeFloatingPipes(char[][] grid, Report report) {
        int h = grid.length;
        int w = grid[0].length;
        for (int x = 0; x < w; x++) {
            for (int y = h - 2; y >= 0; y--) { // bottom row always counts as supported
                if (isPipe(grid[y][x]) && !isSolid(grid[y + 1][x])) {
                    grid[y][x] = EMPTY;
                    report.floatingPipeTilesRemoved++;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Tile helpers
    // ------------------------------------------------------------------

    private static boolean isTPipe(char c) {
        return c == 't' || c == 'T';
    }

    private static boolean isPipe(char c) {
        return isTPipe(c) || c == '<' || c == '>' || c == '[' || c == ']';
    }

    /** Same list the engine uses in MarioLevel.isSolid(), plus the bullet bill tiles. */
    private static boolean isSolid(char c) {
        switch (c) {
            case 'X': case '#': case '@': case '!': case 'B': case 'b': case 'C':
            case 'Q': case '<': case '>': case '[': case ']': case '?':
            case 'S': case 'U': case 'D': case '%': case 't': case 'T':
            case 'L': case '*':
                return true;
            default:
                return false;
        }
    }

    // ------------------------------------------------------------------
    // String <-> grid
    // ------------------------------------------------------------------

    public static char[][] toGrid(String level) {
        String[] lines = level.replace("\r", "").split("\n");
        int h = 0;
        int w = 0;
        for (String line : lines) {
            if (!line.isEmpty()) {
                h++;
                w = Math.max(w, line.length());
            }
        }
        char[][] grid = new char[h][w];
        int y = 0;
        for (String line : lines) {
            if (line.isEmpty()) continue;
            for (int x = 0; x < w; x++) {
                grid[y][x] = x < line.length() ? line.charAt(x) : EMPTY;
            }
            y++;
        }
        return grid;
    }

    public static String toLevelString(char[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < grid.length; y++) {
            sb.append(grid[y]);
            if (y < grid.length - 1) sb.append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Command line: sanitize a level file and print the result + report
    // ------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java -cp src levelGenerators.LGAS.LevelSanitizer <level.txt> [output.txt]");
            return;
        }
        String level = new String(Files.readAllBytes(Paths.get(args[0])));
        char[][] grid = toGrid(level);
        Report report = sanitize(grid);
        String clean = toLevelString(grid);

        System.out.println(report);
        if (args.length >= 2) {
            Files.write(Paths.get(args[1]), clean.getBytes());
            System.out.println("Wrote " + args[1]);
        } else {
            System.out.println(clean);
        }
    }
}