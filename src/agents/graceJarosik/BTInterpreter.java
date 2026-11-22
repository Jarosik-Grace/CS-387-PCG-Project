package agents.graceJarosik;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class BTInterpreter {

    private static final Map<String, Supplier<Task>> LEAVES = new LinkedHashMap<>();
    private static final Map<String, Function<List<Task>, Task>> COMPOSITES = new LinkedHashMap<>();

    private static final Set<String> WRAPPERS = new HashSet<>(Arrays.asList("behaviortree", "root", "tree", "bt"));
    private static final Set<String> GENERIC_LEAF_TAGS = new HashSet<>(Arrays.asList("action", "condition", "task", "leaf", "node"));
    private static final String[] NAME_ATTRIBUTES = {"ID", "id", "class", "type", "name"};

    static {
        COMPOSITES.put("selector", Selector::new);
        COMPOSITES.put("fallback", Selector::new);
        COMPOSITES.put("sequence", Sequence::new);
        register("isAirborne", isAirborne::new);
        register("isEnemyNearby", isEnemyNearby::new);
        register("isEnemyReachable", isEnemyReachable::new);
        register("isStompableEnemy", isStompableEnemy::new);
        register("isGapAhead", isGapAhead::new);
        register("isObstacleAhead", isObstacleAhead::new);
        register("isCoinNearby", isCoinNearby::new);
        register("isImmediateThreat", isImmediateThreat::new);
        register("AvoidEnemy", AvoidEnemy::new);
        register("CollectCoin", CollectCoin::new);
        register("AttackEnemy", AttackEnemy::new);
        register("AStar", AStar::new);
    }

    private static void register(String name, Supplier<Task> factory) {
        LEAVES.put(name.toLowerCase(Locale.ROOT), factory);
    }

    public static Task load(String path) {
        return load(path, true);
    }

    public static Task load(String path, boolean printTree) {
        File file = new File(path);
        if (!file.isFile()) {
            throw new IllegalArgumentException("Behavior tree file not found: " + file.getAbsolutePath());
        }
        try {
            Document doc = newBuilder().parse(file);
            return build(doc, printTree, file.getName());
        } catch (BTException e) {
            throw e;
        } catch (Exception e) {
            throw new BTException("Could not parse " + path + ": " + e.getMessage(), e);
        }
    }

    private static DocumentBuilder newBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }

    private static Task build(Document doc, boolean printTree, String source) {
        Element top = doc.getDocumentElement();
        Element rootNode = findRootNode(top);

        StringBuilder dump = new StringBuilder();

        Task tree = buildNode(rootNode, 0, dump);

        if (printTree) {
            System.out.println("=== BEHAVIOR TREE ===");
            System.out.println(dump);
        }

        return tree;
    }

    private static Element findRootNode(Element element) {
        Element current = element;
        while (WRAPPERS.contains(tag(current))) {
            List<Element> kids = childElements(current);
            if (kids.isEmpty()) {
                throw new BTException("<" + current.getTagName() + "> is empty: no tree defined");
            }
            Element next = kids.get(0);

            String wanted = current.getAttribute("main_tree_to_execute");
            if (!wanted.isEmpty()) {
                for (Element k : kids) {
                    if (wanted.equals(k.getAttribute("ID"))) {
                        next = k;
                        break;
                    }
                }
            }
            current = next;
        }
        return current;
    }

    private static Task buildNode(Element e, int depth, StringBuilder dump) {
        String tag = tag(e);
        List<Element> kids = childElements(e);
        String indent = "  ".repeat(depth);

        Function<List<Task>, Task> composite = COMPOSITES.get(tag);
        if (composite != null) {
            if (kids.isEmpty()) {
                throw new BTException("<" + e.getTagName() + "> has no children"
                        + label(e) + ": a composite needs at least one child");
            }
            dump.append(indent).append(e.getTagName()).append(label(e)).append('\n');
            List<Task> children = new ArrayList<>();
            for (Element kid : kids) {
                children.add(buildNode(kid, depth + 1, dump));
            }
            return composite.apply(children);
        }

        if (!kids.isEmpty()) {
            throw new BTException("<" + e.getTagName() + "> is a leaf but has children. "
                    + "Known composites: " + COMPOSITES.keySet());
        }

        String leafName = GENERIC_LEAF_TAGS.contains(tag) ? leafNameFromAttributes(e) : tag;
        Supplier<Task> factory = LEAVES.get(leafName.toLowerCase(Locale.ROOT));
        if (factory == null) {
            throw new BTException("Unknown behavior tree node '" + leafName + "' (from <"
                    + e.getTagName() + ">). Known leaves: " + LEAVES.keySet()
                    + ". Known composites: " + COMPOSITES.keySet()
                    + ". Register new leaves in BTInterpreter's static block.");
        }
        dump.append(indent).append(leafName).append('\n');
        return factory.get();
    }

    private static String leafNameFromAttributes(Element e) {
        for (String attr : NAME_ATTRIBUTES) {
            String v = e.getAttribute(attr);
            if (!v.isEmpty()) return v;
        }
        throw new BTException("<" + e.getTagName() + "> needs one of the attributes "
                + Arrays.toString(NAME_ATTRIBUTES) + " naming the Task to run");
    }

    private static String tag(Element e) {
        return e.getTagName().toLowerCase(Locale.ROOT);
    }

    private static String label(Element e) {
        String n = e.getAttribute("name");
        return n.isEmpty() ? "" : " (" + n + ")";
    }

    private static List<Element> childElements(Element e) {
        List<Element> out = new ArrayList<>();
        NodeList nodes = e.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) out.add((Element) n);
        }
        return out;
    }

    public static class BTException extends RuntimeException {
        public BTException(String message) {
            super(message);
        }

        public BTException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}