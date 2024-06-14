package org.exoplatform.webconferencing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * The Class InviteEntity.
 */
@Entity(name = "WebConfInvite")
@ExoEntity
@Table(name = "WBC_INVITES")
@NamedQueries({ 
  @NamedQuery(name = "WebConfInvite.deleteCallInvites", query = "DELETE FROM WebConfInvite WHERE callId = :callId"),
  @NamedQuery(name = "WebConfInvite.findCallInvites", query = "SELECT i FROM WebConfInvite i WHERE i.callId = :callId ORDER BY i.identity")
})
@IdClass(InviteId.class)
public class InviteEntity {

  /** The Constant USER_TYPE. */
  public static final String USER_TYPE          = "user";

  /** The Constant GROUP_TYPE. */
  public static final String GROUP_TYPE         = "group";

  /** The Constant EXTERNAL_USER_TYPE. */
  public static final String EXTERNAL_USER_TYPE = "external_user";

  /** The call id. */
  @Id
  @Column(name = "CALL_ID")
  protected String           callId;

  /** The identity. */
  @Column(name = "IDENTITY")
  @Id
  protected String           identity;

  /** The identity type. */
  @Column(name = "IDENTITY_TYPE")
  protected String           identityType;

  /** The invitation id. */
  @Column(name = "INVITATION_ID")
  protected String           invitatationId;

  /**
   * Instantiates a new invite entity.
   */
  public InviteEntity() {

  }

  /**
   * Instantiates a new invite entity.
   *
   * @param callId the call id
   * @param identity the identity
   * @param identityType the identity type
   * @param invitationId the invitation id
   */
  public InviteEntity(String callId, String identity, String identityType, String invitationId) {
    this.callId = callId;
    this.identity = identity;
    this.identityType = identityType;
    this.invitatationId = invitationId;
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
   * Gets the identity type.
   *
   * @return the identity type
   */
  public String getIdentityType() {
    return identityType;
  }

  /**
   * Sets the identity type.
   *
   * @param identityType the new identity type
   */
  public void setIdentityType(String identityType) {
    this.identityType = identityType;
  }

  /**
   * Gets the invitation id.
   *
   * @return the invitation id
   */
  public String getInvitationId() {
    return invitatationId;
  }

  /**
   * Sets the invitation id.
   *
   * @param invitationId the new invitation id
   */
  public void setInvitationId(String invitationId) {
    this.invitatationId = invitationId;
  }

}
