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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.naming.InvalidNameException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
}
