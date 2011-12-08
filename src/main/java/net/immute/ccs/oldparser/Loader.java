package net.immute.ccs.oldparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;

import net.immute.ccs.oldparser.CcsDocumentHandler;
import net.immute.ccs.parser.ImportResolver;
import net.immute.ccs.tree.Node;

import org.w3c.css.sac.InputSource;
import org.w3c.flute.parser.Parser;

public class Loader {
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
        LineNumberReader reader =
            new LineNumberReader(new InputStreamReader(stream));
        InputSource source = new InputSource();
        source.setCharacterStream(reader);
        Parser parser = new Parser();
        parser.setDocumentHandler(new CcsDocumentHandler(root, fileName, reader, importResolver));
        parser.parseStyleSheet(source);
        reader.close();
        return root;
    }

    public Node loadEmpty() {
        return new Node();
    }
}
