#!/bin/sh
set -e

if [ -z "$1" ]; then
    echo -n "Current environment: "
    sed -nre '/"server":/s/^.*webmail\.(.*)\.souvap-univention.de.*$/\1/p' \
        grunt/local.conf.json
    exit
fi

DOMAIN="$1.souvap-univention.de"
CA="/home/vp/dev/vm/traefik/CA"

if [ ! -f "$CA/pki/issued/portal.$DOMAIN.crt" ]; then
    (
        cd "$CA"
        ./easyrsa --subject-alt-name="DNS:portal.$DOMAIN,DNS:*.$DOMAIN" \
            build-server-full "portal.$DOMAIN" nopass
    )
fi

sed 's#"server": ".*",$#"server": "https://webmail.'"$DOMAIN"'/appsuite/",#' \
    -i grunt/local.conf.json
cd ssl
rm -f host.*
ln -s "$CA/pki/issued/portal.$DOMAIN.crt" host.crt
ln -s "$CA/pki/private/portal.$DOMAIN.key" host.key
