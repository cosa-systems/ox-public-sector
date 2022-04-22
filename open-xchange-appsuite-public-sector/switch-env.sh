#!/bin/sh
DOMAIN="$1.at-univention.de"
sed 's#"server": ".*",$#"server": "https://webmail.'"$DOMAIN"'/appsuite/",#' \
    -i grunt/local.conf.json
cd ssl
rm -f host.*
ln -s "/home/vp/dev/vm/traefik/CA/pki/issued/portal.$DOMAIN.crt" host.crt
ln -s "/home/vp/dev/vm/traefik/CA/pki/private/portal.$DOMAIN.key" host.key
