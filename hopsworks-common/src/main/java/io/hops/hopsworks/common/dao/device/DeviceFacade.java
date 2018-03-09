package io.hops.hopsworks.common.dao.device;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class DeviceFacade {

  private final static Logger logger = Logger.getLogger(DeviceFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public void createProjectDevicesSettings(ProjectDevicesSettings projectDevicesSettings){
    if (projectDevicesSettings != null){
      em.persist(projectDevicesSettings);
    }
  }

  public ProjectDevicesSettings readProjectDevicesSettings(Integer projectId) {
    TypedQuery<ProjectDevicesSettings> query = em.createNamedQuery(
      "ProjectDevicesSettings.findByProjectId", ProjectDevicesSettings.class);
    query.setParameter("projectId", projectId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void updateProjectDevicesSettings(ProjectDevicesSettings projectDevicesSettings) {
    ProjectDevicesSettings settings = em.find(ProjectDevicesSettings.class, projectDevicesSettings.getProjectId());
    settings.setJwtSecret(projectDevicesSettings.getJwtSecret());
    settings.setJwtTokenDuration(projectDevicesSettings.getJwtTokenDuration());
    em.persist(settings);
  }

  public void deleteProjectDevicesSettings(Integer projectId) {
    ProjectDevicesSettings projectDevicesSettings = readProjectDevicesSettings(projectId);
    if (projectDevicesSettings != null){
      em.remove(projectDevicesSettings);
    }
  }

  public void createProjectDevice(ProjectDevice projectDevice) {
    if (projectDevice != null){
      em.persist(projectDevice);
    }
  }

  public void createProjectDevice(Integer projectId, AuthDeviceDTO authDTO) {
    if (authDTO != null){
      ProjectDevicePK pdKey = new ProjectDevicePK(projectId, authDTO.getDeviceUuid());
      em.persist(new ProjectDevice(pdKey, authDTO.getPassword(), ProjectDevice.State.Pending, authDTO.getAlias()));
    }
  }

  public ProjectDevice readProjectDevice(Integer projectId, String deviceUuid) {
    ProjectDevicePK pdKey = new ProjectDevicePK(projectId, deviceUuid);
    TypedQuery<ProjectDevice> query = em.createNamedQuery(
      "ProjectDevice.findByProjectDevicePK", ProjectDevice.class);
    query.setParameter("projectDevicePK", pdKey);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<ProjectDeviceDTO> readProjectDevices(Integer projectId) {
    TypedQuery<ProjectDevice> query = em.createNamedQuery(
      "ProjectDevice.findByProjectId", ProjectDevice.class);
    query.setParameter("projectId", projectId);
    List<ProjectDevice> devices =  query.getResultList();
    List<ProjectDeviceDTO> devicesDTO = new ArrayList<>();
    for(ProjectDevice device: devices){
      devicesDTO.add(new ProjectDeviceDTO(
              device.getProjectDevicePK().getProjectId(),
              device.getProjectDevicePK().getDeviceUuid(),
              device.getAlias(),
              device.getCreatedAt(),
              device.getState().name(),
              device.getLastLoggedIn()));
    }
    return devicesDTO;
  }

  public List<ProjectDeviceDTO> readProjectDevices(Integer projectId, String state) {
    TypedQuery<ProjectDevice> query = em.createNamedQuery(
      "ProjectDevice.findByProjectIdAndState", ProjectDevice.class);
    query.setParameter("projectId", projectId);
    query.setParameter("state", ProjectDevice.State.valueOf(state).ordinal());
    List<ProjectDevice> devices =  query.getResultList();
    List<ProjectDeviceDTO> devicesDTO = new ArrayList<>();
    for(ProjectDevice device: devices){
      devicesDTO.add(new ProjectDeviceDTO(
        device.getProjectDevicePK().getProjectId(),
        device.getProjectDevicePK().getDeviceUuid(),
        device.getAlias(),
        device.getCreatedAt(),
        device.getState().name(),
        device.getLastLoggedIn()));
    }
    return devicesDTO;
  }

  public void updateProjectDevice(ProjectDeviceDTO projectDeviceDTO) {
    ProjectDevice projectDevice = em.find(ProjectDevice.class,
      new ProjectDevicePK(projectDeviceDTO.getProjectId(), projectDeviceDTO.getDeviceUuid()));
    projectDevice.setState(ProjectDevice.State.valueOf(projectDeviceDTO.getState()));
    projectDevice.setAlias(projectDeviceDTO.getAlias());
    em.persist(projectDevice);
  }

  public void updateProjectDeviceLastLoggedIn(Integer projectId, AuthDeviceDTO projectDeviceDTO) {
    ProjectDevice projectDevice = em.find(ProjectDevice.class,
      new ProjectDevicePK(projectId, projectDeviceDTO.getDeviceUuid()));
    projectDevice.setLastLoggedIn(new Date());
    em.persist(projectDevice);
  }

  public void updateProjectDevices(List<ProjectDeviceDTO> projectDevicesDTO) {
    for (ProjectDeviceDTO projectDeviceDTO: projectDevicesDTO){
      updateProjectDevice(projectDeviceDTO);
    }
  }

  public void deleteProjectDevice(Integer projectId, String deviceUuid) {
    ProjectDevice projectDevice = readProjectDevice(projectId, deviceUuid);
    if (projectDevice != null){
      em.remove(projectDevice);
    }
  }

}
