package org.exoplatform.webconferencing;

/**
 * The Class InvitedIdentity.
 */
public class InvitedIdentity {
  
  /** The identity. */
  private String id;
  
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
   * @param id the identity
   * @param type the type
   */
  public InvitedIdentity(String id, String type) {
    this.id = id;
    this.type = type;
  }

  /**
   * Gets the identity.
   *
   * @return the identity
   */
  public String getIdentity() {
    return id;
  }

  /**
   * Sets the identity.
   *
   * @param id the new identity
   */
  public void setIdentity(String id) {
    this.id = id;
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
