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

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Equivalent to Sesame's built-in JSON-LD writer, but the triples are sorted into a consistent order.
 * In order to do the sorting, it must be possible to load all of the RDF statements into memory.
 * NOTE: comments are suppressed, as there isn't a clear way to sort them along with triples.
 */
public class SesameSortedJsonLdWriter extends SesameSortedRDFWriter {

    // private static final Logger logger = LoggerFactory.getLogger(SesameSortedJsonLdWriter.class);

    private static final boolean useGeneratedPrefixes = false; // no need to use namespace prefixes generated by the serializer for Turtle.
    private static final Class collectionClass = Value.class; // Turtle allows "values" in RDF collections

    /** Output stream for this JSON-LD writer. */
    private IndentingWriter output = null;

    /**
     * Creates an RDFWriter instance that will write sorted JSON-LD to the supplied output stream.
     *
     * @param out The OutputStream to write the JSON-LD to.
     */
    public SesameSortedJsonLdWriter(OutputStream out) {
        super(out);
        this.output = new IndentingWriter(new OutputStreamWriter(out));
        this.out = this.output;
    }

    /**
     * Creates an RDFWriter instance that will write sorted JSON-LD to the supplied writer.
     *
     * @param writer The Writer to write the JSON-LD to.
     */
    public SesameSortedJsonLdWriter(Writer writer) {
        super(writer);
        this.output = new IndentingWriter(writer);
        this.out = this.output;
    }

    /**
     * Creates an RDFWriter instance that will write sorted JSON-LD to the supplied output stream.
     *
     * @param out The OutputStream to write the JSON-LD to.
     * @param options options for the JSON-LD writer.
     */
    public SesameSortedJsonLdWriter(OutputStream out, Map<String, Object> options) {
        super(out, options);
        this.output = new IndentingWriter(new OutputStreamWriter(out));
        this.out = this.output;
        if (options.containsKey("indent")) {
            this.output.setIndentationString((String) options.get("indent"));
        }
    }

