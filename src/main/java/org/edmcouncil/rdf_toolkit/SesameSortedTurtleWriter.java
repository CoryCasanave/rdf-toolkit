/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Enterprise Data Management Council
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.edmcouncil.rdf_toolkit;

import info.aduna.io.IndentingWriter;
import org.openrdf.model.*;
import org.openrdf.rio.RDFHandlerException;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import javax.xml.namespace.QName;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

/**
 * Equivalent to Sesame's built-in Turtle writer, but the triples are sorted into a consistent order.
 * In order to do the sorting, it must be possible to load all of the RDF statements into memory.
 * NOTE: comments are suppressed, as there isn't a clear way to sort them along with triples.
 */
public class SesameSortedTurtleWriter extends SesameSortedRDFWriter {

    // private static final Logger logger = LoggerFactory.getLogger(SesameSortedTurtleWriter.class);

    /** Output stream for this Turtle writer. */
    private IndentingWriter output = null;

    /**
     * Creates an RDFWriter instance that will write sorted Turtle to the supplied output stream.
     *
     * @param out The OutputStream to write the Turtle to.
     */
    public SesameSortedTurtleWriter(OutputStream out) {
        super(out);
        this.output = new IndentingWriter(new OutputStreamWriter(out));
        this.out = this.output;
    }

    /**
     * Creates an RDFWriter instance that will write sorted Turtle to the supplied writer.
     *
     * @param writer The Writer to write the Turtle to.
     */
    public SesameSortedTurtleWriter(Writer writer) {
        super(writer);
        this.output = new IndentingWriter(writer);
        this.out = this.output;
    }

    /**
     * Creates an RDFWriter instance that will write sorted Turtle to the supplied output stream.
     *
     * @param out The OutputStream to write the Turtle to.
     * @param options options for the Turtle writer.
     */
    public SesameSortedTurtleWriter(OutputStream out, Map<String, Object> options) {
        super(out, options);
        this.output = new IndentingWriter(new OutputStreamWriter(out));
        this.out = this.output;
        if (options.containsKey("indent")) {
            this.output.setIndentationString((String) options.get("indent"));
        }
    }

    /**
     * Creates an RDFWriter instance that will write sorted Turtle to the supplied writer.
     *
     * @param writer The Writer to write the Turtle to.
     * @param options options for the Turtle writer.
     */
    public SesameSortedTurtleWriter(Writer writer, Map<String, Object> options) {
        super(writer, options);
        this.output = new IndentingWriter(writer);
        this.out = this.output;
        if (options.containsKey("indent")) {
            this.output.setIndentationString((String) options.get("indent"));
        }
    }

    /**
     * Signals the start of the RDF data. This method is called before any data
     * is reported.
     *
     * @throws org.openrdf.rio.RDFHandlerException If the RDF handler has encountered an unrecoverable error.
     */
    @Override
    public void startRDF() throws RDFHandlerException {
        super.startRDF();
        output.setIndentationLevel(0);
    }

    protected void writeHeader(Writer out, SortedTurtleObjectList importList) throws Exception {
        // Write TopBraid-specific special comments, if any.
        if ((baseUri != null) || (importList.size() >= 1)) {
            // Write the baseURI, if any.
            if (baseUri != null) {
                output.write("# baseURI: " + baseUri); output.writeEOL();
            }
            // Write ontology imports, if any.
            for (Value anImport : importList) {
                output.write("# imports: " + anImport.stringValue()); output.writeEOL();
            }
            output.writeEOL();
        }

        // Write the baseURI, if any.
        if (baseUri != null) {
            output.write("@base <" + baseUri + "> ."); output.writeEOL();
        }

        // Write out prefixes and namespaces URIs.
        if (namespaceTable.size() > 0) {
            TreeSet<String> prefixes = new TreeSet<String>(namespaceTable.keySet());
            for (String prefix : prefixes) {
                output.write("@prefix " + prefix + ": <" + namespaceTable.get(prefix) + "> ."); output.writeEOL();
            }
            output.writeEOL();
        }
    }

    protected void writeSubjectTriples(Writer out, Resource subject) throws Exception {
        SortedTurtlePredicateObjectMap poMap = sortedTripleMap.get(subject);
        if (subject instanceof BNode) {
            out.write("_:" + blankNodeNameMap.get(subject));
        } else {
            writeUri(out, (URI) subject);
        }
        if (out instanceof IndentingWriter) {
            IndentingWriter output = (IndentingWriter)out;
            output.writeEOL();
            output.increaseIndentation();
        } else {
            out.write("\n");
        }

        // Write predicate/object pairs rendered first.
        for (URI predicate : firstPredicates) {
            if (poMap.containsKey(predicate)) {
                SortedTurtleObjectList values = poMap.get(predicate);
                writePredicateAndObjectValues(out, predicate, values);
            }
        }

        // Write other predicate/object pairs.
        for (URI predicate : poMap.keySet()) {
            if (!firstPredicates.contains(predicate)) {
                SortedTurtleObjectList values = poMap.get(predicate);
                writePredicateAndObjectValues(out, predicate, values);
            }
        }

        // Close statement
        out.write(".");
        if (out instanceof IndentingWriter) {
            IndentingWriter output = (IndentingWriter)out;
            output.writeEOL();
            output.decreaseIndentation();
            output.writeEOL(); // blank line
        } else {
            out.write("\n\n");
        }
    }

