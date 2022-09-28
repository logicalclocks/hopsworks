/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.alert.receiver;

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.AlertManager;
import io.hops.hopsworks.alert.AlertManagerConfiguration;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.alert.dto.ReceiverName;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.EmailConfig;
import io.hops.hopsworks.alerting.config.dto.OpsgenieConfig;
import io.hops.hopsworks.alerting.config.dto.PagerdutyConfig;
import io.hops.hopsworks.alerting.config.dto.PushoverConfig;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.SlackConfig;
import io.hops.hopsworks.alerting.config.dto.VictoropsConfig;
import io.hops.hopsworks.alerting.config.dto.WebhookConfig;
import io.hops.hopsworks.alerting.config.dto.WechatConfig;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ReceiverBuilder {

  private static final Logger LOGGER = Logger.getLogger(ReceiverResource.class.getName());

  @EJB
  private AlertManager alertManager;
  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;

  public ReceiverDTO uri(ReceiverDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    return dto;
  }

  public ReceiverDTO uri(ReceiverDTO dto, UriInfo uriInfo, ReceiverName receiver) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .path(receiver.getName())
        .build());
    return dto;
  }

  public ReceiverDTO expand(ReceiverDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.RECEIVERS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public ReceiverDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, ReceiverName receiver) {
    ReceiverDTO dto = new ReceiverDTO();
    uri(dto, uriInfo, receiver);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setName(receiver.getName());
    }
    return dto;
  }

  public ReceiverDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Receiver receiver) {
    ReceiverDTO dto = new ReceiverDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setName(receiver.getName());
      dto.setEmailConfigs(receiver.getEmailConfigs());
      dto.setSlackConfigs(receiver.getSlackConfigs());
      dto.setPagerdutyConfigs(receiver.getPagerdutyConfigs());
      dto.setOpsgenieConfigs(receiver.getOpsgenieConfigs());
      dto.setPushoverConfigs(receiver.getPushoverConfigs());
      dto.setVictoropsConfigs(receiver.getVictoropsConfigs());
      dto.setWebhookConfigs(receiver.getWebhookConfigs());
      dto.setWechatConfigs(receiver.getWechatConfigs());
    }
    return dto;
  }

  /**
   * Build a single Receiver
   *
   * @param uriInfo
   * @param resourceRequest
   * @param name
   * @return
   */
  public ReceiverDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, String name, Project project)
      throws AlertException {
    Receiver receiver = getReceiver(name, project);
    return build(uriInfo, resourceRequest, receiver);
  }

  private Receiver getReceiver(String name, Project project) throws AlertException {
    Receiver receiver;
    try {
      if (project != null) {
        receiver = alertManagerConfiguration.getReceiver(name, project);
      } else {
        receiver = alertManagerConfiguration.getReceiver(name);
      }
    } catch (AlertManagerConfigReadException | AlertManagerConfigCtrlCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerNoSuchElementException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_NOT_FOUND, Level.FINE, e.getMessage());
    } catch (AlertManagerAccessControlException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
    }
    return receiver;
  }

  /**
   * Build a list of Receivers
   *
   * @param uriInfo
   * @param resourceRequest
   * @return
   */
  public ReceiverDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, ReceiverBeanParam receiverBeanParam
      , Project project, Boolean includeGlobal, Boolean expand)
      throws AlertException {
    if (expand) {
      return itemsExpand(new ReceiverDTO(), uriInfo, resourceRequest, receiverBeanParam, project, includeGlobal);
    }
    return items(new ReceiverDTO(), uriInfo, resourceRequest, receiverBeanParam, project, includeGlobal);
  }

  public ReceiverDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, ReceiverBeanParam receiverBeanParam,
      Boolean expand) throws AlertException {
    if (expand) {
      return itemsExpand(new ReceiverDTO(), uriInfo, resourceRequest, receiverBeanParam, null, null);
    }
    return items(new ReceiverDTO(), uriInfo, resourceRequest, receiverBeanParam, null, null);
  }

  private ReceiverDTO itemsExpand(ReceiverDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      ReceiverBeanParam receiverBeanParam, Project project, Boolean includeGlobal) throws AlertException {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      List<ReceiverName> receiverNameList = getReceiverNames(project, includeGlobal);
      List<Receiver> receivers = new ArrayList<>();
      for (ReceiverName receiverName : receiverNameList) {
        receivers.add(getReceiver(receiverName.getName(), project));
      }
      dto.setCount((long) receivers.size());
      return itemsExpand(dto, uriInfo, resourceRequest, receiverBeanParam, receivers);
    }
    return dto;
  }

  private ReceiverDTO items(ReceiverDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      ReceiverBeanParam receiverBeanParam, Project project, Boolean includeGlobal) throws AlertException {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      List<ReceiverName> receiverNameList = getReceiverNames(project, includeGlobal);
      dto.setCount((long) receiverNameList.size());
      return items(dto, uriInfo, resourceRequest, receiverBeanParam, receiverNameList);
    }
    return dto;
  }

  private List<ReceiverName> getReceiverNames(Project project, Boolean includeGlobal) throws AlertException {
    List<ReceiverName> receiverNameList;
    try {
      if (project != null) {
        receiverNameList = alertManager.getReceivers(project, includeGlobal != null && includeGlobal);
      } else {
        receiverNameList = alertManager.getReceivers();
      }
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    }
    return receiverNameList;
  }

  private ReceiverDTO items(ReceiverDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      ReceiverBeanParam receiverBeanParam, List<ReceiverName> receivers) {
    if (receivers != null && !receivers.isEmpty()) {
      receivers.forEach((receiver) -> dto.addItem(build(uriInfo, resourceRequest, receiver)));
      if (receiverBeanParam.getSortBySet() != null && !receiverBeanParam.getSortBySet().isEmpty()) {
        ReceiverComparator receiverComparator = new ReceiverComparator(receiverBeanParam.getSortBySet());
        dto.getItems().sort(receiverComparator);
      }
      paginate(dto, resourceRequest);
    }
    return dto;
  }

  private ReceiverDTO itemsExpand(ReceiverDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      ReceiverBeanParam receiverBeanParam, List<Receiver> receivers) {
    if (receivers != null && !receivers.isEmpty()) {
      receivers.forEach((receiver) -> dto.addItem(build(uriInfo, resourceRequest, receiver)));
      if (receiverBeanParam.getSortBySet() != null && !receiverBeanParam.getSortBySet().isEmpty()) {
        ReceiverComparator receiverComparator = new ReceiverComparator(receiverBeanParam.getSortBySet());
        dto.getItems().sort(receiverComparator);
      }
      paginate(dto, resourceRequest);
    }
    return dto;
  }

  private void paginate(RestDTO restDTO, ResourceRequest resourceRequest) {
    if (restDTO.getItems() != null && restDTO.getItems().size() > 1) {
      int offset = resourceRequest.getOffset() != null ? resourceRequest.getOffset() : 0;
      int limit = resourceRequest.getLimit() != null ? resourceRequest.getLimit() : restDTO.getItems().size();
      restDTO.getItems()
          .subList(Math.min(restDTO.getItems().size(), offset), Math.min(restDTO.getItems().size(), offset + limit));
    }
  }

  public Receiver build(PostableReceiverDTO postableReceiverDTO, Boolean defaultTemplate, boolean checkUnique)
      throws AlertException {
    return new Receiver(postableReceiverDTO.getName())
        .withEmailConfigs(toEmailConfig(postableReceiverDTO.getEmailConfigs(), defaultTemplate, checkUnique))
        .withSlackConfigs(toSlackConfig(postableReceiverDTO.getSlackConfigs(), defaultTemplate, checkUnique))
        .withPagerdutyConfigs(toPagerdutyConfig(postableReceiverDTO.getPagerdutyConfigs(), checkUnique))
        .withOpsgenieConfigs(toOpsgenieConfig(postableReceiverDTO.getOpsgenieConfigs()))
        .withPushoverConfigs(toPushoverConfig(postableReceiverDTO.getPushoverConfigs()))
        .withVictoropsConfigs(toVictoropsConfig(postableReceiverDTO.getVictoropsConfigs()))
        .withWebhookConfigs(toWebhookConfig(postableReceiverDTO.getWebhookConfigs()))
        .withWechatConfigs(toWechatConfig(postableReceiverDTO.getWechatConfigs()));
  }

  private List<EmailConfig> toEmailConfig(List<PostableEmailConfig> postableEmailConfigs, Boolean defaultTemplate,
      boolean checkUnique) throws AlertException {
    if (postableEmailConfigs != null && !postableEmailConfigs.isEmpty()) {
      List<EmailConfig> emailConfigs = postableEmailConfigs.stream().map(p -> p.toEmailConfig(defaultTemplate))
          .collect(Collectors.toList());
      if (checkUnique) {
        Set<EmailConfig> emailConfigSet = new HashSet<>(emailConfigs);
        if (emailConfigs.size() != emailConfigSet.size()) {
          throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Emails are not unique.");
        }
      }
      return emailConfigs;
    }
    return null;
  }

  private List<SlackConfig> toSlackConfig(List<PostableSlackConfig> slackConfigs, Boolean defaultTemplate,
    boolean checkUnique) throws AlertException {
    if (slackConfigs != null && !slackConfigs.isEmpty()) {
      List<SlackConfig> slackConfigList = slackConfigs.stream().map(p -> p.toSlackConfig(defaultTemplate))
          .collect(Collectors.toList());
      if (checkUnique) {
        Set<SlackConfig> slackConfigSet = new HashSet<>(slackConfigList);
        if (slackConfigList.size() != slackConfigSet.size()) {
          throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Slack channels are not " +
              "unique.");
        }
      }
      return slackConfigList;
    }
    return null;
  }

  private List<PagerdutyConfig> toPagerdutyConfig(List<PostablePagerdutyConfig> pagerdutyConfigs, boolean checkUnique)
    throws AlertException {
    if (pagerdutyConfigs != null && !pagerdutyConfigs.isEmpty()) {
      List<PagerdutyConfig> pagerdutyConfigList =
          pagerdutyConfigs.stream().map(PostablePagerdutyConfig::toPagerdutyConfig).collect(Collectors.toList());
      if (checkUnique) {
        Set<PagerdutyConfig> pagerdutyConfigSet = new HashSet<>(pagerdutyConfigList);
        if (pagerdutyConfigList.size() != pagerdutyConfigSet.size()) {
          throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Pagerdutys are not unique.");
        }
      }
      return pagerdutyConfigList;
    }
    return null;
  }

  private List<OpsgenieConfig> toOpsgenieConfig(List<PostableOpsgenieConfig> opsgenieConfigs) {
    if (opsgenieConfigs != null && !opsgenieConfigs.isEmpty()) {
      return opsgenieConfigs.stream().map(PostableOpsgenieConfig::toOpsgenieConfig)
          .collect(Collectors.toList());
    }
    return null;
  }

  private List<WechatConfig> toWechatConfig(List<PostableWechatConfig> wechatConfigs) {
    if (wechatConfigs != null && !wechatConfigs.isEmpty()) {
      return wechatConfigs.stream().map(PostableWechatConfig::toWechatConfig)
        .collect(Collectors.toList());
    }
    return null;
  }

  private List<WebhookConfig> toWebhookConfig(List<PostableWebhookConfig> webhookConfigs) {
    if (webhookConfigs != null && !webhookConfigs.isEmpty()) {
      return webhookConfigs.stream().map(PostableWebhookConfig::toWebhookConfig)
        .collect(Collectors.toList());
    }
    return null;
  }

  private List<VictoropsConfig> toVictoropsConfig(List<PostableVictoropsConfig> victoropsConfigs) {
    if (victoropsConfigs != null && !victoropsConfigs.isEmpty()) {
      return victoropsConfigs.stream().map(PostableVictoropsConfig::toVictoropsConfig)
        .collect(Collectors.toList());
    }
    return null;
  }

  private List<PushoverConfig> toPushoverConfig(List<PostablePushoverConfig> pushoverConfigs) {
    if (pushoverConfigs != null && !pushoverConfigs.isEmpty()) {
      return pushoverConfigs.stream().map(PostablePushoverConfig::toPushoverConfig)
        .collect(Collectors.toList());
    }
    return null;
  }

  public GlobalReceiverDefaults build() throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    AlertManagerConfig alertManagerConfig = alertManagerConfiguration.read();
    GlobalReceiverDefaults globalReceiverDefaults = new GlobalReceiverDefaults();
    if (alertManagerConfig.getGlobal() != null) {
      globalReceiverDefaults.setEmailConfigured(!Strings.isNullOrEmpty(alertManagerConfig.getGlobal().getSmtpFrom()) &&
          !Strings.isNullOrEmpty(alertManagerConfig.getGlobal().getSmtpSmarthost()));
      globalReceiverDefaults
          .setSlackConfigured(!Strings.isNullOrEmpty(alertManagerConfig.getGlobal().getSlackApiUrl()));
      globalReceiverDefaults
          .setPagerdutyConfigured(!Strings.isNullOrEmpty(alertManagerConfig.getGlobal().getPagerdutyUrl()));
      globalReceiverDefaults
          .setOpsgenieConfigured(!Strings.isNullOrEmpty(alertManagerConfig.getGlobal().getOpsgenieApiKey()));
      globalReceiverDefaults.setPushoverConfigured(true);
      globalReceiverDefaults
          .setVictoropsConfigured(!Strings.isNullOrEmpty(alertManagerConfig.getGlobal().getVictoropsApiUrl()) &&
              !Strings.isNullOrEmpty(alertManagerConfig.getGlobal().getVictoropsApiKey()));
      globalReceiverDefaults.setWebhookConfigured(true);
      globalReceiverDefaults
          .setWechatConfigured(!Strings.isNullOrEmpty(alertManagerConfig.getGlobal().getWechatApiUrl()) &&
              !Strings.isNullOrEmpty(alertManagerConfig.getGlobal().getWechatApiSecret()));
    }
    return globalReceiverDefaults;
  }

  static class ReceiverComparator implements Comparator<ReceiverDTO> {

    Set<ReceiverSortBy> sortBy;

    ReceiverComparator(Set<ReceiverSortBy> sortBy) {
      this.sortBy = sortBy;
    }

    private int compare(ReceiverDTO a, ReceiverDTO b, ReceiverSortBy sortBy) {
      if (sortBy.getSortBy() == ReceiverSortBy.SortBys.NAME) {
        return order(a.getName(), b.getName(), sortBy.getParam());
      }
      throw new UnsupportedOperationException("Sort By " + sortBy + " not supported");
    }

    private int order(String a, String b, AbstractFacade.OrderBy orderBy) {
      switch (orderBy) {
        case ASC:
          return String.CASE_INSENSITIVE_ORDER.compare(a, b);
        case DESC:
          return String.CASE_INSENSITIVE_ORDER.compare(b, a);
        default:
          throw new UnsupportedOperationException("Order By " + orderBy + " not supported");
      }
    }

    @Override
    public int compare(ReceiverDTO a, ReceiverDTO b) {
      Iterator<ReceiverSortBy> sort = sortBy.iterator();
      int c = compare(a, b, sort.next());
      for (; sort.hasNext() && c == 0; ) {
        c = compare(a, b, sort.next());
      }
      return c;
    }
  }
}