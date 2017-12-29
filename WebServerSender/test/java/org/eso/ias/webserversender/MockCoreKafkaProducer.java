package org.eso.ias.webserversender;

import java.util.ArrayList;
import java.util.List;

import org.eso.ias.kafkautils.KafkaUtilsException;
import org.eso.ias.kafkautils.SimpleStringProducer;

public class MockCoreKafkaProducer {


	public static void main(String[] args) throws KafkaUtilsException {

		SimpleStringProducer producer = new SimpleStringProducer("localhost:9092", "test", "PID1");
		producer.setUp();

		String msg1 = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
		String msg2 = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";

		String aux;

		int counter = 0;

		while (true) {

			try {

				Thread.sleep(100);

				if (counter == 10) {

					aux = msg1;
					msg1 = msg2;
					msg2 = aux;

					counter = 0;
				}
				String msg = msg1 + Long.toString(System.currentTimeMillis()) + "}";
				producer.push(msg, null, msg);
				producer.flush();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			counter += 1;

		}



	}

}
