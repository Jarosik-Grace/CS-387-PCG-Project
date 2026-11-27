package levelGenerators.LGAS;
import java.io.IOException;
import java.nio.file.Paths;
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
    private final MarkovModel markovModel;
    private final MarkovModel bigramModel;
    private final int n;
    private final long seed;
    private final boolean fixedSeed;
    private int levelsMade = 0;
    private final Map<Integer, List<String>> bySurface = new HashMap<>();
    private UnaryOperator<String> postProcessor = UnaryOperator.identity();
    private int lastBackoffs = 0;
    private int lastFlatFallbacks = 0;

    public LevelGenerator() {
        this(3, System.nanoTime(), false);
    }

    public LevelGenerator(long seed) {
        this(3, seed, true);
    }

    public LevelGenerator(MarkovModel markovModel, MarkovModel bigramModel, int n, long seed) {
        this(markovModel, bigramModel, n, seed, true);
    }

    private LevelGenerator(int n, long seed, boolean fixedSeed) {
        this(createAndTrainModel(n), createAndTrainModel(1), n, seed, fixedSeed);
    }

    private LevelGenerator(MarkovModel markovModel, MarkovModel bigramModel, int n, long seed, boolean fixedSeed) {
        this.markovModel = markovModel;
        this.bigramModel = bigramModel;
        this.n = n;
        this.seed = seed;
        this.fixedSeed = fixedSeed;
        buildTable();
    }

    private static MarkovModel createAndTrainModel(int order) {
        MarkovModel model = new MarkovModel(order);
        try {
            model.trainFromDirectory(Paths.get(DEFAULT_CORPUS));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to train MarkovModel of order " + order, e);
        }
        return model;
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

        List<String> options = new ArrayList<>(markovModel.getCandidates(cols));
        if (options == null || options.isEmpty()) {
            lastBackoffs++;
            options = new ArrayList<>(bigramModel.getCandidates(cols));
        }
        if (options == null || options.isEmpty()) {
            List<String> surfaceOptions = bySurface.get(surface(last));
            if (surfaceOptions != null) {
                options = bySurface.get(surface(last));
            }
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
        return MarkovModel.cleanColumn(options.get(rnd.nextInt(options.size())));
    }

    static int surface(String col) {
        int y = LEVEL_HEIGHT - 1;
        if (SOLID.indexOf(col.charAt(y)) < 0) return LEVEL_HEIGHT;
        while (y > 0 && SOLID.indexOf(col.charAt(y - 1)) >= 0) y--;
        return y;
    }

    public void buildTable() {
        for (String follower : markovModel.getAllObservedColumns()) {
            String cleaned = MarkovModel.cleanColumn(follower);
            bySurface.computeIfAbsent(surface(cleaned), z -> new ArrayList<>()).add(cleaned);
        }
    }
}