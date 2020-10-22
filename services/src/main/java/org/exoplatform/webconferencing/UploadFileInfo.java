package org.exoplatform.webconferencing;

/**
 * The Class UploadFileInfo.
 */
public class UploadFileInfo {

  /** The call id. */
  private final String  callId;

  /** The identity. */
  private final String  identity;

  /** The is space. */
  private final boolean isSpace;

  /** The user. */
  private final String  user;

  /**
   * Instantiates a new upload file info.
   *
   * @param callId the call id
   * @param identity the identity
   * @param isSpace the is space
   * @param user the user
   */
  public UploadFileInfo(String callId, String identity, boolean isSpace, String user) {
    this.callId = callId;
    this.identity = identity;
    this.isSpace = isSpace;
    this.user = user;
  }

  /**
   * Gets the call id.
   *
   * @return the call id
   */
  public String getCallId() {
    return callId;
  }

  /**
   * Gets the identity.
   *
   * @return the identity
   */
  public String getIdentity() {
    return identity;
  }

  /**
   * Checks if is space.
   *
   * @return true, if is space
   */
  public boolean isSpace() {
    return isSpace;
  }

  /**
   * Gets the user.
   *
   * @return the user
   */
  public String getUser() {
    return user;
  }

}
