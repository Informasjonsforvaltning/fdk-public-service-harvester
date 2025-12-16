package no.fdk.fdk_public_service_harvester.service

import no.fdk.fdk_public_service_harvester.configuration.ApplicationProperties
import no.fdk.fdk_public_service_harvester.harvester.calendarFromTimestamp
import no.fdk.fdk_public_service_harvester.harvester.extractCatalogModel
import no.fdk.fdk_public_service_harvester.model.*
import no.fdk.fdk_public_service_harvester.rdf.DCATNO
import no.fdk.fdk_public_service_harvester.rdf.containsTriple
import no.fdk.fdk_public_service_harvester.rdf.safeParseRDF
import no.fdk.fdk_public_service_harvester.rdf.safeAddProperty
import no.fdk.fdk_public_service_harvester.repository.CatalogRepository
import no.fdk.fdk_public_service_harvester.repository.PublicServicesRepository
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.springframework.stereotype.Service

@Service
class UpdateService (
    private val applicationProperties: ApplicationProperties,
    private val serviceMetaRepository: PublicServicesRepository,
    private val catalogMetaRepository: CatalogRepository,
    private val turtleService: TurtleService
) {

    fun updateMetaData() {
        catalogMetaRepository.findAll()
            .forEach { catalog ->
                val catalogNoRecords = turtleService.getCatalog(catalog.fdkId, withRecords = false)
                    ?.let { safeParseRDF(it, Lang.TURTLE) }

                if (catalogNoRecords != null) {
                    val fdkCatalogURI = "${applicationProperties.publicServiceHarvesterUri}/catalogs/${catalog.fdkId}"
                    val catalogMeta = catalog.createMetaModel()
                    val completeMetaModel = ModelFactory.createDefaultModel()
                    completeMetaModel.add(catalogMeta)

                    val catalogTriples = catalogNoRecords.getResource(catalog.uri)
                        .extractCatalogModel()
                    catalogTriples.add(catalogMeta)

                    serviceMetaRepository.findAllByIsPartOf(fdkCatalogURI)
                        .filter { it.catalogContainsService(catalog.uri, catalogNoRecords) }
                        .forEach { service ->
                            val serviceMeta = service.createMetaModel()
                            completeMetaModel.add(serviceMeta)
                            turtleService.getPublicService(service.fdkId, withRecords = false)
                                ?.let { serviceNoRecords -> safeParseRDF(serviceNoRecords, Lang.TURTLE) }
                                ?.let { serviceModelNoRecords -> serviceMeta.union(serviceModelNoRecords) }
                                ?.let { serviceModel -> catalogTriples.union(serviceModel) }
                                ?.run { turtleService.saveAsPublicService(this, fdkId = service.fdkId, withRecords = true) }
                        }

                    turtleService.saveAsCatalog(
                        completeMetaModel.union(catalogNoRecords),
                        fdkId = catalog.fdkId,
                        withRecords = true
                    )
                }
            }
    }

    private fun CatalogMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.publicServiceHarvesterUri}/catalogs/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun PublicServiceMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.publicServiceHarvesterUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .safeAddProperty(DCTerms.isPartOf, isPartOf)
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun PublicServiceMeta.catalogContainsService(catalogURI: String, catalogModel: Model): Boolean =
        catalogModel.containsTriple("<$catalogURI>", "<${DCATNO.containsService.uri}>", "<$uri>")
}
