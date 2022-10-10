#! /bin/bash

# Note that this function takes certificate strings as parameters
# and not the file names of certificate files!  Also note that when
# dealing with a certificate string, you should always quote the
# variable to preserve the newlines.
fingerprint() {
    local cert="$1"
    if [ -n "$cert" ]; then
        echo "$cert" | openssl x509 -noout -fingerprint | cut -d= -f2
    else
        echo ""
    fi
}

cert_from_pkcs12() {
    local pkcs12="$1"
    local name="$2"
    local password="${3:-password}"
    if [ -e "$pkcs12" ]; then
      # PKCS12 prints a bunch of junk at the top of the output that we don't want.
      openssl pkcs12 -in "$pkcs12" -name "$name" -passin pass:$password -nokeys -nomacver | \
        awk -v RS='\0' '{print substr($0, match($0, /-----BEGIN.*/))}'
    else
        echo ""
    fi
}

fp_pkcs12() {
    echo "$(fingerprint "$(cert_from_pkcs12 "$@")")"
}

./cpsetup

# Use certs to generate key store needed for SSL protocol on port 8443
# TODO: All of these steps should detect if the the keystore generation is needed first so it is not generated every
#   time the container is restarted
echo password > /etc/candlepin/certs/tc_keystore_pass.txt
openssl pkcs12 -export -in /etc/candlepin/certs/candlepin-ca.crt -inkey /etc/candlepin/certs/candlepin-ca.key -out /etc/candlepin/certs/tc_keystore.jks -name tomcat -CAfile /etc/candlepin/certs/candlepin-ca.crt -caname root -chain -password file:/etc/candlepin/certs/tc_keystore_pass.txt
/usr/lib/jvm/jre-11-openjdk/bin/keytool -keystore /etc/candlepin/certs/tc_keystore.jks -noprompt -importcert -storepass:file /etc/candlepin/certs/tc_keystore_pass.txt -alias candlepin_ca -file /etc/candlepin/certs/candlepin-ca.crt

fp_pkcs12 /etc/candlepin/certs/tc_keystore.jks "tomcat" "password"
ln -s /etc/candlepin/certs /opt/webserver/conf/certs

# We could restart/run tomcat at this point and then execute a tail -f on the candlepin logs.
# This would give a better output when using 'podman logs <candlepin container>'' command.
exec "/opt/webserver/bin/catalina.sh" run