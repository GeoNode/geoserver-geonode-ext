geoserver-geonode-ext
=====================

[![Join the chat at https://gitter.im/GeoNode/geoserver-geonode-ext](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/GeoNode/geoserver-geonode-ext?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

GeoNode extends GeoServer with certain JSON, REST, and security capabilities.

Security
--------

GeoServer delegates authentication and authorization to GeoNode.

When the GeoServer plugin sees a request, it attempts to authorize with
GeoNode:

- If the request has a valid `sessionid` cookie (this links to a user in
  GeoNode), GeoNode looks up the user's permissions and replies.

- If there are HTTP credentials in the request (via the `HTTP_AUTHORIZATION`
  header) and they match those configured in the `OGC_SETTINGS`,
  GeoNode replies that this user is a super-user.

- **Uploads** is a special case: here, GeoNode makes the original request
  using the `OGC_SETTINGS` credentials.

JSON
----

TODO

.. todo:: Document GeoServer GeoJSON extensions

REST
----

TODO

.. todo:: Document GeoServer REST extensions
