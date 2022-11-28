package org.jembi.jempi
package configuration

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}

import java.nio.file.Paths

object Main  {


  @main def configure(config_file_name: String): Any =
    println(s"name =  ${config_file_name}")

    if (config_file_name.isBlank) {
      println("Dude: you need to specify a config file name")
      System.exit(0)
    }

    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build() :: ClassTagExtensions
    val config = mapper.readValue(Paths.get(config_file_name).toFile, new TypeReference[Config] {})

    CustomEntity.generateEntity(config.fields)
    CustomEntity.generateGoldenRecord(config.fields)
    CustomMU.generate(config.fields)
    CustomLibMPIConstants.generate(config.fields)
    CustomLibMPIDGraphEntity.generate(config.fields)
    CustomLibMPIGoldenRecord.generate(config.fields)
    CustomLibMPIExpandedGoldenRecord.generate(config.fields)
    CustomLibMPIMutations.generate(config.fields)
    CustomLibMPIQueries.parseRules(config)
    CustomLinkerDeterministic.parseRules(config)
    CustomLinkerProbalistic.parseRules(config)
    CustomLinkerBackEnd.parseRules(config)
    CustomLinkerMU.parseRules(config)

}
