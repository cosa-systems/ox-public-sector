# Overlay image: adds the `scheduled_mail` capability bundle to the OpenDesk OX
# middleware-public-sector image, exposing the App Suite "Send later" composer
# action. See README.md for the root-cause analysis.
#
# Why this works without the OX SDK or Debian pipeline:
#   * The runtime image already ships JDK 21 (/opt/java/openjdk) AND the OX API
#     jars this bundle imports (com.openexchange.capabilities.jar,
#     com.openexchange.config.cascade.jar, com.openexchange.osgi.jar, slf4j, the
#     Equinox OSGi framework) under /opt/open-xchange/bundles/ — so we compile
#     in-image against that classpath.
#   * A bundle is registered exactly like a Debian package install: drop the jar
#     in /opt/open-xchange/bundles/ and an `<name>@start` entry in
#     osgi/bundle.d/<pkg>/. At container start, open-xchange-cloud runs
#     `ox_update_cloud_config_ini config.ini config.ini.template bundle.d`, which
#     regenerates osgi.bundles to include it.
ARG BASE_IMAGE=registry.opencode.de/bmi/opendesk/components/supplier/open-xchange/images-mirror/middleware-public-sector:8.48.91

# ---- build stage: compile + jar the bundle in-image ------------------------
FROM ${BASE_IMAGE} AS build
USER root
ENV PATH="/opt/java/openjdk/bin:${PATH}"
WORKDIR /build
COPY src ./src
COPY META-INF ./META-INF
RUN set -eux; \
    CP="$(find /opt/open-xchange/bundles -name '*.jar' | tr '\n' ':')"; \
    mkdir -p bin; \
    find src -name '*.java' > sources.txt; \
    # -sourcepath src so javac never tries to (implicitly) compile .java
    # sources shipped inside classpath jars (e.g. joda-time); -implicit:none
    # belt-and-braces against the same.
    javac --release 21 -classpath "$CP" -sourcepath src -implicit:none \
        -d bin @sources.txt; \
    jar --create --file com.openexchange.mail.scheduled.capability.jar \
        --manifest META-INF/MANIFEST.MF -C bin .

# ---- final stage: layer the bundle onto a fresh copy of the base image ------
FROM ${BASE_IMAGE}
ARG BUNDLE=com.openexchange.mail.scheduled.capability
ARG PKG=open-xchange-mail-scheduled-capability
USER root
COPY --from=build --chown=open-xchange:open-xchange \
    /build/com.openexchange.mail.scheduled.capability.jar \
    /opt/open-xchange/bundles/com.openexchange.mail.scheduled.capability.jar
RUN set -eux; \
    mkdir -p "/opt/open-xchange/osgi/bundle.d/${PKG}"; \
    printf '/opt/open-xchange/bundles/%s.jar@start\n' "${BUNDLE}" \
        > "/opt/open-xchange/osgi/bundle.d/${PKG}/${BUNDLE}.ini"; \
    chown -R open-xchange:open-xchange "/opt/open-xchange/osgi/bundle.d/${PKG}"
USER open-xchange
