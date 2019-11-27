/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.serving.inference.logger;

import com.twitter.bijection.Injection;
import com.twitter.bijection.avro.GenericAvroCodecs;
import io.hops.hopsworks.common.dao.kafka.KafkaConst;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.serving.Serving;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class KafkaInferenceLogger implements InferenceLogger {

  private static final Logger LOGGER = Logger.getLogger(KafkaInferenceLogger.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private CertificateMaterializer certificateMaterializer;

  public static final String SERVING_MANAGER_USERNAME = "srvmanager";
  private Properties props;

  @PostConstruct
  public void init() {
    // Setup default properties
    props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, settings.getKafkaBrokersStr());
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaServing");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        ByteArraySerializer.class.getName());

  }

  @Override
  @Asynchronous
  public void logInferenceRequest(Serving serving, String inferenceRequest,
                                  Integer responseHttpCode, String inferenceResponse) {

    if (serving.getKafkaTopic() == null) {
      // nothing to log
      return;
    }

    // Setup the producer for the given project
    KafkaProducer <String, byte[]> kafkaProducer = null;
    try {
      kafkaProducer = setupProducer(serving.getProject());
    } catch (IOException | CryptoPasswordNotFoundException e) {
      LOGGER.log(Level.FINE, "Failed to setup the produce for the project: "
          + serving.getProject().getName() , e);
      // We didn't manage to write the log to Kafka, nothing we can do.
    }
    
    //Get the schema for the topic and the serializer
    Schema avroSchema = new Schema.Parser().parse(serving.getKafkaTopic().getSchemaTopics().getContents());
    Injection<GenericRecord, byte[]> recordSerializer = GenericAvroCodecs.toBinary(avroSchema);
  
    //Get the version of the schema
    int schemaVersion = serving.getKafkaTopic().getSchemaTopics().getSchemaTopicsPK().getVersion();
    
    // Create the GenericRecord from the avroSchema
    GenericData.Record inferenceRecord = new GenericData.Record(avroSchema);
  
    // Populate the Inference Record with data
    populateInfererenceRecord(serving, inferenceRequest, responseHttpCode, inferenceResponse, inferenceRecord,
      schemaVersion);

    // Serialize record to byte
    byte[] inferenceRecordBytes = recordSerializer.apply(inferenceRecord);

    // Push the record to the topic
    ProducerRecord<String, byte[]> inferenceKakfaRecord = new ProducerRecord<>(
        serving.getKafkaTopic().getTopicName(), inferenceRecordBytes);

    try {
      kafkaProducer.send(inferenceKakfaRecord);
    } catch (Exception e) {
      LOGGER.log(Level.FINE, "Cannot write to topic: " + serving.getKafkaTopic().getTopicName(), e);
      // We didn't manage to write the log to Kafka, nothing we can do.
    } finally {
      if(kafkaProducer != null) {
        kafkaProducer.flush();
        kafkaProducer.close();
      }
    }

    // De-materialize certificate
    certificateMaterializer.removeCertificatesLocal(SERVING_MANAGER_USERNAME, serving.getProject().getName());
  }
  
  /**
   * Specify inference record based on the schema version. To not break backwards-compatibility, changes to the
   * inference schema (addition or removal of fields) should be made in new versions of the schema. This method will
   * populate the inference record with the right data based on the version of the inference schema.
   *
   * @param serving serving creating the inference
   * @param inferenceRequest inferenceRequest provided by the client
   * @param responseHttpCode http response code by the serving
   * @param inferenceResponse http response by the serving
   * @param inferenceRecord kafka inference record to populate
   * @param schemaVersion version of the inference schema
   */
  private void populateInfererenceRecord(Serving serving, String inferenceRequest, Integer responseHttpCode,
    String inferenceResponse, GenericData.Record inferenceRecord, int schemaVersion){
    if(schemaVersion == 1) {
      inferenceRecord.put("modelId", serving.getId());
      inferenceRecord.put("modelName", serving.getName());
      inferenceRecord.put("modelVersion", serving.getVersion());
      inferenceRecord.put("requestTimestamp", System.currentTimeMillis());
      inferenceRecord.put("responseHttpCode", responseHttpCode);
      inferenceRecord.put("inferenceRequest", inferenceRequest);
      inferenceRecord.put("inferenceResponse", inferenceResponse);
    }
    if(schemaVersion == 2){
      inferenceRecord.put("modelId", serving.getId());
      inferenceRecord.put("modelName", serving.getName());
      inferenceRecord.put("modelVersion", serving.getVersion());
      inferenceRecord.put("requestTimestamp", System.currentTimeMillis());
      inferenceRecord.put("responseHttpCode", responseHttpCode);
      inferenceRecord.put("inferenceRequest", inferenceRequest);
      inferenceRecord.put("inferenceResponse", inferenceResponse);
      inferenceRecord.put("servingType", serving.getServingType().name());
    }
  }


  private KafkaProducer<String, byte[]> setupProducer(Project project) throws IOException,
    CryptoPasswordNotFoundException {
    certificateMaterializer.materializeCertificatesLocal(SERVING_MANAGER_USERNAME, project.getName());
    CertificateMaterializer.CryptoMaterial cryptoMaterial =
        certificateMaterializer.getUserMaterial(SERVING_MANAGER_USERNAME, project.getName());

    // Configure TLS for this producer
    props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, KafkaConst.KAFKA_SECURITY_PROTOCOL);
    props.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
      KafkaConst.KAFKA_ENDPOINT_IDENTIFICATION_ALGORITHM);
    props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
        settings.getHopsworksTmpCertDir() + File.separator + HopsUtils.getProjectTruststoreName(project.getName(),
            SERVING_MANAGER_USERNAME));
    props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, String.valueOf(cryptoMaterial.getPassword()));

    props.setProperty(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
        settings.getHopsworksTmpCertDir() + File.separator + HopsUtils.getProjectKeystoreName(project.getName(),
            SERVING_MANAGER_USERNAME));
    props.setProperty(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, String.valueOf(cryptoMaterial.getPassword()));

    props.setProperty(SslConfigs.SSL_KEY_PASSWORD_CONFIG, String.valueOf(cryptoMaterial.getPassword()));

    return new KafkaProducer<>(props);
  }

  @Override
  public String getClassName() {
    return KafkaInferenceLogger.class.getName();
  }

}
