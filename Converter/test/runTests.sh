#!/usr/bin/bash
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.converter.test.ConverterEngineTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.converter.test.ConverterCdbTester
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.converter.test.ConverterLoopTester
