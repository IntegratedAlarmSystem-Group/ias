package org.eso.ias.webserversender;

import java.util.ArrayList;
import java.util.List;

import org.eso.ias.kafkautils.KafkaUtilsException;
import org.eso.ias.kafkautils.SimpleStringProducer;

public class CoreKafkaProducer {


	public static void main(String[] args) throws KafkaUtilsException {

		SimpleStringProducer producer = new SimpleStringProducer("localhost:9092", "test", "PID1");
		producer.setUp();


//		// nrOfStrings supported of first tests under this structure to webserver: 100
//
//		int nrOfStrings = 100;
//		List<String> strsToSend = new ArrayList<>(nrOfStrings);
//		for (int t=0; t<nrOfStrings; t++) {
//			// strsToSend.add("{\"value\":\"false\",\"tStamp\":1600,\"mode\":\"OPERATIONAL\",\"id\":\"BooleanType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(BooleanType-ID:IASIO)\",\"valueType\":\"BOOLEAN\"}");
//			strsToSend.add("{\"value\":\"false\",\"tStamp\":1600,\"mode\":\"MAINTENANCE\",\"id\":\"BooleanType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(BooleanType-ID:IASIO)\",\"valueType\":\"BOOLEAN\"}");
//		}
//		for (String str: strsToSend) {
//			producer.push(str, null, str);
//		}
//		producer.flush();
//

//		// periodic changes

		String msg2 = "{\"value\":\"true\",\"tStamp\":1600,\"mode\":\"OPERATIONAL\",\"id\":\"BooleanType-IDD\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(BooleanType-ID:IASIO)\",\"valueType\":\"BOOLEAN\"}";
		String msg1 = "{\"value\":\"false\",\"tStamp\":1600,\"mode\":\"MAINTENANCE\",\"id\":\"BooleanType-IDD\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(BooleanType-ID:IASIO)\",\"valueType\":\"BOOLEAN\"}";

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
