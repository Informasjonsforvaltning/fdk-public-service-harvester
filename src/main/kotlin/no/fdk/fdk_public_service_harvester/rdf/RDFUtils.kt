package no.fdk.fdk_public_service_harvester.rdf

import no.fdk.fdk_public_service_harvester.Application
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.StringReader

private val logger = LoggerFactory.getLogger(Application::class.java)
const val BACKUP_BASE_URI = "http://example.com/"

fun jenaTypeFromAcceptHeader(accept: String?): Lang? =
    when {
        accept == null -> null
        accept.contains(Lang.TURTLE.headerString) -> Lang.TURTLE
        accept.contains("text/n3") -> Lang.N3
        accept.contains(Lang.TRIG.headerString) -> Lang.TRIG
        accept.contains(Lang.RDFXML.headerString) -> Lang.RDFXML
        accept.contains(Lang.RDFJSON.headerString) -> Lang.RDFJSON
        accept.contains(Lang.JSONLD.headerString) -> Lang.JSONLD
        accept.contains(Lang.NTRIPLES.headerString) -> Lang.NTRIPLES
        accept.contains(Lang.NQUADS.headerString) -> Lang.NQUADS
        accept.contains(Lang.TRIX.headerString) -> Lang.TRIX
        accept.contains("*/*") -> null
        else -> Lang.RDFNULL
    }

fun parseRDFResponse(responseBody: String, rdfLanguage: Lang, rdfSource: String?): Model? {
    val responseModel = ModelFactory.createDefaultModel()

    try {
        responseModel.read(StringReader(responseBody), BACKUP_BASE_URI, rdfLanguage.name)
    } catch (ex: Exception) {
        logger.error("Parse from $rdfSource has failed", ex)
        return null
    }

    return responseModel
}

fun Model.createRDFResponse(responseType: Lang): String =
    ByteArrayOutputStream().use{ out ->
        write(out, responseType.name)
        out.flush()
        out.toString("UTF-8")
    }
