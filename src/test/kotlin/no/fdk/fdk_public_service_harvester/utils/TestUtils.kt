package no.fdk.fdk_public_service_harvester.utils

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import no.fdk.fdk_public_service_harvester.model.MiscellaneousTurtle
import no.fdk.fdk_public_service_harvester.model.PublicServiceDBO
import no.fdk.fdk_public_service_harvester.rdf.JenaType
import no.fdk.fdk_public_service_harvester.rdf.createRDFResponse
import no.fdk.fdk_public_service_harvester.rdf.parseRDFResponse
import no.fdk.fdk_public_service_harvester.service.ungzip
import no.fdk.fdk_public_service_harvester.utils.ApiTestContext.Companion.mongoContainer
import org.apache.jena.rdf.model.Model
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.net.URL
import org.springframework.http.HttpStatus
import java.net.HttpURLConnection

private val logger = LoggerFactory.getLogger(ApiTestContext::class.java)

fun apiGet(endpoint: String, acceptHeader: String?, port: Int): Map<String,Any> {

    return try {
        val connection = URL("http://localhost:$port$endpoint").openConnection() as HttpURLConnection
        if(acceptHeader != null) connection.setRequestProperty("Accept", acceptHeader)
        connection.connect()

        if(isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body"   to responseBody,
                "header" to connection.headerFields.toString(),
                "status" to connection.responseCode)
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body"   to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body"   to " "
        )
    }
}

private fun isOK(response: Int?): Boolean =
    if(response == null) false
    else HttpStatus.resolve(response)?.is2xxSuccessful == true

fun populateDB() {
    val connectionString = ConnectionString("mongodb://${MONGO_USER}:${MONGO_PASSWORD}@localhost:${mongoContainer.getMappedPort(MONGO_PORT)}/$MONGO_COLLECTION?authSource=admin&authMechanism=SCRAM-SHA-1")
    val pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()))

    val client: MongoClient = MongoClients.create(connectionString)
    val mongoDatabase = client.getDatabase(MONGO_COLLECTION).withCodecRegistry(pojoCodecRegistry)

    val miscCollection = mongoDatabase.getCollection("misc")
    miscCollection.insertMany(miscDBPopulation())

    val serviceCollection = mongoDatabase.getCollection("services")
    serviceCollection.insertMany(serviceDBPopulation())

    client.close()
}

fun MiscellaneousTurtle.printTurtleDiff(expected: MiscellaneousTurtle) {
    checkIfIsomorphicAndPrintDiff(
        actual = parseRDFResponse(ungzip(turtle), JenaType.TURTLE, null)!!,
        expected = parseRDFResponse(ungzip(expected.turtle), JenaType.TURTLE, null)!!,
        name = id
    )
}

fun PublicServiceDBO.printTurtleDiff(expected: PublicServiceDBO) {
    checkIfIsomorphicAndPrintDiff(
        actual = parseRDFResponse(ungzip(turtleHarvested), JenaType.TURTLE, null)!!,
        expected = parseRDFResponse(ungzip(expected.turtleHarvested), JenaType.TURTLE, null)!!,
        name = "harvested model from ${expected.uri}"
    )
    checkIfIsomorphicAndPrintDiff(
        actual = parseRDFResponse(ungzip(turtleService), JenaType.TURTLE, null)!!,
        expected = parseRDFResponse(ungzip(expected.turtleService), JenaType.TURTLE, null)!!,
        name = "full model from ${expected.uri}"
    )
}
fun checkIfIsomorphicAndPrintDiff(actual: Model, expected: Model, name: String): Boolean {
    val isIsomorphic = actual.isIsomorphicWith(expected)

    if (!isIsomorphic) {
        val actualDiff = actual.difference(expected).createRDFResponse(JenaType.TURTLE)
        val expectedDiff = expected.difference(actual).createRDFResponse(JenaType.TURTLE)

        if (actualDiff.isNotEmpty()) {
            logger.error("non expected nodes in $name:")
            logger.error(actualDiff)
        }
        if (expectedDiff.isNotEmpty()) {
            logger.error("missing nodes in $name:")
            logger.error(expectedDiff)
        }
    }
    return isIsomorphic
}
