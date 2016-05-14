/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

/**
 *
 * @author misdess
 */

import kafka.utils.ShutdownableThread;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import se.kth.bbc.jobs.yarn.YarnRunner;

public class HopsKafkaConsumer extends ShutdownableThread {
    private static final Logger logger = Logger.getLogger(HopsKafkaConsumer.class.getName());
    
    private final KafkaConsumer<Integer, String> consumer;
    private final String topic;

    public HopsKafkaConsumer(String topic) {
        super("KafkaConsumerExample", false);
        
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "10.0.2.15:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "DemoConsumer");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.IntegerDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        
        //configure the ssl parameters
//        props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
//        props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/var/private/client/kafka.client.truststore.jks");
//        props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "kafka1");
//        props.setProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "kafka1");
//        props.setProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "/var/private/client/kafka.client.keystore.jks");
//        props.setProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "kafka1");
//        
        

        consumer = new KafkaConsumer<>(props);
        this.topic = topic;
    }

    @Override
    public void doWork() {
        consumer.subscribe(Collections.singletonList(this.topic));
        ConsumerRecords<Integer, String> records = consumer.poll(1000);
        for (ConsumerRecord<Integer, String> record : records) {
             logger.log(Level.INFO, "Received message: {0}", record.value()); 
        }
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }
}