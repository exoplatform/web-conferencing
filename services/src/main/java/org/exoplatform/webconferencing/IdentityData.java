package org.exoplatform.webconferencing;

/**
 * The Class IdentityData.
 */
public class IdentityData  {

  /** The name. */
  protected String id;

  /** The display name. */
  protected String displayName;

  /** The avatar url. */
  protected String avatarUrl;

  /** The type. */
  private String type;

  /**
   * Instantiates a new identity search result.
   *
   * @param id the id
   * @param displayName the display name
   * @param type the type
   * @param avatarUrl the avatar url
   */
  public IdentityData(String id, String displayName, String type, String avatarUrl) {
    this.id = id;
    this.displayName = displayName;
    this.type = type;
    this.avatarUrl = avatarUrl;
  }


  /**
   * Gets the id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }


  /**
   * Sets the id.
   *
   * @param id the new id
   */
  public void setId(String id) {
    this.id = id;
  }


  /**
   * Gets the display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }


  /**
   * Sets the display name.
   *
   * @param displayName the new display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }


  /**
   * Gets the avatar url.
   *
   * @return the avatar url
   */
  public String getAvatarUrl() {
    return avatarUrl;
  }


  /**
   * Sets the avatar url.
   *
   * @param avatarUrl the new avatar url
   */
  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
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