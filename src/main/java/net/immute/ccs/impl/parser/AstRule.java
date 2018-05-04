package net.immute.ccs.impl.parser;

import net.immute.ccs.ImportResolver;
import net.immute.ccs.Origin;
import net.immute.ccs.impl.dag.Key;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

interface AstRule {
    void addTo(BuildContext buildContext, BuildContext baseContext);
    boolean resolveImports(ImportResolver importResolver, Parser parser, Stack<String> inProgress);

    class Import implements AstRule {
        private final String location;

        private AstRule ast;

        Import(String location) {
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
                parser.getLogger().error("Circular import detected involving '" + location + "'");
            } else {
                inProgress.push(location);
                try {
                    ast = parser.parseCcsStream(new InputStreamReader(importResolver.resolve(location)), location,
                                                importResolver, inProgress);
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

    class PropDef implements AstRule {
        private final String name;
        private final Value<?> value;
        private final Origin origin;
        private final boolean local; // TODO remove 'local' support
        private final boolean override;

        PropDef(String name, Value<?> value, Origin origin, boolean local, boolean override) {
            this.name = name;
            this.value = value;
            this.origin = origin;
            this.local = local;
            this.override = override;
        }

        @Override
        public void addTo(BuildContext buildContext, BuildContext baseContext) {
            buildContext.addProperty(name, value, origin, local, override);
        }

        @Override
        public boolean resolveImports(ImportResolver importResolver, Parser parser, Stack<String> inProgress) {
            return true;
        }
    }

    class Constraint implements AstRule {
        private final Key key;

        Constraint(Key key) {
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

    class Nested implements AstRule {
        private final List<AstRule> rules = new ArrayList<>();
        private SelectorBranch selector;

        Nested() {}

        Nested(SelectorBranch selector) {
            this.selector = selector;
        }

        void setSelector(SelectorBranch selector) {
            this.selector = selector;
        }

        void append(AstRule rule) {
            rules.add(rule);
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
