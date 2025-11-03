#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.converter.test.ConverterCdbTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.converter.test.MapperTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.converter.test.TemplatedConversionTest
