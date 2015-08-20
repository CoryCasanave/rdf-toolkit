package org.edmcouncil.rdf_toolkit;

import org.openrdf.model.*;
import org.openrdf.rio.RDFHandlerException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;

/**
 * Equivalent to Sesame's built-in RDF/XML writer, but the triples are sorted into a consistent order.
 * In order to do the sorting, it must be possible to load all of the RDF statements into memory.
 * NOTE: comments are suppressed, as there isn't a clear way to sort them along with triples.
 */
public class SesameSortedRdfXmlWriter extends SesameSortedRDFWriter {
    // TODO: the 'out' parameter in 'write...' methods is not used, and should be refactored out of the code.  Perhaps.  One day.

//    private static final Logger logger = LoggerFactory.getLogger(SesameSortedRdfXmlWriter.class);

    /** Output stream for this RDF/XML writer. */
    // Note: this is an internal Java class, not part of the published API.  But easier than writing our own indenter here.
    private IndentingXMLStreamWriter output = null;

    /** Namespace prefix for the RDF namespace. */
    private String rdfPrefix = "rdf";
    private String xmlPrefix = "xml";

    /**
     * Creates an RDFWriter instance that will write sorted RDF/XML to the supplied output stream.
     *
     * @param out The OutputStream to write the RDF/XML to.
     */
    public SesameSortedRdfXmlWriter(OutputStream out) throws Exception {
        super(out);
        this.output = new IndentingXMLStreamWriter(out);
    }

    /**
     * Creates an RDFWriter instance that will write sorted RDF/XML to the supplied writer.
     *
     * @param writer The Writer to write the RDF/XML to.
     */
    public SesameSortedRdfXmlWriter(Writer writer) throws Exception {
        super(writer);
        this.output = new IndentingXMLStreamWriter(writer);
    }

    /**
     * Creates an RDFWriter instance that will write sorted RDF/XML to the supplied output stream.
     *
     * @param out The OutputStream to write the RDF/XML to.
     * @param options options for the RDF/XML writer.
     */
    public SesameSortedRdfXmlWriter(OutputStream out, Map<String, Object> options) throws Exception {
        super(out, options);
        String indent = options.containsKey("indent") ? ((String) options.get("indent")) : null;
        this.output = new IndentingXMLStreamWriter(out, "UTF-8", indent);
    }

    /**
     * Creates an RDFWriter instance that will write sorted RDF/XML to the supplied writer.
     *
     * @param writer The Writer to write the RDF/XML to.
     * @param options options for the RDF/XML writer.
     */
    public SesameSortedRdfXmlWriter(Writer writer, Map<String, Object> options) throws Exception {
        super(writer, options);
        String indent = options.containsKey("indent") ? ((String) options.get("indent")) : null;
        this.output = new IndentingXMLStreamWriter(writer, indent);
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
    }

