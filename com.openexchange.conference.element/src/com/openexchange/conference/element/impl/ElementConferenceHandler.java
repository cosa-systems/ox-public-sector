
/*
 * @copyright Copyright (c) Open-Xchange GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.conference.element.impl;

import static com.openexchange.chronos.common.CalendarUtils.hasExternalOrganizer;
import static com.openexchange.chronos.common.CalendarUtils.optExtendedParameterValue;
import static com.openexchange.chronos.provider.composition.IDMangling.getUniqueFolderIds;
import static com.openexchange.conference.element.impl.JsonUtils.getUserUuidClaimValue;
import static com.openexchange.conference.element.impl.N.getOAuthTokenFromSession;
import static com.openexchange.conference.element.impl.N.logger;
import static com.openexchange.conference.element.impl.N.notNull;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import org.dmfs.rfc5545.DateTime;
import org.slf4j.Logger;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Conference;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.service.CalendarEvent;
import com.openexchange.chronos.service.CalendarHandler;
import com.openexchange.chronos.service.DeleteResult;
import com.openexchange.chronos.service.TimestampedResult;
import com.openexchange.chronos.service.UpdateResult;
import com.openexchange.conference.element.ElementConfiguration;
import com.openexchange.conference.element.impl.dto.ElementMeeting;
import com.openexchange.conference.element.impl.dto.ExternalData;
import com.openexchange.conference.element.impl.dto.OXCalReference;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;
import com.openexchange.session.Session;

/**
 * {@link ElementConferenceHandler}
 *
 * @author <a href="mailto:carsten.hoeger@open-xchange.com">Carsten Hoeger</a>
 */
@NonNullByDefault
public class ElementConferenceHandler implements CalendarHandler, Reloadable {

    /**
     * MATRIX_TOKEN_SESSION_PARAMETER
     */
    private static final String MATRIX_TOKEN_SESSION_PARAMETER = "c.o.conference.element.matrixToken";

    private static final Logger LOG = logger(ElementConferenceHandler.class);

    /** The name of the type property parameter used in handled conferences */
    public static final String PARAMETER_TYPE  = "X-OX-TYPE";
    /** The name of the id property parameter used in handled conferences */
    public static final String PARAMETER_ID    = "X-OX-ID";
    public static final String CONFERENCE_TYPE = "element";

    private final HttpClientService httpClientService;

    private AtomicReference<ElementConfiguration> configRef;

    public ElementConferenceHandler(HttpClientService httpClientService, ConfigurationService configService) throws OXException {
        super();
        this.httpClientService = httpClientService;
        final ElementConfiguration config = ElementConfiguration.getConfig(configService);
        LOG.info("using configuration: {}", config);
        this.configRef = new AtomicReference<>(config);
    }

