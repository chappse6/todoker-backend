package com.todoker.todokerbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TodokerBackendApplication

fun main(args: Array<String>) {
    runApplication<TodokerBackendApplication>(*args)
}
