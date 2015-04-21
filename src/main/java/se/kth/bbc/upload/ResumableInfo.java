package se.kth.bbc.upload;

import java.io.File;
import java.util.HashSet;

/**
 * @author fanxu;
 * @author Stig
 */
public class ResumableInfo {

  public int resumableChunkSize;
  public long resumableTotalSize;
  public String resumableIdentifier;
  public String resumableFilename;
  public String resumableRelativePath;
  private long uploadedContentLength = 0;

  public static class ResumableChunkNumber {

    public ResumableChunkNumber(int number) {
      this.number = number;
    }

    public int number;

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ResumableChunkNumber
              ? ((ResumableChunkNumber) obj).number == this.number : false;
    }

    @Override
    public int hashCode() {
      return number;
    }
  }

  //Chunks uploaded. Private to enable atomically add and check if finished
  private HashSet<ResumableChunkNumber> uploadedChunks = new HashSet<>();

  public String resumableFilePath;

  public boolean vaild() {
    if (resumableChunkSize < 0 || resumableTotalSize < 0
            || HttpUtils.isEmpty(resumableIdentifier)
            || HttpUtils.isEmpty(resumableFilename)
            || HttpUtils.isEmpty(resumableRelativePath)) {
      return false;
    } else {
      return true;
    }
  }

  private boolean checkIfUploadFinished() {
    if (uploadedContentLength != resumableTotalSize) {
      return false;
    }

    //Upload finished, change filename.
    File file = new File(resumableFilePath);
    String new_path = file.getAbsolutePath().substring(0,
            file.getAbsolutePath().length() - ".temp".length());
    file.renameTo(new File(new_path));
    return true;
  }

  /**
   * Add the chunk <i>rcn</i> to the uploaded chunks and check if upload
   * has finished. Upon upload, change file name. Synchronized method to enable
   * atomic checking.
   * <p>
   * @return true if finished.
   */
  public synchronized boolean addChuckAndCheckIfFinished(
          ResumableChunkNumber rcn, long contentLength) {
    if (!uploadedChunks.contains(rcn)) {
      uploadedContentLength += contentLength;
    }
    uploadedChunks.add(rcn);
    return checkIfUploadFinished();
  }

  /**
   * Check if the resumable chunk has been uploaded.
   * <p>
   * @param rcn
   * @return
   */
  public boolean isUploaded(ResumableChunkNumber rcn) {
    return uploadedChunks.contains(rcn);
  }

}
