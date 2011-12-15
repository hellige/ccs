package net.immute.ccs.parser;

import net.immute.ccs.CcsLogger;
import net.immute.ccs.Origin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface AstRule {
    void addTo(BuildContext buildContext, BuildContext baseContext);
    boolean resolveImports(ImportResolver importResolver, Loader loader);

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
        public boolean resolveImports(ImportResolver importResolver, Loader loader) {
            try {
                // TODO check for circular imports...
                ast = loader.parseCcsStream(importResolver.resolve(location), location, importResolver);
                if (ast != null) return true;
            } catch (IOException e) {
                CcsLogger.error(String.format("Error loading imported document '%s': %s", location, e.toString()));
                // TODO maybe a stack trace or something too?
            }
            return false;
        }
    }

    public static class PropDef implements AstRule {
        private final String name;
        private final Value<?> value;
        private final Origin origin;

        public PropDef(String name, Value<?> value, Origin origin) {
            this.name = name;
            this.value = value;
            this.origin = origin;
        }

        @Override
        public void addTo(BuildContext buildContext, BuildContext _) {
            buildContext.addProperty(name, value, origin, true);
        }

        @Override
        public boolean resolveImports(ImportResolver _, Loader __) {
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
        public boolean resolveImports(ImportResolver importResolver, Loader loader) {
            for (AstRule rule : rules) if (!rule.resolveImports(importResolver, loader)) return false;
            return true;
        }
    }
}