    protected void writeHeader(Writer out, SortedTurtleObjectList importList) throws Exception {
        // Get prefixes used for the XML
        rdfPrefix = reverseNamespaceTable.get(RDF_NS_URI);

        // Create a sorted list of namespace prefix mappings.
        TreeSet<String> prefixes = new TreeSet<String>(namespaceTable.keySet());

        // Write the XML prologue <?xml ... ?>
        output.writeStartDocument(output.getXmlEncoding(), "1.0");
        output.writeEOL();

        // Write the DTD subset, if required
        if (useDtdSubset) {
            output.startDTD("rdf:RDF");
            if (namespaceTable.size() > 0) {
                for (String prefix : prefixes) {
                    if (prefix.length() >= 1) {
                        output.writeDtdEntity(prefix, namespaceTable.get(prefix));
                    }
                }
            }
            output.endDTD();
        }

        // Open the root element.
        output.writeStartElement(rdfPrefix, "RDF", RDF_NS_URI); // <rdf:RDF>

        // Write the baseURI, if any.
        if (baseUri != null) {
            output.writeAttribute("xml", XML_NS_URI, "base", baseUri.stringValue());
        }

        // Write the namespace declarations into the root element.
        if (namespaceTable.size() > 0) {
            for (String prefix : prefixes) {
                if (!"xml".equals(prefix)) {
                    if (prefix.length() >= 1) {
                        output.writeNamespace(prefix, namespaceTable.get(prefix));
                    } else {
                        output.writeDefaultNamespace(namespaceTable.get(prefix));
                    }
                }
            }
        } else { // create RDF namespace at a minimum
            output.writeNamespace(rdfPrefix, RDF_NS_URI);
        }

        addDefaultNamespacePrefixIfMissing(XML_NS_URI, "xml"); // RDF/XML sometimes uses the 'xml' prefix, e.g. xml:lang.  This prefix is never declared explicitly.
        reverseNamespaceTable.put(XML_NS_URI, "xml"); // need to update reverse namespace table manually

        output.writeAttribute("xml", XML_NS_URI, "space", "preserve"); // make sure whitespace is preserved, for consistency of formatting

        output.writeCharacters(""); // force writing of closing angle bracket in root element open tag
        output.writeEOL(); // add extra EOL after root element

    }

    protected void writeSubjectTriples(Writer out, Resource subject) throws Exception {
        SortedTurtlePredicateObjectMap poMap = sortedTripleMap.get(subject);

        // Try to determine whether to use <rdf:Description> or an element based on rdf:type value.
        SortedTurtleObjectList subjectRdfTypes = poMap.get(rdfType); // needed to determine if a type can be used as the XML element name
        URI enclosingElementURI = rdfDescription; // default value
        QName enclosingElementQName = convertUriToQName(enclosingElementURI);
        if ((subjectRdfTypes != null) && (subjectRdfTypes.size() == 1)) {
            Value subjectRdfTypeValue = subjectRdfTypes.first();
            if (subjectRdfTypeValue instanceof URI) {
                QName subjectRdfTypeQName = convertUriToQName((URI) subjectRdfTypeValue);
                if (subjectRdfTypeQName != null) {
                    enclosingElementURI = (URI) subjectRdfTypeValue;
                    enclosingElementQName = subjectRdfTypeQName;
                }
            }
        }

        // Write enclosing element.
        // The variation used for "rdf:about", or "rdf:nodeID", depends on settings and also whether the subject is a blank node or not.
        output.writeStartElement(enclosingElementQName.getPrefix(), enclosingElementQName.getLocalPart(), enclosingElementQName.getNamespaceURI());
        if (subject instanceof BNode) {
            if (!inlineBlankNodes) {
                output.writeAttribute(reverseNamespaceTable.get(RDF_NS_URI), RDF_NS_URI, "nodeID", blankNodeNameMap.get(subject));
            }
        } else if (subject instanceof URI) {
            output.writeStartAttribute(reverseNamespaceTable.get(RDF_NS_URI), RDF_NS_URI, "about");
            QName subjectQName = convertUriToQName((URI)subject);
            if ((subjectQName != null) && (subjectQName.getPrefix() != null) && (subjectQName.getPrefix().length() >= 1)) { // if a prefix is defined, write out the subject QName using an entity reference
                output.writeAttributeEntityRef(subjectQName.getPrefix());
                output.writeAttributeCharacters(((URI) subject).getLocalName());
            } else { // just write the whole subject URI
                output.writeAttributeCharacters(subject.toString());
            }
            output.endAttribute();
        } else {
            output.writeAttribute(reverseNamespaceTable.get(RDF_NS_URI), RDF_NS_URI, "about", subject.stringValue()); // this shouldn't occur, but ...
        }

        // Write predicate/object pairs rendered first.
        for (URI predicate : firstPredicates) {
            if (poMap.containsKey(predicate)) {
                SortedTurtleObjectList values = poMap.get(predicate);
                if (predicate == rdfType) { // assumes that rdfType is one of the firstPredicates
                    values.remove(enclosingElementURI); // no need to state type explicitly if it has been used as an enclosing element name
                }
                if (values.size() >= 1) {
                    writePredicateAndObjectValues(out, predicate, values);
                }
            }
        }

        // Write other predicate/object pairs.
        for (URI predicate : poMap.keySet()) {
            if (!firstPredicates.contains(predicate)) {
                SortedTurtleObjectList values = poMap.get(predicate);
                writePredicateAndObjectValues(out, predicate, values);
            }
        }

        // Close enclosing element.
        output.writeEndElement();
        if (!inlineBlankNodes || !(subject instanceof BNode)) {
            output.writeEOL();
        }
    }

