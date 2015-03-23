/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.upload;

import java.io.*;
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
   * <p>
   * @param resumableChunkSize
   * @param resumableTotalSize
   * @param resumableIdentifier
   * @param resumableFilename
   * @param resumableRelativePath
   * @param resumableFilePath
   * @return
   */
  public synchronized ResumableInfo get(int resumableChunkSize,
          long resumableTotalSize,
          String resumableIdentifier, String resumableFilename,
          String resumableRelativePath, String resumableFilePath) {

    ResumableInfo info = mMap.get(resumableIdentifier);

    if (info == null) {
      info = new ResumableInfo();

      info.resumableChunkSize = resumableChunkSize;
      info.resumableTotalSize = resumableTotalSize;
      info.resumableIdentifier = resumableIdentifier;
      info.resumableFilename = resumableFilename;
      info.resumableRelativePath = resumableRelativePath;
      info.resumableFilePath = resumableFilePath;

      mMap.put(resumableIdentifier, info);
    }
    return info;
  }

  /**
   * ÉŸ³ýResumableInfo
   * <p>
   * @param info
   */
  public void remove(ResumableInfo info) {
    mMap.remove(info.resumableIdentifier);
  }
}