    @Override
    public void handle(@Nullable CalendarEvent event) {
        if (null == event) {
            return;
        }
        final ElementConfiguration config = configRef.get();
        if (!config.isEnabled()) {
            LOG.debug("ElementConferenceHandler not enabled");
            return;
        }
        LOG.debug("Event: {}", event.toString());

        final Session session = event.getSession();
        if (null == session) {
            LOG.error("unable to retrieve session for context={}, account={}", event.getContextId(), event.getAccountId());
            return;
        }

        final String matrixUserIdentifier = getMatrixUserIdentifier(config, session);
        if (null == matrixUserIdentifier) {
            return;
        }

        String matrixToken;
        {
            String tmpMatrixToken = (String) session.getParameter(MATRIX_TOKEN_SESSION_PARAMETER);
            if (null == tmpMatrixToken) {
                final MatrixClient mc = new MatrixClient(httpClient(config.getHttpClientId()), notNull(config.getMatrixLoginUrl()), notNull(config.getAuthToken()));
                try {
                    tmpMatrixToken = mc.getLoginToken(matrixUserIdentifier);
                    LOG.debug("storing matrix token {} into session", tmpMatrixToken);
                    session.setParameter(MATRIX_TOKEN_SESSION_PARAMETER, tmpMatrixToken);
                } catch (OXException e) {
                    LOG.error("unable to retrieve matrix token from {} for context={}, user={}", config.getMatrixLoginUrl(), event.getContextId(), session.getUserId());
                    return;
                }
            }
            matrixToken = tmpMatrixToken;
        }

        for (UpdateResult update : event.getUpdates()) {
            LOG.debug("UpdateResult: {}", update);
            final List<String> list = getCalendarFolders(event, notNull(update));
            if (null == list || list.isEmpty()) {
                // no folders found, so we are either not owner or any other problem occurred
                return;
            }
            // FIXME: moving an appointment to different calendar can result into a list of more than one folders...
            if (LOG.isDebugEnabled() && list.size() > 1) {
                LOG.debug("more than one folder, Calendar changed:");
                list.forEach(le -> LOG.warn(le));
            }
            // looks like the first item in the list is always the new/updated calendar
            handleUpdate(notNull(update), notNull(list.get(0)), matrixToken);
        }
        for (DeleteResult delete : event.getDeletions()) {
            final List<String> list = getCalendarFolders(event, notNull(delete));
            if (null == list || list.isEmpty()) {
                // no folders found, so we are either not owner or any other problem occurred
                return;
            }
            handleDelete(notNull(delete), matrixToken);
        }
    }

    /**
     * Get Matrix User Identifier. Will use the OX Login name from session in case MatrixUuidClaimName is not configured
     * or extract it from the access token as stored in the session using the configured MatrixUuidClaimName
     * 
     * @param config
     * @param session
     * @return the identifier or null in case of errors
     */
    private @Nullable String getMatrixUserIdentifier(final ElementConfiguration config, final Session session) {
        final String token = getOAuthTokenFromSession(session);
        if (null == token) {
            LOG.error("unable to retrieve oauth token from session for context={}, user={}", session.getContextId(), session.getUserId());
            return null;
        }
        final String matrixUserIdentifier;
        final String matrixUuidClaimName = config.getMatrixUuidClaimName();
        if (null != matrixUuidClaimName) {
            LOG.debug("reading matrix user id from claim \"{}\"", matrixUuidClaimName);
            matrixUserIdentifier = getUserUuidClaimValue(token, matrixUuidClaimName);
        } else {
            LOG.debug("reading matrix user id from session");
            matrixUserIdentifier = session.getLoginName();
        }
        return matrixUserIdentifier;
    }

    /**
     * Determine Calendar folders affected by the given result type (supports {@link UpdateResult} and {@link DeleteResult})
     * 
     * @param event
     * @param result
     * @return
     */
    private @Nullable List<String> getCalendarFolders(CalendarEvent event, TimestampedResult result) {
        final CalendarUser cu;
        if (result instanceof UpdateResult) {
            cu = ((UpdateResult) result).getUpdate().getCalendarUser();
        } else if (result instanceof DeleteResult) {
            cu = ((DeleteResult) result).getOriginal().getCalendarUser();
        } else {
            LOG.error("Unsupported type {}, this should not happen", result);
            return null;
        }
        int ownerId = cu.getEntity();
        LOG.debug("Owner of updated calendar: {}", ownerId);
        List<String> list = getAffectedFoldersOfOwnerId(event, ownerId);
        return list;
    }