    /**
     * Creates an RDFWriter instance that will write sorted JSON-LD to the supplied writer.
     *
     * @param writer The Writer to write the JSON-LD to.
     * @param options options for the JSON-LD writer.
     */
    public SesameSortedJsonLdWriter(Writer writer, Map<String, Object> options) {
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

    /**
     * Signals the end of the RDF data. This method is called when all data has
     * been reported.
     *
     * @throws org.openrdf.rio.RDFHandlerException If the RDF handler has encountered an unrecoverable error.
     */
    @Override
    public void endRDF() throws RDFHandlerException {
        try {
            // Sort triples, etc.
            sortedOntologies = unsortedOntologies.toSorted(collectionClass);
            if (sortedOntologies.size() != unsortedOntologies.size()) {
                System.err.println("**** ontologies unexpectedly lost or gained during sorting: " + sortedOntologies.size() + " != " + unsortedOntologies.size());
                System.err.flush();
            }

            sortedTripleMap = unsortedTripleMap.toSorted(collectionClass);
            if (sortedTripleMap.fullSize() != unsortedTripleMap.fullSize()) {
                System.err.println("**** triples unexpectedly lost or gained during sorting: " + sortedTripleMap.fullSize() + " != " + unsortedTripleMap.fullSize());
                System.err.flush();
            }

            sortedBlankNodes = unsortedBlankNodes.toSorted(collectionClass);
            if (sortedBlankNodes.size() != unsortedBlankNodes.size()) {
                System.err.println("**** blank nodes unexpectedly lost or gained during sorting: " + sortedBlankNodes.size() + " != " + unsortedBlankNodes.size());
                System.err.flush();
            }

            super.endRDF();
        } catch (Throwable t) {
            throw new RDFHandlerException("unable to generate/write RDF output", t);
        }
    }

    protected void writeHeader(Writer out, SortedTurtleObjectList importList, String[] leadingComments) throws Exception {
        // Process leading comments, if any.
        if ((leadingComments != null) && (leadingComments.length >= 1)) {
            System.err.println("#### leading comments ignored - JSON-LD does not support comments");
            System.err.flush();
        }

//        // Open context
//        output.write("\"@context\" : {");
//        output.writeEOL();
//        output.increaseIndentation();
//        ArrayList<String> contextLines = new ArrayList<String>(namespaceTable.size()+1);
//
//        // Write the base IRI, if any.
//        if (baseIri != null) {
//            contextLines.add("\"@base\" : \"" + baseIri + "\"");
//        }
//
//        // Write out prefixes and namespaces IRIs.
//        if (namespaceTable.size() > 0) {
//            TreeSet<String> prefixes = new TreeSet<String>(namespaceTable.keySet());
//            for (String prefix : prefixes) {
//                if (useGeneratedPrefixes || !generatedNamespaceTable.containsKey(prefix)) {
//                    contextLines.add("\"" + prefix + "\" : \"" + namespaceTable.get(prefix) + "\"");
//                }
//            }
//        }
//
//        // Write out context lines.
//        int contextIdx = 0;
//        for (String line : contextLines) {
//            contextIdx++;
//            output.write(line);
//            if (contextIdx < contextLines.size()) {
//                output.write(",");
//            }
//            output.writeEOL();
//        }

        // Open list of subject triples
        output.write("[");
        output.writeEOL();
        output.increaseIndentation();
    }

    protected void writeSubjectSeparator(Writer out) throws Exception {
        out.write(",");
        if (out instanceof IndentingWriter) {
            IndentingWriter output = (IndentingWriter)out;
            output.writeEOL();
        } else {
            out.write("\n");
        }
    }

    protected void writeSubjectTriples(Writer out, Resource subject) throws Exception {
        SortedTurtlePredicateObjectMap poMap = sortedTripleMap.get(subject);
        if (poMap == null) { poMap = new SortedTurtlePredicateObjectMap(); }
        out.write("{");
        if (out instanceof IndentingWriter) {
            IndentingWriter output = (IndentingWriter)out;
            output.writeEOL();
            output.increaseIndentation();
        } else {
            out.write("\n");
        }
        out.write("\"@id\" : \"");
        if (subject instanceof BNode) {
            out.write("_:" + blankNodeNameMap.get(subject));
        } else {
            writeIri(out, (IRI) subject);
        }
        out.write("\"");
        if (poMap.size() > 0) {
            out.write(",");
        }
        if (out instanceof IndentingWriter) {
            IndentingWriter output = (IndentingWriter)out;
            output.writeEOL();
        } else {
            out.write("\n");
        }

        // Write predicate/object pairs rendered first.
        int predicateIndex = 0;
        for (IRI predicate : firstPredicates) {
            if (poMap.containsKey(predicate)) {
                predicateIndex++;
                SortedTurtleObjectList values = poMap.get(predicate);
                writePredicateAndObjectValues(out, predicate, values);
                if (predicateIndex < poMap.size()) {
                    out.write(",");
                }
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.writeEOL();
                } else {
                    out.write("\n");
                }
            }
        }

        // Write other predicate/object pairs.
        for (IRI predicate : poMap.keySet()) {
            if (!firstPredicates.contains(predicate)) {
                predicateIndex++;
                SortedTurtleObjectList values = poMap.get(predicate);
                writePredicateAndObjectValues(out, predicate, values);
                if (predicateIndex < poMap.size()) {
                    out.write(",");
                }
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.writeEOL();
                } else {
                    out.write("\n");
                }
            }
        }

