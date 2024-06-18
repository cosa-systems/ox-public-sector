# Element Conference Integration

|                                   |                                                     |
| --------------------------------- | --------------------------------------------------- |
| Story for original implementation | [PBSR-583](https://jira.open-xchange.com/browse/PBSR-583)
| Code repository                   | <https://gitlab.open-xchange.com/extensions/public-sector>
| Package(s)                        | `open-xchange-conference-element`
| Available since                   | 8.12
| Maintainers                       | Carsten Hoeger

## Configuration

The following configuration options are available. Note that the bundle is disabled by default.

| Key                                                    | Default                       | Description          |
| ------------------------------------------------------ | -------- | ----------------------- |
| `com.openexchange.conference.element.enabled` | `false` | Enable the integration |
| `com.openexchange.conference.element.meetingHostUrl` | *none* | URL of the element Meeting Bot |
| `com.openexchange.conference.element.authToken` | *none* | Authentication Token |
| `com.openexchange.conference.element.matrixLoginUrl` | *none* | URL to retrieve the user specific token |
| `com.openexchange.conference.element.matrixUuidClaimName` | *none* | claim name of the retrieve token to get the Matrix identifier from; will use the ox login name from the session in case not set |
