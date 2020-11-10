package org.exoplatform.webconferencing;

/**
 * The Class InvitedIdentity.
 */
public class InvitedIdentity {
  
  /** The identity. */
  private String identity;
  
  /** The type. */
  private String type;
  
  /**
   * Instantiates a new invited identity.
   */
  public InvitedIdentity() {
    
  }

  /**
   * Instantiates a new invited identity.
   *
   * @param identity the identity
   * @param type the type
   */
  public InvitedIdentity(String identity, String type) {
    this.identity = identity;
    this.type = type;
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
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the new type
   */
  public void setType(String type) {
    this.type = type;
  }

}
