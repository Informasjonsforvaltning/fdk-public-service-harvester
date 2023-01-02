package no.fdk.fdk_public_service_harvester.repository

import no.fdk.fdk_public_service_harvester.model.HarvestSourceTurtle
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface HarvestSourceTurtleRepository : MongoRepository<HarvestSourceTurtle, String>
