package com.koalagroup.koala

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
class KoalaApplication

fun main(args: Array<String>) {
	runApplication<KoalaApplication>(*args)
}
