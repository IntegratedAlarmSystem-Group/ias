package org.eso.ias.converter;

import java.util.Optional;
import java.util.Properties;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.rdb.RdbReader;
import org.eso.ias.heartbeat.HbMsgSerializer;
import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.heartbeat.publisher.HbKafkaProducer;
import org.eso.ias.heartbeat.serializer.HbJsonSerializer;
import org.eso.ias.kafkautils.KafkaHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * {@link Converter} configuration with spring annotations.
 * 
 * @author acaproni
 *
 */
@Configuration
public class ConverterConfig {
	
	@Bean
	public CdbReader cdbReader() { 
		return new RdbReader(); }
	
	@Bean
	@Lazy(value = true)
	public ConverterStream converterStream(String cID, Optional<String> kafkaBrokers, Properties props) {
		return new ConverterKafkaStream(cID, kafkaBrokers, props);
	}
	
	@Bean
	@Lazy(value = true)
	public HbProducer hbProducer(String cID, Optional<String> kafkaBrokers, Properties props) { 
		HbMsgSerializer hbSerializer = new HbJsonSerializer();
		
		String kafkaServers;
		Optional<String> brokersFromProperties = Optional.ofNullable(props.getProperty(ConverterKafkaStream.KAFKA_SERVERS_PROP_NAME));
		if (brokersFromProperties.isPresent()) {
			kafkaServers = brokersFromProperties.get();
		} else if (kafkaBrokers.isPresent()) {
			kafkaServers = kafkaBrokers.get();
		} else {
			kafkaServers = KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS;
		}
		
		return new HbKafkaProducer(cID, kafkaServers, hbSerializer); 
	}
}
