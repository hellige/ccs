package net.immute.ccs;

import net.immute.ccs.dag.DagBuilder;
import net.immute.ccs.parser.ImportResolver;
import net.immute.ccs.parser.Loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class CcsDomain {
    private final DagBuilder dag = new DagBuilder();

    private final Loader loader;
    private final CcsLogger log;

    public CcsDomain(CcsLogger log) {
        this.log = log;
        loader = new Loader(log);
    }

    public CcsDomain() {
        this(new CcsLogger.StderrCcsLogger());
    }

    public SearchContext build() {
        return new SearchContext(dag.getRoot(), log);
    }

    public CcsDomain loadCcsFile(String fileName) throws IOException {
        URL uri = new URL("file", null, -1, fileName);
        loadCcsFile(fileName, new ImportResolver.Null());
        return this;
    }

    public CcsDomain loadCcsFile(String fileName, ImportResolver importResolver) throws IOException {
        URL uri = new URL("file", null, -1, fileName);
        loadCcsStream(uri.openStream(), fileName, importResolver);
        return this;
    }

    public CcsDomain loadCcsStream(InputStream stream, String fileName) throws IOException {
        loadCcsStream(stream, fileName, new ImportResolver.Null());
        return this;
    }

    public CcsDomain loadCcsStream(InputStream stream, String fileName, ImportResolver importResolver)
            throws IOException {
        loader.loadCcsStream(stream, fileName, dag, importResolver);
        return this;
    }
}