    protected void writePredicateAndObjectValues(Writer out, URI predicate, SortedTurtleObjectList values) throws Exception {
        writePredicate(out, predicate);
        if (values.size() == 1) {
            out.write(" ");
            writeObject(out, values.first());
            out.write(" ;");
            if (out instanceof IndentingWriter) {
                IndentingWriter output = (IndentingWriter)out;
                output.writeEOL();
            } else {
                out.write("\n");
            }
        } else if (values.size() > 1) {
            if (out instanceof IndentingWriter) {
                IndentingWriter output = (IndentingWriter)out;
                output.writeEOL();
                output.increaseIndentation();
            } else {
                out.write("\n");
            }
            int numValues = values.size();
            int valueIndex = 0;
            for (Value value : values) {
                valueIndex += 1;
                writeObject(out, value);
                if (valueIndex < numValues) { out.write(" ,"); }
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.writeEOL();
                } else {
                    out.write("\n");
                }
            }
            out.write(";");
            if (out instanceof IndentingWriter) {
                IndentingWriter output = (IndentingWriter)out;
                output.writeEOL();
                output.decreaseIndentation();
            } else {
                out.write("\n");
            }
        }
    }

    protected void writePredicate(Writer out, URI predicate) throws Exception {
        writeUri(out, predicate);
    }

//    protected void writeQName(Writer out, QName qname) throws Exception {
//        out.write(convertQNameToString(qname, /*useTurtleQuoting*/true));
//    }

    protected void writeUri(Writer out, URI uri) throws Exception {
        out.write(convertUriToString(uri, /*useTurtleQuoting*/true));
    }

    protected void writeObject(Writer out, Value value) throws Exception {
        if (value instanceof BNode) {
            writeObject(out, (BNode) value);
        } else if (value instanceof URI) {
            writeObject(out, (URI)value);
        } else if (value instanceof Literal) {
            writeObject(out, (Literal)value);
        } else {
            out.write("\"" + value.stringValue() + "\"");
            out.write(" ");
        }
    }

    protected void writeObject(Writer out, BNode bnode) throws Exception {
        if (inlineBlankNodes) {
            SortedTurtlePredicateObjectMap poMap = sortedTripleMap.get(bnode);

            // Open brackets
            out.write("[");
            if (out instanceof IndentingWriter) {
                IndentingWriter output = (IndentingWriter)out;
                output.writeEOL();
                output.increaseIndentation();
            } else {
                out.write("\n");
            }

            // Write predicate/object pairs rendered first.
            for (URI predicate : firstPredicates) {
                if (poMap.containsKey(predicate)) {
                    SortedTurtleObjectList values = poMap.get(predicate);
                    writePredicateAndObjectValues(out, predicate, values);
                }
            }

            // Write other predicate/object pairs.
            for (URI predicate : poMap.keySet()) {
                if (!firstPredicates.contains(predicate)) {
                    SortedTurtleObjectList values = poMap.get(predicate);
                    writePredicateAndObjectValues(out, predicate, values);
                }
            }

            // Close brackets
            if (out instanceof IndentingWriter) {
                IndentingWriter output = (IndentingWriter)out;
                output.decreaseIndentation();
                out.write("]");
            } else {
                out.write("]");
            }
        } else { // no inlining of blank nodes
            if (unsortedTripleMap.containsKey(bnode)) {
                out.write("_:" + blankNodeNameMap.get(bnode));
            } else {
                out.write("[]"); // last resort - this should never happen
            }
        }
    }

    protected void writeObject(Writer out, URI uri) throws Exception {
        writeUri(out, uri);
    }

    protected void writeObject(Writer out, Literal literal) throws Exception {
        if (literal == null) {
            out.write("null<Literal>");
        } else if (literal.getLanguage() != null) {
            writeString(out, literal.stringValue());
            out.write("@" + literal.getLanguage());
        } else if (literal.getDatatype() != null) {
            writeString(out, literal.stringValue());
            out.write("^^");
            writeUri(out, literal.getDatatype());
        } else {
            writeString(out, literal.stringValue());
        }
    }

    protected void writeString(Writer out, String str) throws Exception {
        if (str == null) { return; }
        if (isMultilineString(str)) { // multi-line string
            if (str.contains("\"")) { // string contains double quote chars
                if (str.contains("'")) { // string contains both single and double quote chars
                    out.write("\"\"\"");
                    out.write(escapeString(str).replaceAll("\"", "\\\\\""));
                    out.write("\"\"\"");
                } else { // string contains double quote chars but no single quote chars
                    out.write("'''");
                    out.write(escapeString(str));
                    out.write("'''");
                }
            } else { // string has no double quote chars
                out.write("\"\"\"");
                out.write(escapeString(str));
                out.write("\"\"\"");
            }
        } else { // single-line string
            if (str.contains("\"")) { // string contains double quote chars
                if (str.contains("'")) { // string contains both single and double quote chars
                    out.write("\"");
                    out.write(escapeString(str).replaceAll("\"", "\\\\\""));
                    out.write("\"");
                } else { // string contains double quote chars but no single quote chars
                    out.write("'");
                    out.write(escapeString(str));
                    out.write("'");
                }
            } else { // string has no double quote chars
                out.write("\"");
                out.write(escapeString(str));
                out.write("\"");
            }
        }
    }

    protected void writeFooter(Writer out) {}

    private String escapeString(String str) {
        if (str == null) { return null; }
        return str.replaceAll("\\\\", "\\\\\\\\");
    }

}
