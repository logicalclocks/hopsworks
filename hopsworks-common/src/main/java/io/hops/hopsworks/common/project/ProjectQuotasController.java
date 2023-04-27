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

package io.hops.hopsworks.common.project;

import io.hops.hopsworks.common.dao.hdfs.HdfsDirectoryWithQuotaFeatureFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.hive.HiveController;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.hdfs.HdfsDirectoryWithQuotaFeature;
import io.hops.hopsworks.persistence.entity.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.persistence.entity.project.PaymentType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectQuotasController {

  @Inject
  private HdfsDirectoryWithQuotaFeatureFacade hdfsDirectoryWithQuotaFeatureFacade;
  @Inject
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @Inject
  private HiveController hiveController;
  @Inject
  private ProjectTopicsFacade projectTopicsFacade;
  @Inject
  private ProjectFacade projectFacade;
  @Inject
  private ProjectServiceFacade projectServiceFacade;
  @Inject
  private FeaturestoreController featurestoreController;
  @Inject
  private InodeController inodeController;
  @Inject
  private DistributedFsService dfs;

  public Quotas getQuotas(Project project) {
    Quotas quotas = new Quotas();

    // Yarn Quota
    Optional<YarnProjectsQuota> yarnQuotaOpt = yarnProjectsQuotaFacade.findByProjectName(project.getName());
    if (yarnQuotaOpt.isPresent()) {
      quotas.setYarnQuotaInSecs(yarnQuotaOpt.get().getQuotaRemaining());
      quotas.setYarnUsedQuotaInSecs(yarnQuotaOpt.get().getTotal());
    }
    // HDFS project directory quota
    Optional<HdfsDirectoryWithQuotaFeature> projectInodeAttrsOptional =
        hdfsDirectoryWithQuotaFeatureFacade.getByInodeId(inodeController.getProjectRoot(project.getName()).getId());
    if (projectInodeAttrsOptional.isPresent()) {
      // storage quota
      Long storageQuota = projectInodeAttrsOptional.get().getSsquota().longValue();
      quotas.setHdfsQuota(convertToTeraBytes(storageQuota));
      quotas.setHdfsUsage(convertToTeraBytes(projectInodeAttrsOptional.get().getStorageSpace().longValue()));

      // namespace quota
      Long nsQuota = projectInodeAttrsOptional.get().getNsquota().longValue();
      quotas.setHdfsNsQuota(nsQuota);
      quotas.setHdfsNsCount(projectInodeAttrsOptional.get().getNscount().longValue());
    } else {
      // unlimited
      quotas.setHdfsQuota((float)HdfsConstants.QUOTA_RESET);
      quotas.setHdfsNsQuota(HdfsConstants.QUOTA_RESET);
    }

    // If the Hive service is enabled, get the quota information for the db directory
    List<Dataset> datasets = (List<Dataset>) project.getDatasetCollection();
    for (Dataset ds : datasets) {
      if (ds.getDsType() == DatasetType.HIVEDB) {
        Optional<HdfsDirectoryWithQuotaFeature> dbInodeAttrsOptional =
            hdfsDirectoryWithQuotaFeatureFacade.getByInodeId(ds.getInodeId());
        if (dbInodeAttrsOptional.isPresent()) {
          // storage quota
          quotas.setHiveQuota(convertToTeraBytes(dbInodeAttrsOptional.get().getSsquota().longValue()));
          quotas.setHiveUsage(convertToTeraBytes(dbInodeAttrsOptional.get().getStorageSpace().longValue()));

          // namespace quota
          quotas.setHiveNsQuota(dbInodeAttrsOptional.get().getNsquota().longValue());
          quotas.setHiveNsCount(dbInodeAttrsOptional.get().getNscount().longValue());
        } else {
          // ulimited
          quotas.setHiveQuota((float)HdfsConstants.QUOTA_RESET);
          quotas.setHiveNsQuota(HdfsConstants.QUOTA_RESET);
        }
      } else if (ds.getDsType() == DatasetType.FEATURESTORE) {
        Optional<HdfsDirectoryWithQuotaFeature> fsInodeAttrsOptional =
            hdfsDirectoryWithQuotaFeatureFacade.getByInodeId(ds.getInodeId());
        if (fsInodeAttrsOptional.isPresent()) {
          // storage quota
          quotas.setFeaturestoreQuota(convertToTeraBytes(fsInodeAttrsOptional.get().getSsquota().longValue()));
          quotas.setFeaturestoreUsage(convertToTeraBytes(fsInodeAttrsOptional.get().getStorageSpace().longValue()));

          // namespace quota
          quotas.setFeaturestoreNsQuota(fsInodeAttrsOptional.get().getNsquota().longValue());
          quotas.setFeaturestoreNsCount(fsInodeAttrsOptional.get().getNscount().longValue());
        } else {
          // unlimited
          quotas.setFeaturestoreQuota((float)HdfsConstants.QUOTA_RESET);
          quotas.setFeaturestoreNsQuota(HdfsConstants.QUOTA_RESET);
        }
      }
    }

    quotas.setKafkaNumTopics(projectTopicsFacade.countTopicsByProject(project));
    quotas.setKafkaMaxNumTopics(project.getKafkaMaxNumTopics());

    return quotas;
  }

  public Project updateQuotas(Project project, Quotas quotas, PaymentType newPaymentType) throws ProjectException {
    // Set the quotas information
    if (quotas != null) {

      DistributedFileSystemOps dfso = dfs.getDfsOps();
      try {
        dfso.setHdfsQuotaBytes(new Path(Utils.getProjectPath(project.getName())),
            quotas.getHdfsNsQuota(),
            convertToBytes(quotas.getHdfsQuota()));

        if (projectServiceFacade.isServiceEnabledForProject(project, ProjectServiceEnum.HIVE)) {
          dfso.setHdfsQuotaBytes(hiveController.getDbPath(project.getName()), quotas.getHiveNsQuota(),
              convertToBytes(quotas.getHiveQuota()));
        }

        if (projectServiceFacade.isServiceEnabledForProject(project, ProjectServiceEnum.FEATURESTORE)) {
          dfso.setHdfsQuotaBytes(
              hiveController.getDbPath(featurestoreController.getOfflineFeaturestoreDbName(project)),
              quotas.getFeaturestoreNsQuota(),
              convertToBytes(quotas.getFeaturestoreQuota()));
        }
      } catch (IOException e) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.QUOTA_ERROR,
            Level.SEVERE, "project: " + project.getName(), e.getMessage(), e);
      } finally {
        if (dfso != null) {
          dfso.close();
        }
      }

      yarnProjectsQuotaFacade.changeYarnQuota(project.getName(), quotas.getYarnQuotaInSecs());
      projectFacade.changeKafkaQuota(project, quotas.getKafkaMaxNumTopics());

      project.setPaymentType(newPaymentType);
      project.setLastQuotaUpdate(new Date());
      projectFacade.mergeProject(project);
    }

    return project;
  }

  public Float convertToTeraBytes(Long storage) {
    if (storage == HdfsConstants.QUOTA_RESET) {
      // this means that the storage is unlimited
      return (float)storage;
    }
    return storage / 1073741824f;
  }

  public Long convertToBytes(Float storage) {
    if (storage.longValue() == HdfsConstants.QUOTA_RESET) {
      // this means that the storage is unlimited
      return HdfsConstants.QUOTA_RESET;
    }

    return (long)(storage * 1073741824);
  }

}