    /**
     * Determine the affected list of folders of given Calendar owner (from {@link PushCalendarHandler}).
     * 
     * @param event
     * @param ownerId
     * @return
     */
    private static @Nullable List<String> getAffectedFoldersOfOwnerId(CalendarEvent event, int ownerId) {
        Map<Integer, List<String>> affectedFoldersPerUser = event.getAffectedFoldersPerUser();
        if (null == affectedFoldersPerUser || affectedFoldersPerUser.isEmpty()) {
            LOG.debug("no affected foldes found for event {}, with ownerId {}", event, ownerId);
            return null;
        }
        for (Entry<Integer, List<String>> entry : affectedFoldersPerUser.entrySet()) {
            final Integer entryKey = entry.getKey();
            if (entryKey == ownerId) {
                LOG.debug("affected folder id {} belongs to owner with id {}, returning unique folder ids", entryKey, ownerId);
                return getUniqueFolderIds(event.getAccountId(), entry.getValue());
            }
        }
        LOG.debug("no affected foldes found for event {}, with ownerId {}", event, ownerId);
        return null;
    }

    /**
     * @param update
     */
    private void handleUpdate(UpdateResult update, String folderId, String matrixToken) {
        if (hasExternalOrganizer(update.getOriginal())) {
            LOG.debug("No external organizer: {}", update.getOriginal());
            return;
        }
        List<Conference> originalConferences = getConferences(update.getOriginal());
        List<Conference> updateConferences = getConferences(update.getUpdate());

        if (originalConferences.isEmpty() && updateConferences.isEmpty()) {
            LOG.debug("no conferences to update");
            return;
        }

        if (originalConferences.isEmpty()) {
            LOG.debug("new appointment, nothing to update");
        } else if (updateConferences.isEmpty()) {
            LOG.debug("remove {}", originalConferences);
            delete(originalConferences, matrixToken);
        } else {
            // calculate diffs. Again, "added" is not relevant
            List<Conference> removed = new ArrayList<>(originalConferences);
            List<Conference> changed = new ArrayList<>();
            for (Conference upd : updateConferences) {
                removed.removeIf(c -> matches(c, upd));
                Optional<Conference> optional = originalConferences.stream().filter(c -> matches(c, upd)).findAny();
                if (optional.isPresent()) {
                    changed.add(optional.get());
                }
            }
            if (timeHasChanged(update)) {
                LOG.debug("update {}", changed);
                change(changed, notNull(update.getUpdate()), folderId, matrixToken);
            }
            if (!removed.isEmpty()) {
                LOG.debug("delete {}", removed);
                delete(removed, matrixToken);
            }
        }

    }

    /**
     * @param changed
     * @param update
     * @param event
     */
    private void change(List<Conference> conferences, Event updatedEvent, String folderId, String matrixToken) {
        final ElementConfiguration config = configRef.get();
        final ElementClient mec = new ElementClient(httpClient(config.getHttpClientId()), notNull(config.getMeetingHostUrl()), matrixToken);
        for (Conference conf : conferences) {
            final String roomId = optExtendedParameterValue(conf.getExtendedParameters(), PARAMETER_ID);
            if (null != roomId) {
                LOG.debug("updating meeting for room id {}", roomId);
                try {
                    mec.updateMeeting(ElementMeeting.builder()
                        .withTargetRoomId(roomId)
                        .withStartTime(toZonedDateTime(notNull(updatedEvent.getStartDate())))
                        .withEndTime(toZonedDateTime(notNull(updatedEvent.getEndDate())))
                        .withExternalData(ExternalData.builder()
                            .withIoDotOx(OXCalReference.builder()
                                .withId(notNull(Integer.toString(conf.getId()))) // FIXME???
                                .withFolder(folderId)
                                .build())
                            .build())
                        .build());
                } catch (OXException e) {
                    LOG.error(e.getMessage(), e);
                }
            } else {
                LOG.debug("no roomId found for {}, no meeting to update", PARAMETER_ID);
            }
        }
    }

    /**
     * @param dt
     * @return
     */
    private static ZonedDateTime toZonedDateTime(final DateTime dt) {
        final ZoneId timeZone;
        {
            final TimeZone tz = dt.getTimeZone();
            if (null != tz) {
                timeZone = tz.toZoneId();
            } else {
                timeZone = ZoneId.systemDefault();
            }
        }
        return notNull(ZonedDateTime.of(dt.getYear(), dt.getMonth() + 1, dt.getDayOfMonth(), dt.getHours(), dt.getMinutes(), dt.getSeconds(), 0, timeZone));
    }

