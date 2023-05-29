=begin
 This file is part of Hopsworks
 Copyright (C) 2021, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

module AlertHelper
  @@alert_resource = "#{ENV['HOPSWORKS_API']}/project/%{projectId}/alerts"
  @@alert_silence_resource = "#{ENV['HOPSWORKS_API']}/project/%{projectId}/alerts/silences"
  @@alert_route_resource = "#{ENV['HOPSWORKS_API']}/project/%{projectId}/alerts/routes"
  @@alert_receiver_resource = "#{ENV['HOPSWORKS_API']}/project/%{projectId}/alerts/receivers"

  @@admin_alert_resource = "#{ENV['HOPSWORKS_API']}/admin/alerts"
  @@admin_alert_silence_resource = "#{ENV['HOPSWORKS_API']}/admin/alerts/silences"
  @@admin_alert_route_resource = "#{ENV['HOPSWORKS_API']}/admin/alerts/routes"
  @@admin_alert_receiver_resource = "#{ENV['HOPSWORKS_API']}/admin/alerts/receivers"
  @@admin_alert_management = "#{ENV['HOPSWORKS_API']}/admin/alerts/management"

  @@alert = {"annotations": [], "labels": [], "endsAt": nil, "startsAt": nil}
  @@silence = {"comment": nil, "endsAt": nil, "startsAt": nil, "matchers": []}
  @@route = {"groupBy": nil, "groupWait": nil, "groupInterval": nil, "repeatInterval": nil, "receiver": nil,
             "routes": nil, "continue": nil, "match": nil, "matchRe": nil}

  @@email_config = {"to": nil, "from": nil, "smarthost": nil}
  @@slack_config = {"apiUrl": nil, "channel": nil}
  @@pagerduty_config = {"routingKey": nil, "serviceKey": nil, "url": nil}
  @@receiver = {"name": nil, "emailConfigs": nil, "pagerdutyConfigs": nil, "pushoverConfigs": nil,
                "slackConfigs": nil,  "opsgenieConfigs": nil, "webhookConfigs": nil, "victoropsConfigs": nil,
                "wechatConfigs": nil}

  @@global = { "smtpSmarthost": "smtp.gmail.com:587",
               "smtpFrom": "admin@hopsworks.ai",
               "smtpAuthUsername": "admin@hopsworks.ai",
               "smtpAuthIdentity": "admin@hopsworks.ai",
               "smtpAuthPassword": "password" }
  @@templates = { "templates": %w[/srv/hops/alertmanager/alertmanager/template/email.tmpl /srv/hops/alertmanager/alertmanager/template/slack.tmpl] }
  @@route_admin = {"groupBy": %w[alertname cluster service],
                   "groupWait": "30s",
                   "groupInterval": "5m",
                   "repeatInterval": "5h",
                   "receiver": "email",
                   "routes": nil
                  }
  @@inhibit_rules = { "sourceMatch": ["key": "severity", "value": "critical"],
                      "targetMatch": ["key": "severity", "value": "critical"],
                      "equal": %w[alertname cluster service] }

  def create_match(projectName)
    return [
      {"key": "project", "value": projectName },
    ]
  end

  def create_labels(project)
    return [
      {"key": "alertname", "value":"HopsworksAlert" },
      {"key": "instance", "value":"hopsworks0" },
      {"key": "project", "value": project[:projectname] },
      {"key": "executionId", "value": "#{rand(1...5000)}" },
      {"key": "job", "value": "#{rand(1...500)}" },
      {"key": "severity", "value": %w[warning info].sample },
      {"key": "status", "value": %w[failed success].sample }
    ]
  end

  def create_annotations
    return [
      {"key": "info", "value":"annotation information" },
      {"key": "summary", "value":"annotation summary" }
    ]
  end

  def create_matchers(projectName)
    return [{"isRegex": false, "name": "project", "value": projectName}]
  end

  def create_alert(project, labels: nil, annotations: nil, startsAt: DateTime.now, endsAt: DateTime.now + 1.days)
    alert = @@alert.clone
    alert[:labels] = labels ? labels : create_labels(project)
    alert[:annotations] = annotations ? annotations : create_annotations
    alert[:endsAt] = endsAt.strftime("%Y-%m-%dT%H:%M:%S.%LZ")
    alert[:startsAt] = startsAt.strftime("%Y-%m-%dT%H:%M:%S.%LZ")
    return alert
  end

  def create_silence(project, matchers: nil, startsAt: DateTime.now, endsAt: DateTime.now + 1.days)
    silence = @@silence.clone
    silence[:matchers] = matchers ? matchers : create_matchers(project[:projectname])
    silence[:endsAt] = endsAt.strftime("%Y-%m-%dT%H:%M:%S.%LZ")
    silence[:startsAt] = startsAt.strftime("%Y-%m-%dT%H:%M:%S.%LZ")
    silence[:comment] = "test silence"
    return silence
  end

  def get_receiver_name(projectName, receiverName)
    return "#{projectName}__#{receiverName}"
  end

  def create_route(project, receiver: nil)
    route = @@route.clone
    route[:receiver] = receiver ? receiver : get_receiver_name(project[:projectname], "#{random_id_len(10)}")
    route[:match] = create_match(project[:projectname])
    return route
  end

  def create_email_config(to: nil)
    email_config = @@email_config.clone
    email_config[:to] = to ? to : "admin@hopsworks.ai"
    email_config[:from] = "hopsworks@gmail.com"
    email_config[:smarthost] = "smtp.gmail.com:587"
    return email_config
  end

  def create_slack_config(apiUrl: nil, channel: nil)
    slack_config = @@slack_config.clone
    slack_config[:apiUrl] = apiUrl ? apiUrl : "https://hooks.slack.com/services/#{random_id_len(10)}/#{random_id_len(15)}"
    slack_config[:channel] = channel ? channel : "#random#{random_id_len(4)}"
    return slack_config
  end

  def create_pager_duty_config(serviceKey: nil, url: nil)
    pagerduty_config = @@pagerduty_config.clone
    pagerduty_config[:serviceKey] = serviceKey ? serviceKey : random_id_len(10)
    pagerduty_config[:url] = url ? url : "https://pager.com/services/#{random_id_len(10)}/#{random_id_len(15)}"
    return pagerduty_config
  end

  def create_receiver(project, name: nil, emailConfigs: nil, pagerdutyConfigs: nil, pushoverConfigs: nil,
                      slackConfigs: nil,  opsgenieConfigs: nil, webhookConfigs: nil, victoropsConfigs: nil,
                      wechatConfigs: nil)
    receiver = @@receiver.clone
    receiver[:name] = name ? name : get_receiver_name(project[:projectname], "#{random_id_len(10)}")
    receiver[:emailConfigs] = emailConfigs
    receiver[:pagerdutyConfigs] = pagerdutyConfigs
    receiver[:pushoverConfigs] = pushoverConfigs
    receiver[:slackConfigs] = slackConfigs
    receiver[:opsgenieConfigs] = opsgenieConfigs
    receiver[:webhookConfigs] = webhookConfigs
    receiver[:victoropsConfigs] = victoropsConfigs
    receiver[:wechatConfigs] = wechatConfigs
    return receiver
  end

  def check_eq_receiver_email_config(json_body, receiver)
    expect(json_body[:name]).to eq(receiver[:name])
    expect(json_body[:emailConfigs][0][:to]).to eq(receiver[:emailConfigs][0][:to])
    expect(json_body[:emailConfigs][0][:from]).to eq(receiver[:emailConfigs][0][:from])
    expect(json_body[:emailConfigs][0][:smarthost]).to eq(receiver[:emailConfigs][0][:smarthost])
  end

  def check_eq_match(json_body, project)
    projectEntry = json_body[:match][:project]
    expect(projectEntry).to eq(project[:projectname])
  end

  def create_random_alerts(project, num: 3)
    alerts = create_random_alerts_list(project, num: num)
    create_alerts(project, {:alerts=> alerts})
  end

  def create_random_alerts_list(project, num: 3)
    alerts = Array.new(num)
    i = 1
    while i <= num  do
      alerts[i-1] = create_alert(project, startsAt: DateTime.now + i.minutes, endsAt: DateTime.now + i.hours)
      i = i+1
    end
    return alerts
  end

  def alerts_compare_date_sort_result(json_body, order: "asc", attr: "startsAt", attr2: nil, order2: "asc")
    orderBy = order == "asc"? 1 : -1
    orderBy2 = order2 == "asc"? 1 : -1
    sorted = json_body[:items].sort do |a, b|
      res = orderBy * (a[:"#{attr}"] <=> b[:"#{attr}"])
      res = orderBy2 * (a[:"#{attr2}"] <=> b[:"#{attr2}"]) if res == 0 && attr2
      res
    end
    expect(json_body[:items]).to eq(sorted)
  end

  def create_random_alerts_admin
    project = {}
    project[:projectname] = "testAdminProject1"
    alerts = create_random_alerts_list(project)
    create_alerts_admin({:alerts=> alerts})
  end

  def create_random_silences_list(project, num: 3)
    silences = Array.new(num)
    i = 1
    while i <= num  do
      silences[i-1] = create_silence(project, startsAt: DateTime.now + i.minutes, endsAt: DateTime.now + i.hours)
      i = i+1
    end
    return silences
  end

  def create_random_silences(project, num: 3)
    silences = create_random_silences_list(project, num: num)
    i = 0
    while i < num do
      create_silences_checked(project, silences[i])
      i = i+1
    end
  end

  def create_random_silences_admin(num: 3)
    project = {}
    project[:projectname] = "testAdminProject1"
    silences = create_random_silences_list(project, num: num)
    i = 0
    while i < num do
      create_silences_admin_checked(silences[i])
      i = i+1
    end
  end

  def create_route_list(project, num: 3)
    routes = Array.new(num)
    i = 0
    while i < num  do
      routes[i] = create_route(project)
      i = i+1
    end
    return routes
  end

  def create_random_routes(project, num: 3)
    i = 0
    while i < num do
      receiver = create_receiver(project, emailConfigs: [create_email_config])
      create_receivers_checked(project, receiver)
      create_routes_checked(project, create_route(project, receiver: receiver[:name]))
      i = i+1
    end
  end

  def create_random_route(project)
    receiver = create_receiver(project, emailConfigs: [create_email_config])
    create_receivers_checked(project, receiver)
    route = create_route(project, receiver: receiver[:name])
    create_routes_checked(project, route)
    return route
  end

  def create_random_routes_admin(project, num: 3)
    i = 0
    while i < num do
      receiver = create_receiver(project, emailConfigs: [create_email_config])
      create_receivers_admin_checked(receiver)
      create_routes_admin_checked(create_route(project, receiver: receiver[:name]))
      i = i+1
    end
  end

  def create_random_route_admin(project)
    receiver = create_receiver(project, emailConfigs: [create_email_config])
    create_receivers_admin_checked(receiver)
    route = create_route(project, receiver: receiver[:name])
    create_routes_admin_checked(route)
    return route
  end

  def create_random_receiver(project)
    create_receivers_checked(project, create_receiver(project, emailConfigs: [create_email_config]))
    create_receivers_checked(project, create_receiver(project, slackConfigs: [create_slack_config]))
    create_receivers_checked(project, create_receiver(project, pagerdutyConfigs: [create_pager_duty_config]))
  end

  def create_random_receiver_admin(project)
    create_receivers_admin_checked(create_receiver(project, emailConfigs: [create_email_config]))
    create_receivers_admin_checked(create_receiver(project, slackConfigs: [create_slack_config]))
    create_receivers_admin_checked(create_receiver(project, pagerdutyConfigs: [create_pager_duty_config]))
  end

  def with_global_receivers
    create_receivers_admin(create_receiver(nil, name: "global-receiver__email", emailConfigs: [create_email_config]))
    create_receivers_admin(create_receiver(nil, name: "global-receiver__slack", slackConfigs: [create_slack_config]))
    create_receivers_admin(create_receiver(nil, name: "global-receiver__pagerduty", pagerdutyConfigs: [create_pager_duty_config]))
  end

  def with_receivers(project)
    create_receivers_checked(project, create_receiver(project, name: "#{project[:projectname]}__email", emailConfigs:
      [create_email_config]))
    create_receivers_checked(project, create_receiver(project, name: "#{project[:projectname]}__slack", slackConfigs:
      [create_slack_config]))
    create_receivers_checked(project, create_receiver(project, name: "#{project[:projectname]}__pagerduty",
                                                      pagerdutyConfigs: [create_pager_duty_config]))
    create_receivers_checked(project, create_receiver(project, name: "#{project[:projectname]}__slack1",
                                                      slackConfigs: [create_slack_config]))
  end

  def get_alerts(project, query: "")
    get "#{@@alert_resource}#{query}" % {projectId: project[:id]}
  end

  def get_alerts_admin(query: "")
    get "#{@@admin_alert_resource}#{query}"
  end

  def get_alert_groups(project, query: "")
    get "#{@@alert_resource}/groups?expand_alert=true#{query}" % {projectId: project[:id]}
  end

  def get_alert_groups_admin(query: "")
    get "#{@@admin_alert_resource}/groups?expand_alert=true#{query}"
  end

  def create_alerts(project, alerts)
    post @@alert_resource % {projectId: project[:id]}, alerts.to_json
  end

  def create_alerts_admin(alerts)
    post @@admin_alert_resource, alerts.to_json
  end

  def get_silences(project, query: "")
    get "#{@@alert_silence_resource}#{query}" % {projectId: project[:id]}
  end

  def get_silences_by_id(project, id)
    get "#{@@alert_silence_resource}/#{id}" % {projectId: project[:id]}
  end

  def create_silences(project, silence)
    post "#{@@alert_silence_resource}" % {projectId: project[:id]}, silence.to_json
    # --cluster.gossip-interval value: cluster message propagation speed (default "200ms")
    sleep(2)
  end

  def create_silences_checked(project, silence)
    post "#{@@alert_silence_resource}" % {projectId: project[:id]}, silence.to_json
    expect_status_details(201)
    # --cluster.gossip-interval value: cluster message propagation speed (default "200ms")
    sleep(2)
  end

  def update_silences(project, id, silence)
    put "#{@@alert_silence_resource}/#{id}" % {projectId: project[:id]}, silence.to_json
    # --cluster.gossip-interval value: cluster message propagation speed (default "200ms")
    sleep(2)
  end

  def delete_silences(project, id)
    delete "#{@@alert_silence_resource}/#{id}" % {projectId: project[:id]}
    # --cluster.gossip-interval value: cluster message propagation speed (default "200ms")
    sleep(2)
  end

  def get_routes(project, query: "")
    get "#{@@alert_route_resource}#{query}" % {projectId: project[:id]}
  end

  def get_routes_by_receiver(project, receiver, query: "")
    get "#{@@alert_route_resource}/#{receiver}#{query}" % {projectId: project[:id]}
  end

  def create_routes(project, route)
    post "#{@@alert_route_resource}" % {projectId: project[:id]}, route.to_json
  end

  def create_routes_checked(project, route)
    post "#{@@alert_route_resource}" % {projectId: project[:id]}, route.to_json
    expect_status_details(201)
  end

  def update_routes(project, route, receiver, query: "")
    put "#{@@alert_route_resource}/#{receiver}#{query}" % {projectId: project[:id]}, route.to_json
  end

  def delete_routes(project, receiver, query: "")
    delete "#{@@alert_route_resource}/#{receiver}#{query}" % {projectId: project[:id]}
  end

  def get_receivers(project, query: "")
    get "#{@@alert_receiver_resource}#{query}" % {projectId: project[:id]}
  end

  def get_receivers_by_name(project, name)
    get "#{@@alert_receiver_resource}/#{name}" % {projectId: project[:id]}
  end

  def create_receivers(project, receiver)
    post "#{@@alert_receiver_resource}" % {projectId: project[:id]}, receiver.to_json
  end

  def create_receivers_checked(project, receiver)
    post "#{@@alert_receiver_resource}" % {projectId: project[:id]}, receiver.to_json
    expect_status_details(201)
  end

  def update_receivers(project, name, receiver)
    put "#{@@alert_receiver_resource}/#{name}" % {projectId: project[:id]}, receiver.to_json
  end

  def delete_receivers(project, name)
    delete "#{@@alert_receiver_resource}/#{name}" % {projectId: project[:id]}
  end

  def get_silences_admin(query: "")
    get "#{@@admin_alert_silence_resource}#{query}"
  end

  def get_silences_by_id_admin(id)
    get "#{@@admin_alert_silence_resource}/#{id}"
  end

  def create_silences_admin(silence)
    post "#{@@admin_alert_silence_resource}", silence.to_json
    sleep(2)
  end

  def create_silences_admin_checked(silence)
    post "#{@@admin_alert_silence_resource}", silence.to_json
    expect_status_details(201)
    sleep(2)
  end

  def update_silences_admin(id, silence)
    put "#{@@admin_alert_silence_resource}/#{id}", silence.to_json
    sleep(2)
  end

  def delete_silences_admin(id)
    delete "#{@@admin_alert_silence_resource}/#{id}"
    sleep(2)
  end

  def get_routes_admin(query: "")
    get "#{@@admin_alert_route_resource}#{query}"
  end

  def get_routes_by_receiver_admin(receiver, query: "")
    get "#{@@admin_alert_route_resource}/#{receiver}#{query}"
  end

  def create_routes_admin(route)
    post "#{@@admin_alert_route_resource}", route.to_json
  end

  def create_routes_admin_checked(route)
    post "#{@@admin_alert_route_resource}", route.to_json
    expect_status_details(201)
  end

  def update_routes_admin(route, receiver, query: "")
    put "#{@@admin_alert_route_resource}/#{receiver}#{query}", route.to_json
  end

  def delete_routes_admin(receiver, query: "")
    delete "#{@@admin_alert_route_resource}/#{receiver}#{query}"
  end

  def get_receivers_admin(query: "")
    get "#{@@admin_alert_receiver_resource}#{query}"
  end

  def get_receivers_by_name_admin(name)
    get "#{@@admin_alert_receiver_resource}/#{name}"
  end

  def create_receivers_admin(receiver)
    post "#{@@admin_alert_receiver_resource}", receiver.to_json
  end

  def create_receivers_admin_checked(receiver)
    post "#{@@admin_alert_receiver_resource}", receiver.to_json
    expect_status_details(201)
  end

  def update_receivers_admin(name, receiver)
    put "#{@@admin_alert_receiver_resource}/#{name}" , receiver.to_json
  end

  def delete_receivers_admin(name)
    delete "#{@@admin_alert_receiver_resource}/#{name}"
  end

  def get_global_admin
    return @@global.clone
  end

  def get_templates_admin
    return @@templates.clone
  end

  def get_route_admin
    return @@route_admin.clone
  end

  def get_inhibit_rules_admin
    return {"postableInhibitRulesDTOs": [@@inhibit_rules.clone]}
  end

  def get_config_admin
    get "#{@@admin_alert_management}/config"
  end

  def update_global_admin(global)
    put "#{@@admin_alert_management}/global", global.to_json
  end

  def update_templates_admin(templates)
    put "#{@@admin_alert_management}/templates", templates.to_json
  end

  def update_route_admin(route)
    put "#{@@admin_alert_management}/route", route.to_json
  end

  def update_inhibit_admin(inhibit)
    put "#{@@admin_alert_management}/inhibit", inhibit.to_json
  end

  def check_backup_contains_receiver(receiver)
    config = AlertManagerConfig.order(created: :desc).first
    receivers = JSON.parse(config[:content])["receivers"]
    r = receivers.detect { |r| r["name"] == receiver[:name] }
    r1 = {}
    r1["name"] = receiver[:name]
    r1["emailConfigs"] = receiver[:emailConfigs]
    r1["slackConfigs"] = receiver[:slackConfigs]
    r1["pagerdutyConfigs"] = receiver[:pagerdutyConfigs]
    expect(r).to eq(JSON.parse(r1.compact.to_json))
  end

  def check_backup_contains_route(route)
    config = AlertManagerConfig.order(created: :desc).first
    routes = JSON.parse(config[:content])["route"]["routes"]
    r1 = {}
    r1["match"] = route[:match]
    r1["matchRe"] = route[:matchRe]
    r1["receiver"] = route[:receiver]
    r1["continue"] = route[:continue]
    r1["groupBy"] = route[:groupBy]
    r = routes.detect { |r| r == JSON.parse(r1.compact.to_json) }
    expect(r).to be_present
  end

  def check_receiver_deleted_from_backup(receiver)
    config = AlertManagerConfig.order(created: :desc).first
    receivers = JSON.parse(config[:content])["receivers"]
    r = receivers.detect { |r| r["name"] == receiver[:name] }
    expect(r).to be nil
  end

  def check_route_deleted_from_backup(route)
    config = AlertManagerConfig.order(created: :desc).first
    routes = JSON.parse(config[:content])["route"]["routes"]
    r = routes.detect { |r| r["receiver"] == route[:receiver] }
    expect(r).to be nil
  end

  def check_alert_receiver_created(receiver)
    alert_receiver = AlertReceiver.where(name: receiver).first
    config = JSON.parse(alert_receiver[:config])
    expect(config["name"]).to eq(receiver)
  end

  def create_route_match_query(project, receiver, status, job: nil, fg: nil)
    statusQ = status
    if status.include? "_"
      statusQ = status.partition('_')[2]
    end
    query = "?match=status:#{statusQ.capitalize}"
    query = receiver.start_with?("global-receiver__") ?
              "#{query}&match=type:global-alert-#{receiver.partition('__')[2]}" :
              "#{query}&match=project:#{project[:projectname]}&match=type:project-alert"
    query = job ? "#{query}&match=job:#{job[:name]}" : query
    query = fg ? "#{query}&match=featureGroup:#{fg['name']}" : query
    return query
  end

  def check_route_created(project, receiver, status, job: nil, fg: nil)
    query = create_route_match_query(project, receiver, status, job: job, fg: fg)
    get_routes_by_receiver(project, receiver, query: query)
    expect_status_details(200)
    check_backup_contains_route(json_body)
    check_alert_receiver_created(receiver)
    expect(json_body[:receiver]).to eq(receiver)
  end

  def check_route_exist(project, receiver, status, job: nil, fg: nil)
    query = create_route_match_query(project, receiver, status, job: job, fg: fg)
    get_routes_by_receiver(project, receiver, query: query)
    expect_status_details(200)
  end

  def check_route_deleted(project, receiver, status, job: nil, fg: nil)
    query = create_route_match_query(project, receiver, status, job: job, fg: fg)
    get_routes_by_receiver(project, receiver, query: query)
    expect_status_details(400)
  end

end