        // Close statement
        if (out instanceof IndentingWriter) {
            IndentingWriter output = (IndentingWriter)out;
            output.decreaseIndentation();
            out.write("}");
        } else {
            out.write("}");
        }
    }

    private String convertIriToString(IRI iri) {
        return convertIriToString(iri, useGeneratedPrefixes, /*useTurtleQuoting*/false, /*useJsonLdQuoting*/true);
    }

    protected void writePredicateAndObjectValues(Writer out, IRI predicate, SortedTurtleObjectList values) throws Exception {
        final boolean isRdfTypePredicate = rdfType.equals(predicate);
        out.write("\"");
        writePredicate(out, predicate);
        out.write("\" : ");
        if (values.size() == 1) {
            if (isRdfTypePredicate) {
                writeObject(out, (IRI)values.first(), isRdfTypePredicate);
            } else {
                writeObject(out, values.first());
            }
        } else if (values.size() > 1) {
            out.write("[");
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
                if (isRdfTypePredicate) {
                    writeObject(out, (IRI)value, isRdfTypePredicate);
                } else {
                    writeObject(out, value);
                }
                if (valueIndex < numValues) { out.write(","); }
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.writeEOL();
                } else {
                    out.write("\n");
                }
            }
            if (out instanceof IndentingWriter) {
                IndentingWriter output = (IndentingWriter)out;
                output.writeEOL();
                output.decreaseIndentation();
            } else {
                out.write("\n");
            }
            out.write("]");
        }
    }

    protected void writePredicate(Writer out, IRI predicate) throws Exception {
        writeIri(out, predicate);
    }

    protected void writeIri(Writer out, IRI iri) throws Exception {
        out.write(convertIriToString(iri));
    }

    protected void writeObject(Writer out, Value value) throws Exception {
        if (value instanceof BNode) {
            writeObject(out, (BNode) value);
        } else if (value instanceof IRI) {
            writeObject(out, (IRI)value);
        } else if (value instanceof Literal) {
            writeObject(out, (Literal)value);
        } else {
            out.write("\"" + value.stringValue() + "\"");
        }
    }

    protected void writeObject(Writer out, BNode bnode) throws Exception {
        if (inlineBlankNodes) {
            if (isCollection(bnode, collectionClass)) {
                // Open braces
                out.write("{");
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.writeEOL();
                    output.increaseIndentation();
                } else {
                    out.write("\n");
                }

                // Write collection members
                out.write("\"@list\" : [");
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.writeEOL();
                    output.increaseIndentation();
                } else {
                    out.write("\n");
                }
                ArrayList<Value> members = getCollectionMembers(bnode, collectionClass);
                int memberIndex = 0;
                for (Value member : members) {
                    memberIndex++;
                    writeObject(out, member);
                    if (memberIndex < members.size()) {
                        out.write(",");
                        if (out instanceof IndentingWriter) {
                            IndentingWriter output = (IndentingWriter)out;
                            output.writeEOL();
                        } else {
                            out.write("\n");
                        }
                    }
                }
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.writeEOL();
                    output.decreaseIndentation();
                } else {
                    out.write("\n");
                }
                out.write("]");

                // Close braces
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.decreaseIndentation();
                    out.write("}");
                } else {
                    out.write("}");
                }
            } else { // not a collection
                SortedTurtlePredicateObjectMap poMap = sortedTripleMap.get(bnode);
                if (poMap == null) { poMap = new SortedTurtlePredicateObjectMap(); }

                // Open braces
                out.write("{");
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.writeEOL();
                    output.increaseIndentation();
                } else {
                    out.write("\n");
                }

                // Write predicate/object pairs rendered first.
                int predicateIndex = 0;
                for (IRI predicate : firstPredicates) {
                    if (poMap.containsKey(predicate)) {
                        predicateIndex++;
                        SortedTurtleObjectList values = poMap.get(predicate);
                        writePredicateAndObjectValues(out, predicate, values);
                        if (predicateIndex < poMap.size()) {
                            out.write(",");
                        }
                        if (out instanceof IndentingWriter) {
                            IndentingWriter output = (IndentingWriter)out;
                            output.writeEOL();
                        } else {
                            out.write("\n");
                        }
                    }
                }

                // Write other predicate/object pairs.
                for (IRI predicate : poMap.keySet()) {
                    if (!firstPredicates.contains(predicate)) {
                        predicateIndex++;
                        SortedTurtleObjectList values = poMap.get(predicate);
                        writePredicateAndObjectValues(out, predicate, values);
                        if (predicateIndex < poMap.size()) {
                            out.write(",");
                        }
                        if (out instanceof IndentingWriter) {
                            IndentingWriter output = (IndentingWriter)out;
                            output.writeEOL();
                        } else {
                            out.write("\n");
                        }
                    }
                }

                // Close braces
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.decreaseIndentation();
                    out.write("}");
                } else {
                    out.write("}");
                }
            }
        } else { // no inlining of blank nodes
            if (unsortedTripleMap.containsKey(bnode)) {
                out.write("{ \"@id\" : \"_:" + blankNodeNameMap.get(bnode) + "\" }");
            } else {
                System.out.println("**** blank node not a subject: " + bnode.stringValue()); System.out.flush();
                out.write("{ }"); // last resort - this should never happen
            }
        }
    }

    protected void writeObject(Writer out, IRI iri) throws Exception {
        writeObject(out, iri, false);
    }

    protected void writeObject(Writer out, IRI iri, boolean isRdfType) throws Exception {
        out.write(isRdfType ? "\"" : "{ \"@id\" : \"");
        writeIri(out, iri);
        out.write(isRdfType ? "\"" : "\" }");
    }

    protected void writeObject(Writer out, Literal literal) throws Exception {
        if (literal == null) {
            out.write("null<Literal>");
        } else if (literal.getLanguage().isPresent()) {
            out.write("{");
            if (out instanceof IndentingWriter) {
                IndentingWriter output = (IndentingWriter)out;
                output.writeEOL();
                output.increaseIndentation();
            } else {
                out.write("\n");
            }

            out.write("\"@language\" : \"" + literal.getLanguage().get() + "\",");
            if (out instanceof IndentingWriter) {
                IndentingWriter output = (IndentingWriter)out;
                output.writeEOL();
            } else {
                out.write("\n");
            }

            out.write("\"@value\" : \"" + literal.stringValue() + "\"");
            if (out instanceof IndentingWriter) {
                IndentingWriter output = (IndentingWriter)out;
                output.writeEOL();
            } else {
                out.write("\n");
            }

            if (out instanceof IndentingWriter) {
                IndentingWriter output = (IndentingWriter)out;
                output.decreaseIndentation();
                out.write("}");
            } else {
                out.write("}");
            }
        } else if (literal.getDatatype() != null) {
            boolean useExplicit = (stringDataTypeOption == SesameSortedRDFWriterFactory.StringDataTypeOptions.explicit) || !(xsString.equals(literal.getDatatype()) || rdfLangString.equals(literal.getDatatype()));
            if (useExplicit) {
                out.write("{");
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.writeEOL();
                    output.increaseIndentation();
                } else {
                    out.write("\n");
                }

                out.write("\"@type\" : \"");
                writeIri(out, literal.getDatatype());
                out.write("\",");
                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.writeEOL();
                } else {
                    out.write("\n");
                }

                out.write("\"@value\" : ");
                writeString(out, literal.stringValue());

                if (out instanceof IndentingWriter) {
                    IndentingWriter output = (IndentingWriter)out;
                    output.decreaseIndentation();
                    output.writeEOL();
                    out.write("}");
                } else {
                    out.write("\n}");
                }
            } else {
                writeString(out, literal.stringValue());
            }
        } else {
            writeString(out, literal.stringValue());
        }
    }

    protected void writeString(Writer out, String str) throws Exception {
        // Note that JSON does not support multi-line strings, unlike Turtle
        if (str == null) { return; }
        out.write("\"");
        out.write(escapeString(str));
        out.write("\"");
    }

    protected void writeFooter(Writer out, String[] trailingComments) throws Exception {
        // Write closing bracket for subject list.
        output.writeEOL();
        output.decreaseIndentation();
        output.write("]");
        output.writeEOL();

        // Process traiing comments, if any.
        if ((trailingComments != null) && (trailingComments.length >= 1)) {
            System.err.println("#### trailing comments ignored - JSON-LD does not support comments");
            System.err.flush();
        }
    }

    private String escapeString(String str) { // JSON does not support multi-line strings, different to Turtle
        if (str == null) { return null; }
        return str.replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r").replaceAll("\\\\", "\\\\\\\\");
    }

}
