#!/usr/bin/bash
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.ConverterCdbTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.MapperTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.TemplatedConversionTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.TestKafkaStreaming
