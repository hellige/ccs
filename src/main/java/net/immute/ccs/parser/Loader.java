package net.immute.ccs.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;

import net.immute.ccs.tree.Node;

import org.w3c.css.sac.InputSource;
import org.w3c.flute.parser.Parser;

public class Loader {
    public Node loadCcsFile(String fileName) throws IOException {
        URL uri = new URL("file", null, -1, fileName);
        return loadCcsStream(uri.openStream(), fileName);
    }

    public Node loadCcsStream(InputStream stream, String fileName)
        throws IOException {
        Node root = new Node();
        LineNumberReader reader =
            new LineNumberReader(new InputStreamReader(stream));
        InputSource source = new InputSource();
        source.setCharacterStream(reader);
        Parser parser = new Parser();
        parser
            .setDocumentHandler(new CcsDocumentHandler(root, fileName, reader));
        parser.parseStyleSheet(source);
        reader.close();
        return root;
    }
}
