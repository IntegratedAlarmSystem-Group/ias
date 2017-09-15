package org.eso.ias.converter;

import org.eso.ias.cdb.CdbReader;
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
	public ConverterStream converterStream() { 
		return new ConverterKafkaStream(); }
	
	@Bean
	public CdbReader cdbReader() { 
		return null; }
	
}
