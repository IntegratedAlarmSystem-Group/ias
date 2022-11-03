#!/usr/bin/bash
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.ConverterCdbTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.MapperTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.TemplatedConversionTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.TestKafkaStreaming
