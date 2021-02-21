#!/bin/bash

gpg --quiet --batch --yes --decrypt --passphrase=${ENCRYPT_KEY} --output release/keystore.jks release/keystore.gpg
gpg --quiet --batch --yes --decrypt --passphrase=${ENCRYPT_KEY} --output release/serviceAccount.json release/serviceAccount.gpg
