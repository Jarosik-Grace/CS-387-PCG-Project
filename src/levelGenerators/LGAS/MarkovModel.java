package levelGenerators.LGAS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MarkovModel {
    public static final int LEVEL_HEIGHT = 16;
    public static final char EMPTY_TILE = '-';
    private static final String CONTEXT_DELIMITER = "\u0001";

    private final int order;
    private final Map<String, List<String>> transitions = new HashMap<>();

    public MarkovModel(int order) {
        if (order < 1) {
            throw new IllegalArgumentException("Markov order must be at least 1");
        }
        this.order = order;
    }

    public static String cleanColumn(String col) {
        return col.replace('M', EMPTY_TILE).replace('F', EMPTY_TILE);
    }

    public void trainFromDirectory(Path directory) throws IOException {
        transitions.clear();

        if (!Files.isDirectory(directory)) {
            throw new IOException("Level directory not found: " + directory.toAbsolutePath());
        }

        List<Path> levelFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.txt")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    levelFiles.add(entry);
                }
            }
        }

        if (levelFiles.isEmpty()) {
            throw new IOException("No .txt level files found in: " + directory.toAbsolutePath());
        }

        for (Path file : levelFiles) {
            String[] columns = parseLevelColumns(file);
            if (columns.length > order) {
                recordTransitions(columns);
            }
        }
    }

    private String[] parseLevelColumns(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> rows = new ArrayList<>(LEVEL_HEIGHT);

        for (int y = 0; y < LEVEL_HEIGHT; y++) {
            rows.add(y < lines.size() ? lines.get(y).replace("\r", "") : "");
        }

        int width = 0;
        for (String row : rows) {
            if (row.length() > width) {
                width = row.length();
            }
        }

        String[] columns = new String[width];
        for (int x = 0; x < width; x++) {
            StringBuilder colBuilder = new StringBuilder(LEVEL_HEIGHT);
            for (int y = 0; y < LEVEL_HEIGHT; y++) {
                String row = rows.get(y);
                colBuilder.append(x < row.length() ? row.charAt(x) : EMPTY_TILE);
            }
            columns[x] = cleanColumn(colBuilder.toString());
        }
        return columns;
    }

    private void recordTransitions(String[] columns) {
        for (int i = 0; i <= columns.length - order - 1; i++) {
            String contextKey = buildContextKey(columns, i, order);
            String nextColumn = columns[i + order];
            transitions.computeIfAbsent(contextKey, k -> new ArrayList<>()).add(nextColumn);
        }
    }

    public List<String> getCandidates(List<String> history) {
        if (history == null || history.size() < order) {
            return List.of();
        }
        String key = buildContextKey(history, history.size() - order, order);
        List<String> candidates = transitions.get(key);
        return (candidates == null) ? List.of() : candidates;
    }

    public List<String> getAllObservedColumns() {
        List<String> all = new ArrayList<>();
        for (List<String> followers : transitions.values()) {
            all.addAll(followers);
        }
        return all;
    }

    private String buildContextKey(List<String> columns, int startIndex, int length) {
        StringBuilder sb = new StringBuilder(length * (LEVEL_HEIGHT + 1));
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(CONTEXT_DELIMITER);
            }
            sb.append(cleanColumn(columns.get(startIndex + i)));
        }
        return sb.toString();
    }

    private String buildContextKey(String[] columns, int startIndex, int length) {
        StringBuilder sb = new StringBuilder(length * (LEVEL_HEIGHT + 1));
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(CONTEXT_DELIMITER);
            }
            sb.append(columns[startIndex + i]);
        }
        return sb.toString();
    }
}