package org.eso.ias.webserversender;

import org.eso.ias.kafkautils.KafkaIasiosProducer;
import org.eso.ias.kafkautils.KafkaUtilsException;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.types.Alarm;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueStringSerializer;
import org.eso.ias.types.OperationalMode;

public class MockCoreKafkaProducer {


	public static void main(String[] args) throws KafkaUtilsException {

		IasValueStringSerializer serializer = new IasValueJsonSerializer();

		KafkaIasiosProducer producer = new KafkaIasiosProducer("localhost:9092", KafkaHelper.IASIOs_TOPIC_NAME, "PID1", serializer);
		producer.setUp();

		IASValue<?> msg;
		int counter = 0;

		Alarm value1 = Alarm.CLEARED;
		Alarm value2 = Alarm.SET_MEDIUM;
		Alarm aux;

		while (true) {

			try {
				Thread.sleep(100);

				if (counter == 10) {
					aux = value1;
					value1 = value2;
					value2 = aux;
					counter = 0;
				}
				msg = IASValue.build(
						value1,
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
