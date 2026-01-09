package org.eso.ias.webserversender;

import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaIasiosProducer;
import org.eso.ias.kafkautils.KafkaUtilsException;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.types.*;

public class MockCoreKafkaProducer {


	public static void main(String[] args) throws KafkaUtilsException {

		IasValueStringSerializer serializer = new IasValueJsonSerializer();

		SimpleStringProducer stringProducer = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,"PID1");

		KafkaIasiosProducer producer = new KafkaIasiosProducer(stringProducer,KafkaHelper.IASIOs_TOPIC_NAME, serializer);
		producer.setUp();

		IASValue<?> msg;
		int counter = 0;

		Alarm value1 = Alarm.getInitialAlarmState();
		Alarm value2 = Alarm.getInitialAlarmState().set();
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
