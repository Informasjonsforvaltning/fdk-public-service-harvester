package no.fdk.fdk_public_service_harvester.model

import no.fdk.fdk_public_service_harvester.rdf.safeParseRDF
import no.fdk.fdk_public_service_harvester.service.ungzip
import org.apache.jena.riot.Lang
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "serviceMeta")
data class PublicServiceMeta (
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val isPartOf: String? = null,
    val removed: Boolean = false,

    val issued: Long,
    val modified: Long
)

@Document(collection = "catalogMeta")
data class CatalogMeta(
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val services: Set<String>,
    val issued: Long,
    val modified: Long
)

@Document(collection = "harvestSourceTurtle")
data class HarvestSourceTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

@Document(collection = "serviceTurtle")
data class PublicServiceTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

@Document(collection = "fdkServiceTurtle")
data class FDKPublicServiceTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

@Document(collection = "catalogTurtle")
data class CatalogTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

@Document(collection = "fdkCatalogTurtle")
data class FDKCatalogTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

abstract class TurtleDBO {
    abstract val id: String
    abstract val turtle: String
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TurtleDBO

        return when {
            id != other.id -> false
            else -> zippedModelsAreIsomorphic(turtle, other.turtle)
        }

    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + turtle.hashCode()
        return result
    }
}

private fun zippedModelsAreIsomorphic(zip0: String, zip1: String): Boolean {
    val model0 = safeParseRDF(ungzip(zip0), Lang.TURTLE)
    val model1 = safeParseRDF(ungzip(zip1), Lang.TURTLE)

    return model0.isIsomorphicWith(model1)
}
