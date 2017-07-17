import com.huddle.integration.hoopla.GameSessionHandlingServiceProtocol.HooplaIntegration.{MetricResource, MetricValue, MetricUser}
import org.json4s.{DefaultFormats, Formats, ShortTypeHints}
import org.json4s.native.Serialization

import org.json4s.JsonDSL._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport

import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.ZoneOffset

val metric = MetricResource("https://api.hoopla.net/metrics/c2eac1f5-ddd3-4696-a989-a5861d8f4a65")

val metricUser = MetricUser("user","https://api.hoopla.net/metrics/c2eac1f5-ddd3-4696-a989-a5861d8f4a65")

val metricResource = MetricValue(
  "c2eac1f5-ddd3-4696-a989-a5861d8f4a65",
  "76927a77-3f0f-4def-be8c-213168165eb5",
  metric,
  metricUser,
  8,
  "2017-07-09T11:45:24Z"
)
val json =parse("""



println(metricResource.toJSON)