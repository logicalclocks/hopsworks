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
import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.alerting.api.alert.dto.ReceiverName;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.EmailConfig;
import io.hops.hopsworks.alerting.config.dto.OpsgenieConfig;
import io.hops.hopsworks.alerting.config.dto.PagerdutyConfig;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.SlackConfig;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.api.alert.Entry;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    }
    if (receiverBeanParam.getSortBySet() != null && !receiverBeanParam.getSortBySet().isEmpty()) {
      ReceiverComparator receiverComparator = new ReceiverComparator(receiverBeanParam.getSortBySet());
      dto.getItems().sort(receiverComparator);
    }
    paginate(dto, resourceRequest);
    return dto;
  }

  private ReceiverDTO itemsExpand(ReceiverDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      ReceiverBeanParam receiverBeanParam, List<Receiver> receivers) {
    if (receivers != null && !receivers.isEmpty()) {
      receivers.forEach((receiver) -> dto.addItem(build(uriInfo, resourceRequest, receiver)));
    }
    if (receiverBeanParam.getSortBySet() != null && !receiverBeanParam.getSortBySet().isEmpty()) {
      ReceiverComparator receiverComparator = new ReceiverComparator(receiverBeanParam.getSortBySet());
      dto.getItems().sort(receiverComparator);
    }
    paginate(dto, resourceRequest);
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

  public Receiver build(PostableReceiverDTO postableReceiverDTO, Boolean defaultTemplate) {
    Receiver receiver = new Receiver(postableReceiverDTO.getName())
        .withEmailConfigs(toEmailConfig(postableReceiverDTO.getEmailConfigs(), defaultTemplate))
        .withSlackConfigs(toSlackConfig(postableReceiverDTO.getSlackConfigs(), defaultTemplate))
        .withPagerdutyConfigs(toPagerdutyConfig(postableReceiverDTO.getPagerdutyConfigs(), defaultTemplate))
        .withOpsgenieConfigs(toOpsgenieConfig(postableReceiverDTO.getOpsgenieConfigs()))
        .withPushoverConfigs(postableReceiverDTO.getPushoverConfigs())
        .withVictoropsConfigs(postableReceiverDTO.getVictoropsConfigs())
        .withWebhookConfigs(postableReceiverDTO.getWebhookConfigs())
        .withWechatConfigs(postableReceiverDTO.getWechatConfigs());
    return receiver;
  }

  private List<EmailConfig> toEmailConfig(List<PostableEmailConfig> postableEmailConfigs, Boolean defaultTemplate) {
    if (postableEmailConfigs != null && !postableEmailConfigs.isEmpty()) {
      return postableEmailConfigs.stream().map(p -> toEmailConfig(p, defaultTemplate))
          .collect(Collectors.toList());
    }
    return null;
  }

  private EmailConfig toEmailConfig(PostableEmailConfig postableEmailConfigs, Boolean defaultTemplate) {
    EmailConfig emailConfig = new EmailConfig(postableEmailConfigs.getTo())
        .withFrom(postableEmailConfigs.getFrom())
        .withSmarthost(postableEmailConfigs.getSmarthost())
        .withHello(postableEmailConfigs.getHello())
        .withAuthIdentity(postableEmailConfigs.getAuthIdentity())
        .withAuthPassword(postableEmailConfigs.getAuthPassword())
        .withAuthSecret(postableEmailConfigs.getAuthSecret())
        .withAuthUsername(postableEmailConfigs.getAuthUsername())
        .withHtml(postableEmailConfigs.getHtml())
        .withText(postableEmailConfigs.getText())
        .withRequireTls(postableEmailConfigs.getRequireTls())
        .withTlsConfig(postableEmailConfigs.getTlsConfig())
        .withSendResolved(postableEmailConfigs.getSendResolved());
    if (postableEmailConfigs.getHeaders() != null && !postableEmailConfigs.getHeaders().isEmpty()) {
      Map<String, String> headers;
      headers = postableEmailConfigs.getHeaders().stream()
          .filter(entry -> entry != null && entry.getKey() != null && entry.getValue() != null)
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      emailConfig.setHeaders(headers);
    }
    if (defaultTemplate && Strings.isNullOrEmpty(emailConfig.getHtml())) {
      emailConfig.setHtml(Constants.DEFAULT_EMAIL_HTML);
    }
    return emailConfig;
  }

  private List<SlackConfig> toSlackConfig(List<SlackConfig> slackConfigs, Boolean defaultTemplate) {
    if (slackConfigs != null && !slackConfigs.isEmpty()) {
      return slackConfigs.stream().map(p -> toSlackConfig(p, defaultTemplate))
          .collect(Collectors.toList());
    }
    return null;
  }

  private SlackConfig toSlackConfig(SlackConfig slackConfig, Boolean defaultTemplate) {
    if (defaultTemplate) {
      if (Strings.isNullOrEmpty(slackConfig.getIconUrl())) {
        slackConfig.setIconUrl(Constants.DEFAULT_SLACK_ICON_URL);
      }
      if (Strings.isNullOrEmpty(slackConfig.getText())) {
        slackConfig.setText(Constants.DEFAULT_SLACK_TEXT);
      }
      if (Strings.isNullOrEmpty(slackConfig.getTitle())) {
        slackConfig.setTitle(Constants.DEFAULT_SLACK_TITLE);
      }
    }
    return slackConfig;
  }

  private List<PagerdutyConfig> toPagerdutyConfig(List<PostablePagerdutyConfig> pagerdutyConfigs,
      Boolean defaultTemplate) {
    if (pagerdutyConfigs != null && !pagerdutyConfigs.isEmpty()) {
      return pagerdutyConfigs.stream().map(p -> toPagerdutyConfig(p, defaultTemplate))
          .collect(Collectors.toList());
    }
    return null;
  }

  private PagerdutyConfig toPagerdutyConfig(PostablePagerdutyConfig p, Boolean defaultTemplate) {
    Map<String, String> details = null;
    if (p.getDetails() != null && !p.getDetails().isEmpty()) {
      details = p.getDetails().stream()
          .filter(entry -> entry != null && entry.getKey() != null && entry.getValue() != null)
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
    return new PagerdutyConfig()
        .withSendResolved(p.getSendResolved())
        .withRoutingKey(p.getRoutingKey())
        .withServiceKey(p.getServiceKey())
        .withUrl(p.getUrl())
        .withClient(p.getClient())
        .withClientUrl(p.getClientUrl())
        .withDescription(p.getDescription())
        .withSeverity(p.getSeverity())
        .withDetails(details)
        .withImages(p.getImages())
        .withLinks(p.getLinks())
        .withHttpConfig(p.getHttpConfig());
  }

  private List<OpsgenieConfig> toOpsgenieConfig(List<PostableOpsgenieConfig> opsgenieConfigs) {
    if (opsgenieConfigs != null && !opsgenieConfigs.isEmpty()) {
      return opsgenieConfigs.stream().map(p -> toOpsgenieConfig(p))
          .collect(Collectors.toList());
    }
    return null;
  }

  private OpsgenieConfig toOpsgenieConfig(PostableOpsgenieConfig p) {
    Map<String, String> details = null;
    if (p.getDetails() != null && !p.getDetails().isEmpty()) {
      details = p.getDetails().stream()
          .filter(entry -> entry != null && entry.getKey() != null && entry.getValue() != null)
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
    return new OpsgenieConfig()
        .withSendResolved(p.getSendResolved())
        .withApiKey(p.getApiKey())
        .withApiUrl(p.getApiUrl())
        .withMessage(p.getMessage())
        .withDescription(p.getDescription())
        .withSource(p.getSource())
        .withDetails(details)
        .withResponders(p.getResponders())
        .withTags(p.getTags())
        .withNote(p.getNote())
        .withPriority(p.getPriority())
        .withHttpConfig(p.getHttpConfig());
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

  class ReceiverComparator implements Comparator<ReceiverDTO> {

    Set<ReceiverSortBy> sortBy;

    ReceiverComparator(Set<ReceiverSortBy> sortBy) {
      this.sortBy = sortBy;
    }

    private int compare(ReceiverDTO a, ReceiverDTO b, ReceiverSortBy sortBy) {
      switch (sortBy.getSortBy()) {
        case NAME:
          return order(a.getName(), b.getName(), sortBy.getParam());
        default:
          throw new UnsupportedOperationException("Sort By " + sortBy + " not supported");
      }
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