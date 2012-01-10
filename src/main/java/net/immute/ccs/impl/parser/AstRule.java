package net.immute.ccs.impl.parser;

import net.immute.ccs.ImportResolver;
import net.immute.ccs.Origin;
import net.immute.ccs.impl.dag.Key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public interface AstRule {
    void addTo(BuildContext buildContext, BuildContext baseContext);
    boolean resolveImports(ImportResolver importResolver, Parser parser, Stack<String> inProgress);

    public static class Import implements AstRule {
        private final String location;

        private AstRule ast;

        public Import(String location) {
            this.location = location;
        }

        @Override
        public void addTo(BuildContext buildContext, BuildContext baseContext) {
            assert ast != null;
            ast.addTo(buildContext, baseContext);
        }

        @Override
        public boolean resolveImports(ImportResolver importResolver, Parser parser, Stack<String> inProgress) {
            if (inProgress.contains(location)) {
                parser.getLogger().error(String.format("Circular import detected involving '" + location + "'"));
            } else {
                inProgress.push(location);
                try {
                    ast = parser.parseCcsStream(importResolver.resolve(location), location, importResolver, inProgress);
                    if (ast != null) return true;
                } catch (IOException e) {
                    parser.getLogger().error(String.format("Error loading imported document '%s': %s",
                            location, e.toString()), e);
                } finally {
                    inProgress.pop();
                }
            }
            return false;
        }
    }

    public static class PropDef implements AstRule {
        private final String name;
        private final Value<?> value;
        private final Origin origin;
        private final boolean local;
        private final boolean override;

        public PropDef(String name, Value<?> value, Origin origin, boolean local, boolean override) {
            this.name = name;
            this.value = value;
            this.origin = origin;
            this.local = local;
            this.override = override;
        }

        @Override
        public void addTo(BuildContext buildContext, BuildContext _) {
            buildContext.addProperty(name, value, origin, local, override);
        }

        @Override
        public boolean resolveImports(ImportResolver _, Parser __, Stack<String> ___) {
            return true;
        }
    }

    public class Constraint implements AstRule {
        private final Key key;

        public Constraint(Key key) {
            this.key = key;
        }

        @Override
        public void addTo(BuildContext buildContext, BuildContext baseContext) {
            buildContext.addConstraint(key);
        }

        @Override
        public boolean resolveImports(ImportResolver importResolver, Parser parser, Stack<String> inProgress) {
            return true;
        }
    }

    public static class Nested implements AstRule {
        private final List<AstRule> rules = new ArrayList<AstRule>();
        private SelectorBranch selector;

        public Nested(SelectorBranch selector) {
            this.selector = selector;
        }

        // these return boolean for ease of use with parboiled...
        boolean setSelector(SelectorBranch selector) {
            this.selector = selector;
            return true;
        }

        boolean append(AstRule rule) {
            rules.add(rule);
            return true;
        }

        @Override
        public void addTo(BuildContext buildContext, BuildContext baseContext) {
            if (selector != null) buildContext = selector.traverse(buildContext, baseContext);
            for (AstRule rule : rules) rule.addTo(buildContext, baseContext);
        }

        @Override
        public boolean resolveImports(ImportResolver importResolver, Parser parser, Stack<String> inProgress) {
            for (AstRule rule : rules) if (!rule.resolveImports(importResolver, parser, inProgress)) return false;
            return true;
        }
    }
}