    protected void writePredicateAndObjectValues(Writer out, URI predicate, SortedTurtleObjectList values) throws Exception {
        // Get prefixes used for the XML
        rdfPrefix = reverseNamespaceTable.get(RDF_NS_URI);
        xmlPrefix = reverseNamespaceTable.get(XML_NS_URI);

        QName predicateQName = convertUriToQName(predicate);
        for (Value value : values) {
            if (inlineBlankNodes && (value instanceof BNode)) {
                output.writeStartElement(predicateQName.getPrefix(), predicateQName.getLocalPart(), predicateQName.getNamespaceURI());
                writeSubjectTriples(out, (Resource)value);
                output.writeEndElement();
            } else { // not an inline blank node`
                if ((value instanceof BNode) || (value instanceof URI)) {
                    output.writeEmptyElement(predicateQName.getPrefix(), predicateQName.getLocalPart(), predicateQName.getNamespaceURI());
                } else {
                    output.writeStartElement(predicateQName.getPrefix(), predicateQName.getLocalPart(), predicateQName.getNamespaceURI());
                }
                if (value instanceof BNode) {
                    output.writeAttribute(rdfPrefix, RDF_NS_URI, "nodeID", blankNodeNameMap.get(value));
                } else if (value instanceof URI) {
                    output.writeStartAttribute(rdfPrefix, RDF_NS_URI, "resource");
                    QName uriQName = convertUriToQName((URI) value);
                    if (uriQName == null) {
                        output.writeAttributeCharacters(value.stringValue());
                    } else {
                        if ((uriQName.getPrefix() != null) && (uriQName.getPrefix().length() >= 1)) {
                            output.writeAttributeEntityRef(uriQName.getPrefix());
                            output.writeAttributeCharacters(uriQName.getLocalPart());
                        } else {
                            output.writeAttributeCharacters(value.stringValue());
                        }
                    }
                    output.endAttribute();
                } else if (value instanceof Literal) {
                    if (((Literal)value).getDatatype() != null) {
                        output.writeStartAttribute(rdfPrefix, RDF_NS_URI, "datatype");
                        QName datatypeQName = convertUriToQName(((Literal)value).getDatatype());
                        if ((datatypeQName == null) || (datatypeQName.getPrefix() == null) || (datatypeQName.getPrefix().length() < 1)) {
                            output.writeAttributeCharacters(((Literal)value).getDatatype().stringValue());
                        } else {
                            output.writeAttributeEntityRef(datatypeQName.getPrefix());
                            output.writeAttributeCharacters(datatypeQName.getLocalPart());
                        }
                        output.endAttribute();
                    }
                    if (((Literal)value).getLanguage() != null) {
                        output.writeAttribute(xmlPrefix, XML_NS_URI, "lang", ((Literal)value).getLanguage());
                    }
                    output.writeCharacters(value.stringValue());
                } else {
                    output.writeCharacters(value.stringValue());
                }
                output.writeEndElement();
            }
        }
    }

    protected void writeFooter(Writer out) throws Exception {
        output.writeEndElement(); // </rdf:RDF>
        output.writeEndDocument();
    }

}
