from kafka import KafkaConsumer, TopicPartition
import threading
import time
import json


class SimpleStringConsumer:

    def __init__(self, servers, topic_name, listener):
        self.KAFKA_CONSUMER_GROUP = "test"
        self.POLLING_TIMEOUT = 60000

        self.kafka_servers = servers
        if(topic_name == ""):
            raise Exception('Invalid empty topic name')
        self.topic_name = topic_name
        self.listener = listener

        self.thread = None
        self.consumer = None
        self.is_initialized = False
        self.is_closed = False
        self.processed_records = 0
        self.processed_strings = 0

        print('Simple Kafka Consumer will get events from %s topic connected \
        to kafka broker %s'.format(self.topic_name, self.kafka_servers))

    def _set_up(self, properties):
        if(self.is_initialized):
            raise Exception('Already initialized!')      
        # TODO: merge properties with default properties
        self.consumer = KafkaConsumer(**properties) 
        # TODO: Init tear down thread and shutdown hooK
        self.is_initialized = True
        print('Kafka Consumer Initialized')

    def set_up(self, properties=None):
        if(properties is None):
            properties = self.get_default_properties()
        self._set_up(properties)

    def start_getting_events(self):
        if(not self.is_initialized):
            raise Exception('Not initialized')
        if(self.thread is not None):
            print('Cannot start receiving: already receiving events')
        self.thread = threading.Thread(target=self.run)
        self.thread.daemon = True
        self.thread.start()
        time.sleep(60)

    def run(self):
        print('Thread to get events from the topic started')
        self.consumer.subscribe([self.topic_name])

        print('Start polling loop')
        while(not self.is_closed):
            try:
                # import pdb; pdb.set_trace()
                records = self.consumer.poll(self.POLLING_TIMEOUT)
                print('Item read with {} records', len(records))
                self.processed_records += 1
                
            except:
                print('Fail on try to poll the consumer')
                continue
            try:
                # import pdb; pdb.set_trace()
                # print(records)
                for key, record in records:
                    print('************************')
                    print(key)
                    print(record)
                    self.processed_strings += 1
                    self.listener.string_events_received(record.value)
                    # print(record.value)
            except:
                print('Exception, to get events from the topic terminated')
                continue

    def stop_getting_events(self):
        if(self.thread is None):
            print('Cannot stop receiving: I am not receiving events')
        try:
            self.thread.join(60000)
            if(self.thread.isAlive()):
                print('The thread to get events did not exit')
        except:
            exit(0)
        self.thread = None

    def get_default_properties(self):
        properties = {}
        properties['group_id'] = self.KAFKA_CONSUMER_GROUP
        properties['bootstrap_servers'] = self.kafka_servers
        properties['enable_auto_commit'] = True
        properties['auto_commit_interval_ms'] = 1000
        # properties['key_deserializer'] = str.decode()
        # properties['value_deserializer'] = lambda v: json.loads(v)
        properties['auto_offset_reset'] = 'latest'

        return properties
