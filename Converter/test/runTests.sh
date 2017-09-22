#!/usr/bin/bash
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.converter.test.ConverterCdbTester
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.converter.test.MapperTester
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.converter.test.TestKafkaStreaming