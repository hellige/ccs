package net.immute.ccs.impl.parser;

import net.immute.ccs.ImportResolver;
import net.immute.ccs.Origin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public interface AstRule {
    void addTo(BuildContext buildContext, BuildContext baseContext);
    boolean resolveImports(ImportResolver importResolver, Loader loader, Stack<String> inProgress);

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
        public boolean resolveImports(ImportResolver importResolver, Loader loader, Stack<String> inProgress) {
            if (inProgress.contains(location)) {
                loader.getLogger().error(String.format("Circular import detected involving '" + location + "'"));
            } else {
                inProgress.push(location);
                try {
                    ast = loader.parseCcsStream(importResolver.resolve(location), location, importResolver, inProgress);
                    if (ast != null) return true;
                } catch (IOException e) {
                    loader.getLogger().error(String.format("Error loading imported document '%s': %s",
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
        private final Boolean local;

        public PropDef(String name, Value<?> value, Origin origin, Boolean local) {
            this.name = name;
            this.value = value;
            this.origin = origin;
            this.local = local;
        }

        @Override
        public void addTo(BuildContext buildContext, BuildContext _) {
            buildContext.addProperty(name, value, origin, local);
        }

        @Override
        public boolean resolveImports(ImportResolver _, Loader __, Stack<String> ___) {
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
        public boolean resolveImports(ImportResolver importResolver, Loader loader, Stack<String> inProgress) {
            for (AstRule rule : rules) if (!rule.resolveImports(importResolver, loader, inProgress)) return false;
            return true;
        }
    }
}
