/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.ca.controllers;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.naming.InvalidNameException;
import java.io.IOException;
import java.security.KeyPair;
import java.security.Security;

public class TestPKIUtils {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testParseHostCertificateSubject() throws Exception {
    PKIUtils utils = new PKIUtils();
    X500Name name = utils.parseHostCertificateSubjectName("ip-172-31-22-209.us-east-2.compute.internal__consul__1");
    Assert.assertEquals("CN=ip-172-31-22-209.us-east-2.compute.internal,L=consul,OU=1", name.toString());
    name = utils.parseHostCertificateSubjectName("hopsworks.glassfish.service.consul__glassfish__0");
    Assert.assertEquals("CN=hopsworks.glassfish.service.consul,L=glassfish,OU=0", name.toString());

    thrown.expect(InvalidNameException.class);
    utils.parseHostCertificateSubjectName("test0__onlinefs");
  }

  @Test
  public void testParseApplicationCertificateSubject() throws Exception {
    PKIUtils utils = new PKIUtils();
    X500Name name = utils.parseApplicationCertificateSubjectName(
        "ProjectName__UserName__application_1586380949751_0855__0");
    Assert.assertEquals("CN=ProjectName__UserName,O=application_1586380949751_0855,OU=0", name.toString());

    thrown.expect(InvalidNameException.class);
    utils.parseApplicationCertificateSubjectName("ProjectName__UserName__1586380949751_0855__0");
  }

  @Test
  public void testParseGenericCertificateSubject() throws Exception {
    PKIUtils utils = new PKIUtils();
    X500Name name = utils.parseGenericCertificateSubjectName("ProjectName__Username");
    Assert.assertEquals("CN=ProjectName__Username", name.toString());

    thrown.expect(InvalidNameException.class);
    utils.parseGenericCertificateSubjectName("");
  }

  @Test
  public void testGetCertificateValidityInS() {
    PKIUtils pkiUtils = new PKIUtils();
    Assert.assertEquals(10, pkiUtils.getCertificateValidityInS("10s"));
    Assert.assertEquals(60*2, pkiUtils.getCertificateValidityInS("2m"));
    Assert.assertEquals(60*60*30, pkiUtils.getCertificateValidityInS("30h"));
    Assert.assertEquals(60*60*24*2, pkiUtils.getCertificateValidityInS("2d"));
  }

  @Test
  public void testLoadPrivateKey() throws IOException, Exception {
    Security.addProvider(new BouncyCastleProvider());
    String privateKey = "-----BEGIN PRIVATE KEY-----\n" +
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC0b91ohTVHecKA\n" +
        "o2U1e3DFsS6RyR8oRiVLHBRFv2SMI+/GNxB1mAUAEE4V7T0vNIv76fV54NRh3MBY\n" +
        "4cUDiWYtFCcxV1acHtCP8ppix+74dO3ZeoQ6ZklwgVLGUHcsicRpSegiZzEn840H\n" +
        "KCNSE7+Kyql7JgIZD4G4vWN3v0taIMhsTEK/iKs5gGVRNPQgu9j2CA07BqoplHde\n" +
        "412D8gJCEXhs0ZXq3L4OrV1HfmGiqqU/7fauTGnKmVp2AHpwHrzW8Qx+jc8d33Bl\n" +
        "OxwbmptjoG42uTQsrX/HKBddQ4RI3MbHZIVmUkVtQsXsAYD3/4IE8UI50TY3qEC2\n" +
        "2bLBpuTxAgMBAAECggEAWThvTL2BiORGLwGcjAOL0dU459GBXJLC4g7yX0KyXzFt\n" +
        "4J9dvif7YPqvAdybQnpDNb+MKEXp/rH/UnPgzUzlfyjV8Gn1Y6FBE8ysVmfyXFzF\n" +
        "N6KDO7VUXxMzcOhc5WMCAeYPqONJxS2C8KUCQhWNwv1PLJuwsd+fD1BmnNG0Ws3C\n" +
        "NRmEkBp9RfoHTmSAV3+c4DmllAfoXVNA/KkAfPGNTZGcVm0FikWb2uC91sBD7aHG\n" +
        "w8FUi4VvniQeKaksfhCucJoA/cl8h7imEl3sVvbImhhkH7EtEET/4HNXsAacBRdL\n" +
        "juFjTapvI5G/zaxqdKlzw5+2ycEuFtM5cMZztzoYAQKBgQDL5kl3x0XqSCnLRclx\n" +
        "W7wU1J7ZVzbHoohujL7X62q7CakJ45byOQKtjRJyYRYE5zgjdzL8ZgO/ieog2n5C\n" +
        "tpaMP/0jY8r33HfdHV/KhA6VSiNmqu/xj0Puq14xEvJ7jVImvlhH8gSa7/EXXnNg\n" +
        "udvc5GMEEHrGKlPzMGB0Kl8wwQKBgQDiitDuoeBX30DZ91snCZHIVZ/SwcCWeX53\n" +
        "1EKIK+wLtd1pjxR3zyagyKqyFsDht8Cl/AvqljEJJegFoPLuLxJCDccTHf8I20Rc\n" +
        "GcT9Bpvfy2YyiTNZIjUAJu764dDmQqYYbS2HOtevNqwbivJPUR1+4j8Ra96poCTi\n" +
        "5OiIWIKQMQKBgHhnaILenZ6XNnbeovHZpdr3I0ZchfClPcNqQVfnoIMKVVONnZkz\n" +
        "qS0q3PXF9ua2UyQ+Q1FgPF5i5mq4G07x2zy+nJDFYRm0iuN7cRF5odLukLETx9Tx\n" +
        "MMBDWb/I3H+xGA3g4Oi7NZT4k3mlQKShm/94ri+8O4PBgwlcS9jNHKEBAoGBAI8N\n" +
        "aXHG9ouGhsUc1YqJGG2Q5COKBbr/bUTt3DVwxtV+Ohp2J06gmJvfGyrqA1KFXjly\n" +
        "N3Qi80P7k9A6Gi0dvEHJwXPo9Sr6iug9vY6ppbRkFFzFFo+qch1ueGokPm2omInE\n" +
        "J4PFPH1/4J5j1y8O4blF1N2DaE9kuOYt9khi28+BAoGAdhUtA0YQnENzjQKF7Hyf\n" +
        "tLvvm/em/eb974KzT/IC0kVHGbgJEOdhJ0nXI/oRk3+9ojmLjqAVMLMjK05j4gLp\n" +
        "jBbcHA/zPwv5C+Pe9u5iiFy1lV6Oridlm4hFcJnAM67y4/9aKVFBlJdTcAa0lGHU\n" +
        "MO3BxsWG2mgkPn63/mSfbAk=\n" +
        "-----END PRIVATE KEY-----";

    PKIUtils pkiUtils = new PKIUtils();
    pkiUtils.init();
    KeyPair keyPair = pkiUtils.loadKeyPair(privateKey, "");
    // The key above contains only the private key
    Assert.assertNull(keyPair.getPublic());
    Assert.assertNotNull(keyPair.getPrivate());
    Assert.assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
  }
}
