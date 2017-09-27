# Kolibri Cockpit Manage Android Secrets

## Initialize

You need to setup cryptex on your Android project after you have installed and configured fastlane.

```
$ fastlane add_plugin cryptex
$ cryptex init
```

# Setup secret files

`appbranch` must be branch name of the app/team/company which you will use later for signing applications.

## Import keystore file

```
cryptex --git_branch appbranch --type import --in keystore.jks --key android_keystore
```

## Import keystore credentials

```
cryptex --git_branch appbranch --type import_env --hash '{"KEYSTORE_PASSWORD":"pass", "KEYSTORE_ALIAS":"alias", "KEYSTORE_ALIAS_PASSWORD":"aliaspassword"}' --key android_keystore
```

## Import Google Play json credentials

```
cryptex --git_branch appbranch --type import --in googleplay.json --key android_google_play
```

## Export some file from Cryptex

```
cryptex --git_branch appbranch --type export --key file_key --out file_to_hide.txt
```

## Delete key

```
cryptex --git_branch appbranch --type delete --key file_key
```
