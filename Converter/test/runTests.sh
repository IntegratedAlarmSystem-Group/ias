#!/usr/bin/bash
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.ConverterCdbTester
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.MapperTester
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.TestKafkaStreaming
