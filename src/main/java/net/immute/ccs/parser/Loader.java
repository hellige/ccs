package net.immute.ccs.parser;

import net.immute.ccs.CcsLogger;
import net.immute.ccs.dag.Dag;
import net.immute.ccs.dag.Node;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.support.ParsingResult;

import java.io.*;
import java.net.URL;

public class Loader {
    CcsParser parser = new CcsParser();

    public Node loadCcsFile(String fileName) throws IOException {
        URL uri = new URL("file", null, -1, fileName);
        return loadCcsStream(uri.openStream(), fileName);
    }

    public Node loadCcsStream(InputStream stream, String fileName) throws IOException {
        return loadCcsStream(stream, fileName, new ImportResolver.Null());
    }

    public Node loadCcsStream(InputStream stream, String fileName, ImportResolver importResolver)
            throws IOException {
        return loadCcsStream(stream, fileName, new Dag(), importResolver);
    }

    public Node loadCcsStream(InputStream stream, String fileName, Dag dag, ImportResolver importResolver)
            throws IOException {
        AstRule rule = parseCcsStream(stream, fileName, importResolver);
        if (rule == null) return dag.getRoot();

        // everything parsed, no errors. now it's safe to modify the dag...
        rule.addTo(dag.getBuildContext(), dag.getBuildContext());

        return dag.getRoot();
    }

    public Node loadEmpty() {
        return new Node();
    }

    AstRule parseCcsStream(InputStream stream, String fileName, ImportResolver importResolver)
            throws IOException {
        Reader reader = new InputStreamReader(stream);
        ParsingResult<AstRule> result = parser.parse(reader, fileName);
        reader.close();

        if (result.hasErrors()) {
            CcsLogger.error("Errors parsing " + fileName + ":" + ErrorUtils.printParseErrors(result));
            return null;
        }

        AstRule rule = result.resultValue;
        if (!rule.resolveImports(importResolver, this)) return null;

        return rule;
    }

    public static void main(String[] args) throws Exception {
//        String input = args.length > 0 ? args[0] : "\n@context (foo#bar);\n @import /* hi */ 'foo';\n@import 'bar';" +
//                ".class > #id[a = 'b'] (a + b + c) asdf { foo = 'test'; bar = 32; baz = 0.3; bum = 1e1 };";
        String input = args.length > 0 ? args[0] : "\n@context (foo#bar);\n @import /* hi */ 'foo';\n@import 'bar';" +
                ".class > #id[a = 'b'] asdf { foo = 'test'; bar = 32; baz = 0.3; bum = 1e1 };";

        Node result = new Loader().loadCcsStream(new ByteArrayInputStream(input.getBytes("UTF-8")), "<literal>");

        System.out.println(result);
    }
}
