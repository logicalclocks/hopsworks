/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.util;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import fish.payara.nucleus.cluster.PayaraCluster;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Iterator;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PayaraClusterManager {
  private static final Logger LOGGER = Logger.getLogger(PayaraClusterManager.class.getName());
  private static final String DAS_NAME = "server";
  @Inject
  private HazelcastInstance hazelcastInstance;
  private PayaraCluster cluster;
  
  private static final int DEFAULT_DAS_PORT = 4900;
  
  @PostConstruct
  public void init() {
    cluster = HK2Lookups.getCluster();
    Optional<String> memberName = getMemberName();
    Optional<String> memberGroup = getMemberGroup();
    LOGGER.log(Level.INFO, "This Member: {0}, MemberGroup: {1}, Is primary: {2}",
      new Object[]{memberName.orElse("Unknown"), memberGroup.orElse("Unknown"), amIThePrimary()});
  }
  
  public boolean isDASInstance(Member member) {
    return cluster != null ? DAS_NAME.equals(cluster.getMemberName(member.getUuid())) :
      member.getAddress().getPort() == DEFAULT_DAS_PORT;// a fallback if cluster is null. Should not happen.
  }
  
  public Optional<String> getMemberName() {
    return cluster != null && cluster.getUnderlyingHazelcastService().isEnabled() ?
      Optional.ofNullable(cluster.getUnderlyingHazelcastService().getMemberName()) : Optional.empty();
  }
  
  public String getMemberName(Member member) {
    return cluster != null ? cluster.getMemberName(member.getUuid()) : member.toString();
  }
  
  public Optional<String> getMemberGroup() {
    return cluster != null && cluster.getUnderlyingHazelcastService().isEnabled() ?
      Optional.ofNullable(cluster.getUnderlyingHazelcastService().getMemberGroup()) : Optional.empty();
  }
  
  public boolean amIThePrimary() {
    try {
      if (hazelcastInstance == null || hazelcastInstance.getCluster().getMembers().size() < 2) {
        return true;
      }
      Iterator<Member> iterator = hazelcastInstance.getCluster().getMembers().iterator();
      Member oldestMember = iterator.next();
      if (isDASInstance(oldestMember) && iterator.hasNext()) {
        oldestMember = iterator.next();
        LOGGER.log(Level.FINE, "Oldest Member was DAS getting next member. Primary: {0}", getMemberName(oldestMember));
      }
      return oldestMember.localMember();
    } catch (Exception ex) {
      // To not expunge timers
      LOGGER.log(Level.SEVERE, "Error checking if node is primary. ", ex);
    }
    return true;
  }
}
