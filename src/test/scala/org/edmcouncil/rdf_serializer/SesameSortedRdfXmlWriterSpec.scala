package org.edmcouncil.rdf_serializer

import scala.language.postfixOps

import java.io._
import java.nio.charset.Charset
import java.util

import org.edmcouncil.rdf_serializer.SesameSortedRDFWriter.ShortUriPreferences
import org.edmcouncil.rdf_serializer.SesameSortedRDFWriterFactory.TargetFormats
import org.openrdf.model.impl.URIImpl
import org.openrdf.rio.{RDFWriter, RDFFormat, Rio}
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriterFactory
import org.slf4j.LoggerFactory

import org.scalatest.{Matchers, FlatSpec}

/**
 * ScalaTest tests for the SesameSortedRdfXmlWriter and SesameSortedRDFWriterFactory.
 */
class SesameSortedRdfXmlWriterSpec extends FlatSpec with Matchers with SesameSortedWriterSpecSupport /*with OutputSuppressor*/ {

  override val logger = LoggerFactory.getLogger(classOf[SesameSortedRdfXmlWriterSpec])

  val outputDir0 = mkCleanDir(s"target//temp//${classOf[RDFWriter].getName}")
  val outputDir1 = mkCleanDir(s"target//temp//${this.getClass.getName}")
  val outputDir2 = mkCleanDir(s"target//temp//${this.getClass.getName}_2")

  "A SortedRDFWriterFactory" should "be able to create a SortedRdfXmlWriter" in {
    val outWriter = new OutputStreamWriter(System.out)
    val factory = new SesameSortedRDFWriterFactory(TargetFormats.rdf_xml)

    val writer1 = new SesameSortedRdfXmlWriter(System.out)
    assert(writer1 != null, "failed to create default SortedTurtleWriter from OutputStream")

    val writer2 = new SesameSortedRdfXmlWriter(outWriter)
    assert(writer2 != null, "failed to create default SortedTurtleWriter from Writer")

    val writer3Options = new util.HashMap[String, Object]()
    writer3Options.put("baseUri", new URIImpl("http://example.com#"))
    writer3Options.put("indent", "\t\t")
    writer3Options.put("shortUriPref", ShortUriPreferences.prefix)
    val writer3 = new SesameSortedRdfXmlWriter(System.out, writer3Options)
    assert(writer3 != null, "failed to create default SortedTurtleWriter from OutputStream with parameters")

    val writer4Options = new util.HashMap[String, Object]()
    writer4Options.put("baseUri", new URIImpl("http://example.com#"))
    writer4Options.put("indent", "\t\t")
    writer4Options.put("shortUriPref", ShortUriPreferences.base_uri)
    val writer4 = new SesameSortedRdfXmlWriter(outWriter, writer4Options)
    assert(writer4 != null, "failed to create default SortedTurtleWriter from Writer")
  }

  "An RDFWriter" should "be able to read various RDF documents and write them in RDF/XML format" in {
    val rawTurtleDirectory = new File("src/test/resources")
    assert(rawTurtleDirectory isDirectory, "raw turtle directory is not a directory")
    assert(rawTurtleDirectory exists, "raw turtle directory does not exist")

    var fileCount = 0
    for (sourceFile <- listDirTreeFiles(rawTurtleDirectory)) {
      fileCount += 1
      val targetFile = new File(outputDir0, setFilePathExtension(sourceFile getName, "rdf"))
      val outWriter = new OutputStreamWriter(new FileOutputStream(targetFile), "UTF-8")
      val factory = new RDFXMLPrettyWriterFactory()
      val turtleWriter = factory getWriter (outWriter)
      val rdfFormat = Rio getParserFormatForFileName(sourceFile getName, RDFFormat.RDFXML)

      val inputModel = Rio parse (new FileReader(sourceFile), "", rdfFormat)
      Rio write (inputModel, turtleWriter)
      outWriter flush ()
      outWriter close ()
    }
  }

