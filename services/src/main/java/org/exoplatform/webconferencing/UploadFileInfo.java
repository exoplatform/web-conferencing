package org.exoplatform.webconferencing;

import java.util.List;

/**
 * The Class UploadFileInfo.
 */
public class UploadFileInfo {

  /** The call id. */
  private final String       callId;

  /** The identity. */
  private final String       identity;

  /** The is space. */
  private final String      type;

  /** The user. */
  private final String       user;

  /** The participants. */
  private final List<String> participants;

  /**
   * Instantiates a new upload file info.
   *
   * @param callId the call id
   * @param identity the identity
   * @param type the is type
   * @param user the user
   * @param participants the participants
   */
  public UploadFileInfo(String callId, String identity, String type, String user, List<String> participants) {
    this.callId = callId;
    this.identity = identity;
    this.type = type;
    this.user = user;
    this.participants = participants;
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
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Gets the user.
   *
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * Gets the participants.
   *
   * @return the participants
   */
  public List<String> getParticipants() {
    return participants;
  }

}
