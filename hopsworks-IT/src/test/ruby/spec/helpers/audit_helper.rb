=begin
 This file is part of Hopsworks
 Copyright (C) 2020, Logical Clocks AB. All rights reserved

 Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 the GNU Affero General Public License as published by the Free Software Foundation,
 either version 3 of the License, or (at your option) any later version.

 Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/>.
=end

module AuditHelper
  @@Log_file = ENV.fetch('AUDIT_LOG_DIR', '/srv/hops/domains/domain1/logs/audit/')

  def getLatestAccountAudit(initiator, target, action)
    AccountAudit.where(initiator: "#{initiator}", target: "#{target}", action: "#{action}").order({ action_timestamp:
                                                                                                        :asc }).first
  end

  def getLatestRoleAudit(initiator, target, action)
    RolesAudit.where(initiator: "#{initiator}", target: "#{target}", action: "#{action}").order({ action_timestamp:
                                                                                                     :asc }).first
  end

  def getLatestUserLogin(uid, action)
    UserLogins.where(uid: "#{uid}", action: "#{action}").order({ login_date: :asc }).first
  end

  def getLogLastLine
    lastLogFile = (Dir["#{@@Log_file}*"]).max_by {|f| File.mtime(f)}
    lastLine = `tail -n 1 #{lastLogFile}`
    #if last line is by agent get previous
    if lastLine.include? "Caller: agent@hops.io"
      lastLine = `tail -n 3 #{lastLogFile}`
    end
    lastLine
  end

  def testLog(method, caller, response)
    logLine = getLogLastLine
    expect(logLine).to include("Method Called: #{method}")
    expect(logLine).to include("Caller: #{caller}")
    expect(logLine).to include("#{response}")
  end


end