  "A SortedRdfXmlWriter" should "be able to produce a sorted RDF/XML file" in {
    val inputFile = new File("src/test/resources/other/topbraid-countries-ontology.ttl")
    val baseUri = new URIImpl("http://topbraid.org/countries")
    val outputFile = new File(outputDir1, setFilePathExtension(inputFile getName, "rdf"))
    val outWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
    val factory = new SesameSortedRDFWriterFactory(SesameSortedRDFWriterFactory.TargetFormats.rdf_xml)
    val rdfXmlWriterOptions = new util.HashMap[String, Object]()
    rdfXmlWriterOptions.put("baseUri", baseUri)
    rdfXmlWriterOptions.put("useDtdSubset", java.lang.Boolean.TRUE)
    val rdfXmlWriter = factory getWriter (outWriter, rdfXmlWriterOptions)

    val inputModel = Rio parse (new FileReader(inputFile), baseUri stringValue, RDFFormat.TURTLE)
    Rio write (inputModel, rdfXmlWriter)
    outWriter flush ()
    outWriter close ()

    val outputFile2 = new File(outputDir2, outputFile getName)
    val outWriter2 = new OutputStreamWriter(new FileOutputStream(outputFile2), "UTF-8")
    val rdfXmlWriter2Options = new util.HashMap[String, Object]()
    rdfXmlWriter2Options.put("baseUri", baseUri)
    rdfXmlWriter2Options.put("useDtdSubset", java.lang.Boolean.TRUE)
    val rdfXmlWriter2 = factory getWriter (outWriter2, rdfXmlWriter2Options)

    val inputModel2 = Rio parse (new FileReader(outputFile), baseUri stringValue, RDFFormat.RDFXML)
    Rio write (inputModel2, rdfXmlWriter2)
    outWriter2 flush ()
    outWriter2 close ()
  }

//  it should "be able to produce a sorted Turtle file with blank object nodes" in {
//    val inputFile = new File("src/test/resources/other/topquadrant-extended-turtle-example.ttl")
//    val outputFile = new File(outputDir1, setFilePathExtension(inputFile getName, "ttl"))
//    val outWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
//    val factory = new SesameSortedRDFWriterFactory()
//    val turtleWriter = factory getWriter outWriter
//
//    val inputModel = Rio parse (new FileReader(inputFile), "", RDFFormat.TURTLE)
//    Rio write (inputModel, turtleWriter)
//    outWriter flush ()
//    outWriter close ()
//
//    val outputFile2 = new File(outputDir2, outputFile getName)
//    val outWriter2 = new OutputStreamWriter(new FileOutputStream(outputFile2), "UTF-8")
//    val turtleWriter2 = factory getWriter (outWriter2)
//
//    val inputModel2 = Rio parse (new FileReader(outputFile), "", RDFFormat.TURTLE)
//    Rio write (inputModel2, turtleWriter2)
//    outWriter2 flush ()
//    outWriter2 close ()
//  }
//
//  it should "be able to produce a sorted Turtle file with blank subject nodes" in {
//    val inputFile = new File("src/test/resources/rdf_turtle_spec/turtle-example-17.ttl")
//    val outputFile = new File(outputDir1, setFilePathExtension(inputFile getName, "ttl"))
//    val outWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
//    val factory = new SesameSortedRDFWriterFactory()
//    val turtleWriter = factory getWriter (outWriter)
//
//    val inputModel = Rio parse (new FileReader(inputFile), "", RDFFormat.TURTLE)
//    Rio write (inputModel, turtleWriter)
//    outWriter flush ()
//    outWriter close ()
//
//    val outputFile2 = new File(outputDir2, outputFile getName)
//    val outWriter2 = new OutputStreamWriter(new FileOutputStream(outputFile2), "UTF-8")
//    val turtleWriter2 = factory getWriter (outWriter2)
//
//    val inputModel2 = Rio parse (new FileReader(outputFile), "", RDFFormat.TURTLE)
//    Rio write (inputModel2, turtleWriter2)
//    outWriter2 flush ()
//    outWriter2 close ()
//  }
//
//  it should "be able to produce a sorted Turtle file with directly recursive blank object nodes" in {
//    val inputFile = new File("src/test/resources/rdf_turtle_spec/turtle-example-14.ttl")
//    val outputFile = new File(outputDir1, setFilePathExtension(inputFile getName, "ttl"))
//    val outWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
//    val factory = new SesameSortedRDFWriterFactory()
//    val turtleWriter = factory getWriter outWriter
//
//    val inputModel = Rio parse (new FileReader(inputFile), "", RDFFormat.TURTLE)
//    Rio write (inputModel, turtleWriter)
//    outWriter flush ()
//    outWriter close ()
//
//    val outputFile2 = new File(outputDir2, outputFile getName)
//    val outWriter2 = new OutputStreamWriter(new FileOutputStream(outputFile2), "UTF-8")
//    val turtleWriter2 = factory getWriter (outWriter2)
//
//    val inputModel2 = Rio parse (new FileReader(outputFile), "", RDFFormat.TURTLE)
//    Rio write (inputModel2, turtleWriter2)
//    outWriter2 flush ()
//    outWriter2 close ()
//  }
//
//  it should "be able to produce a sorted Turtle file with indirectly recursive blank object nodes" in {
//    val inputFile = new File("src/test/resources/rdf_turtle_spec/turtle-example-26.ttl")
//    val outputFile = new File(outputDir1, setFilePathExtension(inputFile getName, "ttl"))
//    val outWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
//    val factory = new SesameSortedRDFWriterFactory()
//    val turtleWriter = factory getWriter outWriter
//
//    val inputModel = Rio parse (new FileReader(inputFile), "", RDFFormat.TURTLE)
//    Rio write (inputModel, turtleWriter)
//    outWriter flush ()
//    outWriter close ()
//
//    val outputFile2 = new File(outputDir2, outputFile getName)
//    val outWriter2 = new OutputStreamWriter(new FileOutputStream(outputFile2), "UTF-8")
//    val turtleWriter2 = factory getWriter (outWriter2)
//
//    val inputModel2 = Rio parse (new FileReader(outputFile), "", RDFFormat.TURTLE)
//    Rio write (inputModel2, turtleWriter2)
//    outWriter2 flush ()
//    outWriter2 close ()
//  }
//
//  it should "be able to produce a sorted Turtle file preferring prefix over base URI" in {
//    val inputFile = new File("src/test/resources/other/topbraid-countries-ontology.ttl")
//    val baseUri = new URIImpl("http://topbraid.org/countries")
//    val outputFile = new File(outputDir1, "topbraid-countries-ontology_prefix.ttl")
//    val outWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
//    val factory = new SesameSortedRDFWriterFactory()
//    val turtleWriter = factory getWriter (outWriter, baseUri, null, ShortUriPreferences.prefix)
//
//    val inputModel = Rio parse (new InputStreamReader(new FileInputStream(inputFile), "UTF-8"), baseUri stringValue, RDFFormat.TURTLE)
//    Rio write (inputModel, turtleWriter)
//    outWriter flush ()
//    outWriter close ()
//    val contents1 = getFileContents(outputFile, "UTF-8")
//    assert(contents1.contains("countries:AD"), "prefix preference has failed (1a)")
//    assert(!contents1.contains("#AD"), "prefix preference has failed (1b)")
//    assert(contents1.contains("Åland"), "prefix preference file has encoding problem (1)")
//
//    val outputFile2 = new File(outputDir2, "topbraid-countries-ontology_prefix.ttl")
//    val outWriter2 = new OutputStreamWriter(new FileOutputStream(outputFile2), "UTF-8")
//    val turtleWriter2 = factory getWriter (outWriter2, baseUri, null, ShortUriPreferences.prefix)
//
//    val inputModel2 = Rio parse (new InputStreamReader(new FileInputStream(outputFile), "UTF-8"), baseUri stringValue, RDFFormat.TURTLE)
//    Rio write (inputModel2, turtleWriter2)
//    outWriter2 flush ()
//    outWriter2 close ()
//    val contents2 = getFileContents(outputFile2, "UTF-8")
//    assert(contents2.contains("countries:AD"), "prefix preference has failed (2a)")
//    assert(!contents2.contains("#AD"), "prefix preference has failed (2b)")
//    assert(contents2.contains("Åland"), "prefix preference file has encoding problem (2)")
//  }
//
//  it should "be able to produce a sorted Turtle file preferring base URI over prefix" in {
//    val inputFile = new File("src/test/resources/other/topbraid-countries-ontology.ttl")
//    val baseUri = new URIImpl("http://topbraid.org/countries")
//    val outputFile = new File(outputDir1, "topbraid-countries-ontology_base_uri.ttl")
//    val outWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")
//    val factory = new SesameSortedRDFWriterFactory()
//    val turtleWriter = factory getWriter (outWriter, baseUri, null, ShortUriPreferences.base_uri)
//
//    val inputModel = Rio parse (new InputStreamReader(new FileInputStream(inputFile), "UTF-8"), baseUri stringValue, RDFFormat.TURTLE)
//    Rio write (inputModel, turtleWriter)
//    outWriter flush ()
//    outWriter close ()
//    val contents1 = getFileContents(outputFile, "UTF-8")
//    assert(contents1.contains("#AD"), "base URI preference has failed (1a)")
//    assert(!contents1.contains("countries:AD"), "base URI preference has failed (1b)")
//    assert(contents1.contains("Åland"), "base URI preference file has encoding problem (1)")
//
//    val outputFile2 = new File(outputDir2, "topbraid-countries-ontology_base_uri.ttl")
//    val outWriter2 = new OutputStreamWriter(new FileOutputStream(outputFile2), "UTF-8")
//    val turtleWriter2 = factory getWriter (outWriter2, baseUri, null, ShortUriPreferences.base_uri)
//
//    val inputModel2 = Rio parse (new InputStreamReader(new FileInputStream(outputFile), "UTF-8"), baseUri stringValue, RDFFormat.TURTLE)
//    Rio write (inputModel2, turtleWriter2)
//    outWriter2 flush ()
//    outWriter2 close ()
//    val contents2 = getFileContents(outputFile2, "UTF-8")
//    assert(contents2.contains("#AD"), "base URI preference has failed (2a)")
//    assert(!contents2.contains("countries:AD"), "base URI preference has failed (2b)")
//    assert(contents2.contains("Åland"), "base URI preference file has encoding problem (2)")
//  }
//
//  it should "be able to read various RDF documents and write them in sorted Turtle format" in {
//    val rawTurtleDirectory = new File("src/test/resources")
//    assert(rawTurtleDirectory isDirectory, "raw turtle directory is not a directory")
//    assert(rawTurtleDirectory exists, "raw turtle directory does not exist")
//
//    var fileCount = 0
//    for (sourceFile <- listDirTreeFiles(rawTurtleDirectory)) {
//      fileCount += 1
//      val targetFile = new File(outputDir1, setFilePathExtension(sourceFile getName, "ttl"))
//      SesameRdfFormatter run Array[String](
//        "-s", sourceFile getAbsolutePath,
//        "-t", targetFile getAbsolutePath
//      )
//    }
//  }
//
//  it should "be able to sort RDF triples consistently when writing in Turtle format" in {
//    val rawTurtleDirectory = new File("src/test/resources")
//    assert(rawTurtleDirectory isDirectory, "raw turtle directory is not a directory")
//    assert(rawTurtleDirectory exists, "raw turtle directory does not exist")
//
//    // Serialise sample files as sorted Turtle.
//    var fileCount = 0
//    for (sourceFile <- listDirTreeFiles(rawTurtleDirectory)) {
//      fileCount += 1
//      val targetFile = new File(outputDir1, setFilePathExtension(sourceFile getName, "ttl"))
//      SesameRdfFormatter run Array[String](
//        "-s", sourceFile getAbsolutePath,
//        "-t", targetFile getAbsolutePath
//      )
//    }
//
//    // Re-serialise the sorted files, again as sorted Turtle.
//    fileCount = 0
//    for (sourceFile <- listDirTreeFiles(outputDir1) if !sourceFile.getName.contains("_prefix") && !sourceFile.getName.contains("_base_uri")) {
//      fileCount += 1
//      val targetFile = new File(outputDir2, setFilePathExtension(sourceFile getName, "ttl"))
//      SesameRdfFormatter run Array[String](
//        "-s", sourceFile getAbsolutePath,
//        "-t", targetFile getAbsolutePath
//      )
//    }
//
//    // Check that re-serialising the Turtle files has changed nothing.
//    fileCount = 0
//    for (file1 <- listDirTreeFiles(outputDir1)) {
//      fileCount += 1
//      val file2 = new File(outputDir2, file1 getName)
//      assert(file2 exists, s"file missing in outputDir2: ${file2.getName}")
//      assert(compareFiles(file1, file2, "UTF-8"), s"file mismatch between outputDir1 and outputDir2: ${file1.getName}")
//    }
//  }
//
//  it should "should not add/lose RDF triples when writing in Turtle format" in {
//    val rawTurtleDirectory = new File("src/test/resources")
//    assert(rawTurtleDirectory isDirectory, "raw turtle directory is not a directory")
//    assert(rawTurtleDirectory exists, "raw turtle directory does not exist")
//
//    // Serialise sample files as sorted Turtle.
//    var fileCount = 0
//    for (sourceFile <- listDirTreeFiles(rawTurtleDirectory)) {
//      fileCount += 1
//      val targetFile = new File(outputDir1, setFilePathExtension(sourceFile getName, "ttl"))
//      SesameRdfFormatter run Array[String](
//        "-s", sourceFile getAbsolutePath,
//        "-t", targetFile getAbsolutePath
//      )
//    }
//
//    // Re-serialise the sorted files, again as sorted Turtle.
//    fileCount = 0
//    for (sourceFile <- listDirTreeFiles(outputDir1) if !sourceFile.getName.contains("_prefix") && !sourceFile.getName.contains("_base_uri")) {
//      fileCount += 1
//      val targetFile = new File(outputDir2, setFilePathExtension(sourceFile getName, "ttl"))
//      SesameRdfFormatter run Array[String](
//        "-s", sourceFile getAbsolutePath,
//        "-t", targetFile getAbsolutePath
//      )
//    }
//
//    // Check that re-serialising the Turtle files has changed nothing.
//    fileCount = 0
//    for (file1 <- listDirTreeFiles(outputDir1)) {
//      fileCount += 1
//      val file2 = new File(outputDir2, file1 getName)
//      assert(file2 exists, s"file missing in outputDir2: ${file2.getName}")
//      assert(compareFiles(file1, file2, "UTF-8"), s"file mismatch between outputDir1 and outputDir2: ${file1.getName}")
//    }
//
//    // Check that the re-serialised Turtle file have the same triple count as the matching raw files
//    fileCount = 0
//    for (sourceFile <- listDirTreeFiles(rawTurtleDirectory)) {
//      fileCount += 1
//      val targetFile = new File(outputDir2, setFilePathExtension(sourceFile getName, "ttl"))
//      val rdfFormat1 = Rio getParserFormatForFileName(sourceFile getName, RDFFormat.TURTLE)
//      val inputModel1 = Rio parse (new FileReader(sourceFile), "", rdfFormat1)
//      val inputModel2 = Rio parse (new FileReader(targetFile), "", RDFFormat.TURTLE)
//      assert(inputModel1.size() === inputModel2.size(), s"ingested triples do not match for: ${sourceFile.getName}")
//    }
//  }
//
//  "A SesameRdfFormatter" should "be able to do pattern-based URI replacements" in {
//    val inputFile = new File("src/test/resources/other/topbraid-countries-ontology.ttl")
//    val outputFile = new File(outputDir1, "topbraid-countries-ontology_replaced.ttl")
//    SesameRdfFormatter run Array[String](
//      "-s", inputFile getAbsolutePath,
//      "-t", outputFile getAbsolutePath,
//      "-up", "^http://topbraid.org/countries",
//      "-ur", "http://replaced.example.org/countries"
//    )
//    val content = getFileContents(outputFile, "UTF-8")
//    assert(content.contains("@prefix countries: <http://replaced.example.org/countries#> ."), "URI replacement seems to have failed")
//  }

}
