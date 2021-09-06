package no.fdk.fdk_public_service_harvester.harvester

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.fdk.fdk_public_service_harvester.adapter.HarvestAdminAdapter
import no.fdk.fdk_public_service_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_public_service_harvester.service.UpdateService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)
private const val HARVEST_ALL_ID = "all"

@Service
class HarvesterActivity(
    private val harvestAdminAdapter: HarvestAdminAdapter,
    private val harvester: PublicServicesHarvester,
    private val publisher: RabbitMQPublisher,
    private val updateService: UpdateService
): CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val activitySemaphore = Semaphore(1)
    private val harvestSemaphore = Semaphore(5)

    @EventListener
    fun fullHarvestOnStartup(event: ApplicationReadyEvent) = initiateHarvest(null)

    fun initiateHarvest(params: Map<String, String>?) {
        if (params == null || params.isEmpty()) LOGGER.debug("starting harvest of all services")
        else LOGGER.debug("starting harvest with parameters $params")

        val harvest = launch {
            activitySemaphore.withPermit {
                harvestAdminAdapter.getDataSources(params)
                    .filter { it.dataType == "publicService" }
                    .filter { it.url != null }
                    .forEach {
                        launch {
                            harvestSemaphore.withPermit {
                                try {
                                    harvester.harvestServices(it, Calendar.getInstance())
                                } catch (exception: Exception) {
                                    LOGGER.error("Harvest of ${it.url} failed", exception)
                                }
                            }
                        }
                    }
            }
        }

        harvest.invokeOnCompletion {
            updateService.updateUnionModel()

            if (params == null || params.isEmpty()) LOGGER.debug("completed harvest of all services")
            else LOGGER.debug("completed harvest with parameters $params")

            publisher.send(HARVEST_ALL_ID)
        }

    }
}
