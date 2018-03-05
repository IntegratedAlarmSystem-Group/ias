package org.eso.ias.webserversender;

import org.eso.ias.kafkautils.KafkaIasiosProducer;
import org.eso.ias.kafkautils.KafkaUtilsException;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.prototype.input.java.AlarmSample;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasValidity;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.eso.ias.prototype.input.java.IasValueStringSerializer;
import org.eso.ias.prototype.input.java.OperationalMode;

public class MockCoreKafkaProducer {


	public static void main(String[] args) throws KafkaUtilsException {

		IasValueStringSerializer serializer = new IasValueJsonSerializer();

		KafkaIasiosProducer producer = new KafkaIasiosProducer("localhost:9092", KafkaHelper.IASIOs_TOPIC_NAME, "PID1", serializer);
		producer.setUp();

		IASValue<?> msg;
		int counter = 0;

		AlarmSample value1 = AlarmSample.CLEARED;
		AlarmSample value2 = AlarmSample.SET;
		AlarmSample aux;

		while (true) {

			try {
				Thread.sleep(100);

				if (counter == 10) {
					aux = value1;
					value1 = value2;
					value2 = aux;
					counter = 0;
				}
				msg = IASValue.buildIasValue(
						value1,
						System.currentTimeMillis(),
						OperationalMode.OPERATIONAL,
						IasValidity.RELIABLE,
						"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)",
						IASTypes.ALARM
					);
				producer.push(msg);
				counter += 1;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}