    /**
     * @param delete
     */
    private void handleDelete(DeleteResult delete, String matrixToken) {
        Event original = delete.getOriginal();
        if (hasExternalOrganizer(original)) {
            LOG.debug("No external organizer: {}", original);
            return;
        }

        List<Conference> conferences = getConferences(original);
        delete(conferences, matrixToken);
    }

    /**
     * @param conferences
     * @param original
     * @param event
     */
    private void delete(List<Conference> conferences, String matrixToken) {
        final ElementConfiguration config = configRef.get();
        final ElementClient mec = new ElementClient(httpClient(config.getHttpClientId()), notNull(config.getMeetingHostUrl()), matrixToken);
        for (final Conference conference : conferences) {
            final String roomId = optExtendedParameterValue(conference.getExtendedParameters(), PARAMETER_ID);
            if (null != roomId) {
                try {
                    mec.deleteMeeting(roomId);
                } catch (OXException e) {
                    LOG.error(e.getMessage(), e);
                }
            } else {
                LOG.debug("{} not found, no meeting to delete", PARAMETER_ID);
            }
        }
    }

    private ManagedHttpClient httpClient(final String id) {
        return notNull(httpClientService.getHttpClient(id));
    }

    /**
     * Gets conferences of an event that are of type <i>element</i>.
     * 
     * @param event The event to get the conferences from
     * @return The conferences, or an empty list if there are none
     */
    private static List<Conference> getConferences(@Nullable Event event) {
        if (null == event) {
            return notNull(Collections.emptyList());
        }
        List<Conference> conferences = event.getConferences();
        if (null == conferences || conferences.isEmpty()) {
            return notNull(Collections.emptyList());
        }
        List<Conference> matchingConferences = new ArrayList<Conference>(conferences.size());
        for (Conference conference : event.getConferences()) {
            String conferenceType = optExtendedParameterValue(conference.getExtendedParameters(), PARAMETER_TYPE);
            if (Strings.isNotEmpty(conferenceType) && conferenceType.equals(CONFERENCE_TYPE)) {
                matchingConferences.add(conference);
            }
        }
        return matchingConferences;
    }

    /**
     * Checks, if the two given conferences match according to their zoom id
     *
     * @param conf1 The first conference
     * @param conf2 The second conference
     * @return true if the zoom ids match, false otherwise
     */
    private static boolean matches(@Nullable Conference conf1, @Nullable Conference conf2) {
        if (conf1 == null || conf2 == null) {
            return false;
        }

        String param1 = optExtendedParameterValue(conf1.getExtendedParameters(), PARAMETER_ID);
        return null != param1 && param1.equals(optExtendedParameterValue(conf2.getExtendedParameters(), PARAMETER_ID));
    }

    /**
     * Checks if start or end date of the event have changed.
     *
     * @param update The Update
     * @return true if start or end date have changed, false otherwise
     */
    private boolean timeHasChanged(UpdateResult update) {
        return update.getUpdatedFields().contains(EventField.START_DATE) || update.getUpdatedFields().contains(EventField.END_DATE);
    }

    @Override
    public Interests getInterests() {
        return notNull(Reloadables.interestsForProperties(ElementConfiguration.allProperties()));
    }

    @Override
    public void reloadConfiguration(@Nullable ConfigurationService configService) {
        if (configService == null) {
            return;
        }
        final ElementConfiguration newConfig;
        try {
            newConfig = ElementConfiguration.getConfig(configService);
            if (Objects.equals(newConfig, configRef.get())) {
                return;
            }
            configRef.getAndSet(newConfig);
            LOG.info("applied configuration changes");
        } catch (OXException e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
