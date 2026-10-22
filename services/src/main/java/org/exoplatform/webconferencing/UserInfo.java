
/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.webconferencing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Represent eXo user as a conversation participant for calls.
 * 
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserInfo.java 00000 Feb 23, 2017 pnedonosko $
 */
public class UserInfo extends IdentityInfo {

  /** The Constant TYPE_NAME. */
  public static final String TYPE_NAME = "user".intern();

  /** The longest list of client ids kept, the size of the participant's client id column. */
  public static final int    CLIENT_IDS_MAX_LENGTH = 255;

  /** The separator of the client ids in the list. */
  public static final String CLIENT_IDS_SEPARATOR  = ",";

  /** The mark of the client the user started the call from, in the list of clients. */
  public static final String STARTER_CLIENT_PREFIX = "*";

  /** The Constant LOG. */
  protected static final Log LOG       = ExoLogger.getLogger(UserInfo.class);

  /**
   * The Class IMInfo.
   */
  public static class IMInfo {

    /** The type. */
    private final String type;

    /** The id. */
    private final String id;

    /**
     * Instantiates a new IM info.
     *
     * @param type the type
     * @param id the id
     */
    protected IMInfo(String type, String id) {
      super();
      this.type = type;
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
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
      return id;
    }
  }

  /** The first name. */
  private final String                    firstName;

  /** The last name. */
  private final String                    lastName;

  /** The IM accounts. */
  private final Map<String, List<IMInfo>> imAccounts = new HashMap<String, List<IMInfo>>();

  /** The state. */
  private String                          state;

  /** The ids of the user's clients in the call, separated by {@link #CLIENT_IDS_SEPARATOR}. */
  private String                          clientId;

  /**
   * Instantiates a new user info.
   *
   * @param userId the user id in the system
   * @param firstName the first type
   * @param lastName the last type
   */
  public UserInfo(String userId, String firstName, String lastName) {
    super(userId, new StringBuffer(firstName).append(' ').append(lastName).toString());
    this.firstName = firstName;
    this.lastName = lastName;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isGroup() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return TYPE_NAME;
  }

  /**
   * Gets the first name.
   *
   * @return the firstName
   */
  public String getFirstName() {
    return firstName;
  }

  /**
   * Gets the last name.
   *
   * @return the lastName
   */
  public String getLastName() {
    return lastName;
  }

  /**
   * Gets the im accounts.
   *
   * @return the map of user instant messaging accounts: key is a type, value is a collection of
   *         {@link IMInfo}
   */
  public Map<String, Collection<IMInfo>> getImAccounts() {
    return Collections.unmodifiableMap(imAccounts);
  }

  /**
   * Adds the user IM account.
   *
   * @param type the IM type type (e.g. <code>gtalk</code>)
   * @param id the IM id
   * @return the added IM info
   */
  public IMInfo addImAccount(String type, String id) {
    IMInfo im = new IMInfo(type, id);
    imAccounts.computeIfAbsent(type, list -> new ArrayList<IMInfo>()).add(im);
    return im;
  }

  /**
   * Adds the user IM account(s). Note that if IM with some type already added an another one will be skipped.
   *
   * @param ims the IM account(s) to add to the user profile
   */
  public void addImAccount(IMInfo... ims) {
    for (IMInfo im : ims) {
      imAccounts.computeIfAbsent(im.getType(), list -> new ArrayList<IMInfo>()).add(im);
    }
  }

  /**
   * Checks for IM account associated with the user.
   *
   * @param type the IM account type
   * @return <code>true</code>, if successful
   */
  public boolean hasImAccount(String type) {
    return imAccounts.containsKey(type);
  }

  /**
   * Gets the IM accounts by type.
   *
   * @param type the type
   * @return the IM accounts
   */
  public Collection<IMInfo> getImAccount(String type) {
    return Collections.unmodifiableCollection(imAccounts.get(type));
  }

  /**
   * Gets the state.
   *
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * Sets the state.
   *
   * @param state the new state
   */
  public void setState(String state) {
    this.state = state;
  }

