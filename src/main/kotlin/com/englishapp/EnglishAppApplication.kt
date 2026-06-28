package com.englishapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EnglishAppApplication

fun main(args: Array<String>) {
    runApplication<EnglishAppApplication>(*args)
}
