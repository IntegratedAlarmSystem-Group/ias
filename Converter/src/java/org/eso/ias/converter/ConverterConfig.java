package org.eso.ias.converter;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.converter.corepublisher.CoreFeeder;
import org.eso.ias.converter.pluginconsumer.RawDataReader;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.eso.ias.prototype.input.java.IasValueStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link Converter} configuration with spring annotations.
 * 
 * @author acaproni
 *
 */
@Configuration
public class ConverterConfig {

	@Bean
	public RawDataReader rawDataReader() { 
		return null; }
	
	@Bean
	public CdbReader cdbReader() { 
		return null; }
	
	@Bean
	public CoreFeeder coreFeeder() { 
		return null; }
	
	@Bean
	public IasValueStringSerializer iasValueStringSerialize() {
		return new IasValueJsonSerializer();
	}
}
