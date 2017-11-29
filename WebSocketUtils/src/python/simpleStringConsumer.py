from kafka import KafkaConsumer

class SimpleStringConsumer(object):

    KAFKA_CONSUMER_GROUP = "test" 
    POLLING_TIMEOUT = 60000
    WAIT_FOR_PARTITIONS_TIMEOUT = 3
    processedRecords = 0
    processedStrings = 0

    consumer = None
    topicName = None
    kafkaServers = None




