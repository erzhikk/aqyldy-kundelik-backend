package kz.aqyldykundelik

import kz.aqyldykundelik.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class AqyldyKundelikBackendApplication

fun main(args: Array<String>) {
    runApplication<AqyldyKundelikBackendApplication>(*args)
}
