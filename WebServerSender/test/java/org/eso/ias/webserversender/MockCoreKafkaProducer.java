package org.eso.ias.webserversender;

import java.util.ArrayList;
import java.util.List;

import org.eso.ias.kafkautils.KafkaUtilsException;
import org.eso.ias.kafkautils.SimpleStringProducer;

public class MockCoreKafkaProducer {


	public static void main(String[] args) throws KafkaUtilsException {

		SimpleStringProducer producer = new SimpleStringProducer("localhost:9092", "test", "PID1");
		producer.setUp();

    String msg2 = "{\"value\":\"SET\",\"tStamp\":1600,\"mode\":\"OPERATIONAL\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\"}";
    String msg1 = "{\"value\":\"CLEARED\",\"tStamp\":1600,\"mode\":\"MAINTENANCE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\"}";

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

				producer.push(msg1, null, msg1);
				producer.flush();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			counter += 1;

		}



	}

}
