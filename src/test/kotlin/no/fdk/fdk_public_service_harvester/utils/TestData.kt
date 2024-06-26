package no.fdk.fdk_public_service_harvester.utils

import no.fdk.fdk_public_service_harvester.model.HarvestDataSource
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap
import java.util.*

const val LOCAL_SERVER_PORT = 5050

const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_PORT = 27017
const val MONGO_COLLECTION = "publicServiceHarvester"

val MONGO_ENV_VALUES: Map<String, String> = ImmutableMap.of(
    "MONGO_INITDB_ROOT_USERNAME", MONGO_USER,
    "MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD
)

const val SERVICE_ID_0 = "d5d0c07c-c14f-3741-9aa3-126960958cf0"
const val SERVICE_ID_1 = "6ce4e524-3226-3591-ad99-c026705d4259"
const val SERVICE_ID_2 = "31249174-df02-3746-9d61-59fc61b4c5f9"
const val SERVICE_ID_3 = "1fc38c3c-1c86-3161-a9a7-e443fd94d413"
const val SERVICE_ID_4 = "ef4ca382-ee65-3a92-be9e-40fd93da53bc"
const val SERVICE_ID_5 = "7baa248b-1a27-3c46-80cf-889882d6b894"

const val CATALOG_ID_0 = "e09277f3-1eec-3ab9-a979-79259736d768"
const val CATALOG_ID_1 = "b7c6d34c-624d-3c72-9e30-2b608e433ad7"

val TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 9, 5).setTimeOfDay(13, 15, 39, 831).build()
val NEW_TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 9, 15).setTimeOfDay(11, 52, 16, 122).build()

val TEST_HARVEST_SOURCE = HarvestDataSource(
    id = "test-source",
    url = "http://localhost:5050/fdk-public-service-publisher.ttl",
    acceptHeaderValue = "text/turtle",
    dataType = "publicService",
    dataSourceType = "CPSV-AP-NO"
)
