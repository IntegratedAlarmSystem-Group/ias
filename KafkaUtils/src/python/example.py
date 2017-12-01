from simpleStringConsumer import SimpleStringConsumer
import sys


class KafkaConsumerListener:

    def __init__(self):
        self.name = 'Listener'

    def stringEventReceived(self, event):
        print(event)


consumer = SimpleStringConsumer(servers='localhost:9092', topic_name='test',
                                listener=KafkaConsumerListener())
consumer.set_up()
consumer.start_getting_events()
