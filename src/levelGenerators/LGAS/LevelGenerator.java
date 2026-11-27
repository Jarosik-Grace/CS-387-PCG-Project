package levelGenerators.LGAS;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.UnaryOperator;
import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;

public class LevelGenerator implements MarioLevelGenerator {
    public static final int LEVEL_HEIGHT = 16;
    public static final int START_PAD = 5;
    public static final int END_PAD = 5;
    public static final int STEER_WINDOW = 8;
    public static final String DEFAULT_CORPUS = "./levels/original/";
    public static final String FLAT = "--------------XX";
    private static final int FLAT_SURFACE = LEVEL_HEIGHT - 2;
    private static final String SOLID = "X#tT[]<>D";
    private final Map<String, List<String>> nGrams;
    private final int n;
    private final long seed;
    private final boolean fixedSeed;
    private int levelsMade = 0;
    private final Map<String, List<String>> bigrams = new HashMap<>();
    private final Map<Integer, List<String>> bySurface = new HashMap<>();
    private UnaryOperator<String> postProcessor = UnaryOperator.identity();
    private int lastBackoffs = 0;
    private int lastFlatFallbacks = 0;

    public LevelGenerator() {
        this(buildTable(DEFAULT_CORPUS, 3), 3, System.nanoTime(), false);
    }

    public LevelGenerator(long seed) {
        this(buildTable(DEFAULT_CORPUS, 3), 3, seed, true);
    }

    public LevelGenerator(Map<String, List<String>> nGrams, int n, long seed) {
        this(nGrams, n, seed, true);
    }

    private LevelGenerator(Map<String, List<String>> nGrams, int n, long seed, boolean fixedSeed) {
        this.nGrams = nGrams;
        this.n = n;
        this.seed = seed;
        this.fixedSeed = fixedSeed;
        buildBackoffTables();
    }

    public void setPostProcessor(UnaryOperator<String> postProcessor) {
        this.postProcessor = postProcessor == null ? UnaryOperator.identity() : postProcessor;
    }

    public int getLastBackoffs() { return lastBackoffs; }
    public int getLastFlatFallbacks() { return lastFlatFallbacks; }

    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer) {
        int width = model.getWidth();
        if (width < START_PAD + END_PAD + n + 1) {
            throw new IllegalArgumentException("Level width " + width + " is too small");
        }

        Random rnd = new Random(fixedSeed ? seed + levelsMade : seed ^ System.nanoTime());
        levelsMade++;
        lastBackoffs = 0;
        lastFlatFallbacks = 0;

        List<String> cols = new ArrayList<>(width);

        for (int i = 0; i < START_PAD; i++) cols.add(FLAT);

        int middleEnd = width - END_PAD;
        while (cols.size() < middleEnd) {
            boolean landing = cols.size() >= middleEnd - STEER_WINDOW;
            cols.add(nextColumn(cols, rnd, landing));
        }

        for (int i = 0; i < END_PAD; i++) cols.add(FLAT);

        model.clearMap();
        for (int x = 0; x < width; x++) {
            String c = cols.get(x);
            for (int y = 0; y < LEVEL_HEIGHT; y++) model.setBlock(x, y, c.charAt(y));
        }
        model.setBlock(1, FLAT_SURFACE - 1, MarioLevelModel.MARIO_START);
        int flagX = width - 1 - END_PAD / 2;
        model.setBlock(flagX, FLAT_SURFACE - 1, MarioLevelModel.PYRAMID_BLOCK);
        model.setBlock(flagX, FLAT_SURFACE - 2, MarioLevelModel.MARIO_EXIT);

        return postProcessor.apply(model.getMap());
    }

    @Override
    public String getGeneratorName() {
        return "LGASLevelGenerator";
    }

    private String nextColumn(List<String> cols, Random rnd, boolean landing) {
        String last = cols.get(cols.size() - 1);

        StringBuilder key = new StringBuilder();
        for (int i = cols.size() - n; i < cols.size(); i++) key.append(cols.get(i));

        List<String> options = nGrams.get(key.toString());
        if (options == null || options.isEmpty()) {
            lastBackoffs++;
            options = bigrams.get(last);
        }
        if (options == null || options.isEmpty()) {
            options = bySurface.get(surface(last));
        }
        if (options == null || options.isEmpty()) {
            lastFlatFallbacks++;
            return FLAT;
        }

        if (landing) {
            List<String> flatLanding = new ArrayList<>();
            for (String o : options) if (surface(o) == FLAT_SURFACE) flatLanding.add(o);
            if (!flatLanding.isEmpty()) options = flatLanding;
        }
        return clean(options.get(rnd.nextInt(options.size())));
    }

    private static String clean(String col) {
        return col.replace('M', '-').replace('F', '-');
    }

    static int surface(String col) {
        int y = LEVEL_HEIGHT - 1;
        if (SOLID.indexOf(col.charAt(y)) < 0) return LEVEL_HEIGHT;
        while (y > 0 && SOLID.indexOf(col.charAt(y - 1)) >= 0) y--;
        return y;
    }

    private void buildBackoffTables() {
        int colLen = LEVEL_HEIGHT;
        for (Map.Entry<String, List<String>> e : nGrams.entrySet()) {
            String k = e.getKey();
            if (k.length() < colLen) continue;
            String lastCol = k.substring(k.length() - colLen);
            bigrams.computeIfAbsent(lastCol, z -> new ArrayList<>()).addAll(e.getValue());
            for (String follower : e.getValue()) {
                bySurface.computeIfAbsent(surface(follower), z -> new ArrayList<>()).add(follower);
            }
        }
    }

    public static Map<String, List<String>> buildTable(String dir, int n) {
        Map<String, List<String>> table = new HashMap<>();
        File[] files = new File(dir).listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) throw new IllegalArgumentException("No levels found in " + dir);
        for (File f : files) {
            List<String> cols = readColumns(f);
            for (int i = 0; i + n < cols.size(); i++) {
                StringBuilder key = new StringBuilder();
                for (int j = i; j < i + n; j++) key.append(cols.get(j));
                table.computeIfAbsent(key.toString(), z -> new ArrayList<>()).add(cols.get(i + n));
            }
        }
        return table;
    }

    private static List<String> readColumns(File f) {
        List<String> cols = new ArrayList<>();
        try {
            List<String> rows = Files.readAllLines(f.toPath());
            rows.removeIf(String::isEmpty);
            if (rows.size() != LEVEL_HEIGHT) return cols;
            int w = rows.get(0).length();
            for (int x = 0; x < w; x++) {
                StringBuilder c = new StringBuilder();
                for (String row : rows) c.append(x < row.length() ? row.charAt(x) : '-');
                cols.add(clean(c.toString()));
            }
        } catch (IOException e) {
            System.err.println("Could not read " + f + ": " + e.getMessage());
        }
        return cols;
    }
}