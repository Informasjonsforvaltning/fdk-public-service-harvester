package no.fdk.fdk_public_service_harvester.service

import no.fdk.fdk_public_service_harvester.model.TurtleDBO
import no.fdk.fdk_public_service_harvester.rdf.JenaType
import no.fdk.fdk_public_service_harvester.rdf.createRDFResponse
import no.fdk.fdk_public_service_harvester.repository.TurtleRepository
import org.apache.jena.rdf.model.Model
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8


private const val NO_RECORDS_ID_PREFIX = "no-records-"
const val UNION_ID = "services-union-graph"

@Service
class TurtleService(private val turtleRepository: TurtleRepository) {

    fun saveAsUnion(model: Model) =
        turtleRepository.save(model.createUnionTurtleDBO())

    fun getUnion(): String? =
        turtleRepository.findByIdOrNull(UNION_ID)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsPublicService(model: Model, fdkId: String, withRecords: Boolean) =
        turtleRepository.save(model.createPublicServiceTurtleDBO(fdkId, withRecords))

    fun getPublicService(fdkId: String, withRecords: Boolean): String? =
        turtleRepository.findByIdOrNull(turtleId(fdkId, withRecords))
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsHarvestSource(model: Model, uri: String) =
        turtleRepository.save(model.createHarvestSourceTurtleDBO(uri))

    fun getHarvestSource(uri: String): String? =
        turtleRepository.findByIdOrNull(uri)
            ?.turtle
            ?.let { ungzip(it) }

}

private fun Model.createUnionTurtleDBO(): TurtleDBO =
    TurtleDBO(
        id = UNION_ID,
        turtle = gzip(createRDFResponse(JenaType.TURTLE))
    )

private fun Model.createPublicServiceTurtleDBO(fdkId: String, withRecords: Boolean): TurtleDBO =
    TurtleDBO(
        id = turtleId(fdkId, withRecords),
        turtle = gzip(createRDFResponse(JenaType.TURTLE))
    )

fun turtleId(fdkId: String, withRecords: Boolean): String =
    "${if (withRecords) "" else NO_RECORDS_ID_PREFIX}$fdkId"

private fun Model.createHarvestSourceTurtleDBO(uri: String): TurtleDBO =
    TurtleDBO(
        id = uri,
        turtle = gzip(createRDFResponse(JenaType.TURTLE))
    )

fun gzip(content: String): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
    return Base64.getEncoder().encodeToString(bos.toByteArray())
}

fun ungzip(base64Content: String): String {
    val content = Base64.getDecoder().decode(base64Content)
    return GZIPInputStream(content.inputStream())
        .bufferedReader(UTF_8)
        .use { it.readText() }
}
