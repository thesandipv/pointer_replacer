#!/bin/bash

gpg --passphrase=${ENCRYPT_KEY} --cipher-algo AES256 --symmetric --output release/keystore.gpg release/keystore.jks
gpg --passphrase=${ENCRYPT_KEY} --cipher-algo AES256 --symmetric --output release/serviceAccount.gpg release/serviceAccount.json
gpg --passphrase=${ENCRYPT_KEY} --cipher-algo AES256 --symmetric --output allusive/google-services.gpg allusive/google-services.json
