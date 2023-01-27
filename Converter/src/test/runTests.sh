#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.ConverterCdbTest
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.MapperTest
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.TemplatedConversionTest
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.converter.test.TestKafkaStreaming