  /**
   * Gets the clients of the user in the call, as stored: one id, or several separated by
   * {@link #CLIENT_IDS_SEPARATOR}. The client the user started the call from carries
   * {@link #STARTER_CLIENT_PREFIX}: it is the page that started the call, which the user is not in the call
   * from until it joins it.
   *
   * @return the client ids, or <code>null</code> when the user has no client in the call
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Sets the clients of the user in the call, replacing the stored ones.
   *
   * @param clientId the client ids, as {@link #getClientId()} returns them, or <code>null</code> for none
   */
  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  /**
   * Sets the client the user started the call from as their only client.
   *
   * @param clientId the client id; <code>null</code>, an empty id, or one {@link #addClientId(String)} ignores,
   *          leaves the user with no client
   */
  public void setStarterClientId(String clientId) {
    this.clientId = isValidClientId(clientId) ? STARTER_CLIENT_PREFIX + clientId : null;
  }

  /**
   * Adds a client the user is in the call from. The client the user started the call from becomes one they are
   * in the call from when it joins it. When the list would exceed {@link #CLIENT_IDS_MAX_LENGTH}, its oldest
   * clients are dropped: a client whose leave never came, after a crash or a network loss, goes first.
   *
   * @param clientId the client id, ignored when <code>null</code>, empty, already in the list, too long to fit
   *          the column once marked as the starter, or holding {@link #CLIENT_IDS_SEPARATOR} or starting with
   *          {@link #STARTER_CLIENT_PREFIX}
   */
  public void addClientId(String clientId) {
    if (!isValidClientId(clientId)) {
      return;
    }
    List<String> clientIds = clientIds();
    int starter = clientIds.indexOf(STARTER_CLIENT_PREFIX + clientId);
    if (starter >= 0) {
      clientIds.set(starter, clientId);
    } else if (clientIds.contains(clientId)) {
      return;
    } else {
      clientIds.add(clientId);
      while (clientIds.size() > 1 && String.join(CLIENT_IDS_SEPARATOR, clientIds).length() > CLIENT_IDS_MAX_LENGTH) {
        clientIds.remove(0);
      }
    }
    this.clientId = String.join(CLIENT_IDS_SEPARATOR, clientIds);
  }

  /**
   * Removes a client of the user, the one they started the call from included.
   *
   * @param clientId the client id
   * @return true, if the client was in the list
   */
  public boolean removeClientId(String clientId) {
    List<String> clientIds = clientIds();
    boolean removed = clientIds.removeIf(id -> unmarked(id).equals(clientId));
    if (removed) {
      this.clientId = clientIds.isEmpty() ? null : String.join(CLIENT_IDS_SEPARATOR, clientIds);
    }
    return removed;
  }

  /**
   * Tells if the user is in the call from at least one client, the one they started it from not counted.
   *
   * @return true, if a client joined the call
   */
  public boolean hasJoinedClientIds() {
    return clientIds().stream().anyMatch(id -> !id.startsWith(STARTER_CLIENT_PREFIX));
  }

  /**
   * Checks if the given client is one of the user's clients in the call, the one they started it from included.
   *
   * @param clientId the client id
   * @return true, if the client is in the list
   */
  public boolean hasSameClientId(String clientId) {
    return clientId != null && clientIds().stream().anyMatch(id -> unmarked(id).equals(clientId));
  }

  private List<String> clientIds() {
    List<String> clientIds = new ArrayList<>();
    if (clientId != null) {
      for (String id : clientId.split(CLIENT_IDS_SEPARATOR)) {
        if (!id.isEmpty()) {
          clientIds.add(id);
        }
      }
    }
    return clientIds;
  }

  private static boolean isValidClientId(String clientId) {
    return clientId != null && !clientId.isEmpty() && clientId.length() < CLIENT_IDS_MAX_LENGTH
        && !clientId.contains(CLIENT_IDS_SEPARATOR)
        && !clientId.startsWith(STARTER_CLIENT_PREFIX);
  }

  private static String unmarked(String id) {
    return id.startsWith(STARTER_CLIENT_PREFIX) ? id.substring(STARTER_CLIENT_PREFIX.length()) : id;
  }

}
