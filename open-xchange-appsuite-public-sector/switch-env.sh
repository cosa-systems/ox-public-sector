#!/bin/sh
set -e

DOMAIN="$1.at-univention.de"
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
