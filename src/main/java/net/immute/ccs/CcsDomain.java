package net.immute.ccs;

import net.immute.ccs.impl.dag.DagBuilder;
import net.immute.ccs.impl.parser.Parser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class CcsDomain {
    private final DagBuilder dag = new DagBuilder();

    private final Parser parser;
    private final CcsLogger log;
    private final boolean logAccesses;

    public CcsDomain(CcsLogger log, boolean logAccesses) {
        this.log = log;
        this.logAccesses = logAccesses;
        parser = new Parser(log);
    }

    public CcsDomain(boolean logAccesses) {
        this(new CcsLogger.StderrCcsLogger(), logAccesses);
    }

    public CcsDomain(CcsLogger log) {
        this(log, false);
    }

    public CcsDomain() {
        this(new CcsLogger.StderrCcsLogger());
    }

    public CcsContext build() {
        return new CcsContext(dag.getRoot(), log, logAccesses);
    }

    public CcsDomain loadCcsFile(String fileName) throws IOException {
        loadCcsFile(fileName, new ImportResolver.Null());
        return this;
    }

    public CcsDomain loadCcsFile(String fileName, ImportResolver importResolver) throws IOException {
        URL uri = new URL("file", null, -1, fileName);
        loadCcsStream(new InputStreamReader(uri.openStream()), fileName, importResolver);
        return this;
    }

    public CcsDomain loadCcsStream(Reader stream, String fileName) throws IOException {
        loadCcsStream(stream, fileName, new ImportResolver.Null());
        return this;
    }

    public CcsDomain loadCcsStream(Reader stream, String fileName, ImportResolver importResolver)
            throws IOException {
        parser.loadCcsStream(stream, fileName, dag, importResolver);
        return this;
    }
}
