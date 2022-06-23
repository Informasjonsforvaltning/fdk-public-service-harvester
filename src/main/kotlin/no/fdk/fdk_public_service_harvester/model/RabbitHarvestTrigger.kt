package no.fdk.fdk_public_service_harvester.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class RabbitHarvestTrigger(
    val dataSourceId: String? = null,
    val publisherId: String? = null,
    val dataSourceType: String? = null,
    val forceUpdate: Boolean = false
)
