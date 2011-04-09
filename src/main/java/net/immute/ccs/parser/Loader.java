package net.immute.ccs.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;

import net.immute.ccs.tree.CCSNode;

import org.w3c.css.sac.InputSource;
import org.w3c.flute.parser.Parser;

public class Loader {
    public CCSNode loadCCSFile(String fileName) throws IOException {
        URL uri = new URL("file", null, -1, fileName);
        return loadCCSStream(uri.openStream(), fileName);
    }

    public CCSNode loadCCSStream(InputStream stream, String fileName)
        throws IOException {
        CCSNode root = new CCSNode();
        LineNumberReader reader =
            new LineNumberReader(new InputStreamReader(stream));
        InputSource source = new InputSource();
        source.setCharacterStream(reader);
        Parser parser = new Parser();
        parser
            .setDocumentHandler(new CCSDocumentHandler(root, fileName, reader));
        parser.parseStyleSheet(source);

        reader.close();

        return root;
    }
}
