package se.kth.bbc.upload;

import java.util.HashMap;

/**
 * by fanxu
 */
public class ResumableInfoStorage {

  //Single instance
  private ResumableInfoStorage() {
  }
  private static ResumableInfoStorage sInstance;

  public static synchronized ResumableInfoStorage getInstance() {
    if (sInstance == null) {
      sInstance = new ResumableInfoStorage();
    }
    return sInstance;
  }

  //resumableIdentifier --  ResumableInfo
  private HashMap<String, ResumableInfo> mMap
          = new HashMap<String, ResumableInfo>();

  /**
   * Get ResumableInfo from mMap or Create a new one.
   * <p/>
   * @param resumableChunkSize
   * @param resumableTotalSize
   * @param resumableIdentifier
   * @param resumableFilename
   * @param resumableRelativePath
   * @param resumableFilePath
   * @param resumableTemplateId
   * @return
   */
  public synchronized ResumableInfo get(int resumableChunkSize,
          long resumableTotalSize,
          String resumableIdentifier, String resumableFilename,
          String resumableRelativePath, String resumableFilePath,
          int resumableTemplateId) {

    ResumableInfo info = mMap.get(resumableIdentifier);

    if (info == null) {
      info = new ResumableInfo();

      info.setResumableChunkSize(resumableChunkSize);
      info.setResumableTotalSize(resumableTotalSize);
      info.setResumableIdentifier(resumableIdentifier);
      info.setResumableFilename(resumableFilename);
      info.setResumableRelativePath(resumableRelativePath);
      info.setResumableFilePath(resumableFilePath);
      info.setResumableTemplateId(resumableTemplateId);

      mMap.put(resumableIdentifier, info);
    }
    return info;
  }

  /**
   * ResumableInfo
   * <p/>
   * @param info
   */
  public void remove(ResumableInfo info) {
    mMap.remove(info.getResumableIdentifier());
  }
}
