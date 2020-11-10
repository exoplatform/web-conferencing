package org.exoplatform.webconferencing.domain;

import java.io.Serializable;

/**
 * The Class InviteId.
 */
public class InviteId implements Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -6212210915304512100L;

  /** The call id. */
  protected String          callId;

  /** The identity. */
  protected String          identity;

  /**
   * Instantiates a new invite id.
   */
  public InviteId() {
  }

  /**
   * Instantiates a new invite id.
   *
   * @param callId the call id
   * @param identity the identity
   */
  public InviteId(String callId, String identity) {
    this.callId = callId;
    this.identity = identity;
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
   * Sets the call id.
   *
   * @param callId the new call id
   */
  public void setCallId(String callId) {
    this.callId = callId;
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
   * Sets the identity.
   *
   * @param identity the new identity
   */
  public void setIdentity(String identity) {
    this.identity = identity;
  }

  /**
   * Hash code.
   *
   * @return the int
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((callId == null) ? 0 : callId.hashCode());
    result = prime * result + ((identity == null) ? 0 : identity.hashCode());
    return result;
  }

  /**
   * Equals.
   *
   * @param obj the obj
   * @return true, if successful
   */
  @Override
  public boolean equals(Object obj) {
    if (obj != null) {
      if (InviteId.class.isAssignableFrom(obj.getClass())) {
        InviteId other = InviteId.class.cast(obj);
        return callId.equals(other.getCallId()) && identity.equals(other.getIdentity());
      }
    }
    return false;
  }

}
