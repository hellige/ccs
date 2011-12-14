package net.immute.ccs.parser;

import net.immute.ccs.CcsLogger;
import net.immute.ccs.dag.Node;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.support.ParsingResult;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Loader {
    CcsParser parser = new CcsParser();

    public Node loadCcsFile(String fileName) throws IOException {
        URL uri = new URL("file", null, -1, fileName);
        return loadCcsStream(uri.openStream(), fileName);
    }

    public Node loadCcsStream(InputStream stream, String fileName) throws IOException {
        return loadCcsStream(stream, fileName, null);
    }

    public Node loadCcsStream(InputStream stream, String fileName, ImportResolver importResolver)
            throws IOException {
        Node root = new Node();
        return loadCcsStream(stream, fileName, root, importResolver);
    }

    public Node loadCcsStream(InputStream stream, String fileName, Node root, ImportResolver importResolver)
            throws IOException {
        List<AstRule> rules = new ArrayList<AstRule>();
        if (!parseCcsStream(rules, stream, fileName, importResolver)) return root;

        // everything parsed, no errors. now it's safe to modify the dag...
        for (AstRule rule : rules) rule.addTo(root);

        return root;
    }

    public Node loadEmpty() {
        return new Node();
    }

    boolean parseCcsStream(List<AstRule> rules, InputStream stream, String fileName, ImportResolver importResolver)
            throws IOException {
        Reader reader = new InputStreamReader(stream);
        ParsingResult<AstRule> result = parser.parse(reader, fileName);
        reader.close();

        if (result.hasErrors()) {
            CcsLogger.error("Errors parsing " + fileName + ":" + ErrorUtils.printParseErrors(result));
            return false;
        }

        AstRule rule = result.resultValue;

        // resolve imports first, so they have lower property numbers...
        if (!rule.resolveImports(rules, importResolver, this)) return false;

        // then add the importing rule...
        rules.add(rule);

        return true;
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
