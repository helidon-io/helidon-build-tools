# Helidon CLI Release

The CLI is currently released to `HomeBrew` and `SDKMan!`. This document explain the steps to release a new version.

## HomeBrew

HomeBrew Core has a Helidon Formula to allow every user to easily install the CLI. To release a new version, 
update the `url` configuration key to the new version. The Formula has some basic testing to make sure the CLI can generate
project. Nevertheless, for more test coverage, it is possible to test the updated Formula locally. Update the formula in 
your local Homebrew (`homebrew/Library/Taps/homebrew/homebrew-core/h/helidon.rb`) and use this command:

```shell
export HOMEBREW_NO_INSTALL_FROM_API=1
brew install --build-from-source --verbose helidon
```

Formula: https://github.com/Homebrew/homebrew-core/blame/master/Formula/h/helidon.rb

## SDKMan!

Use SDKMan REST endpoint to publish the new released binaries with our credentials available in the Vault.

1. Release a new candidate

```shell
curl -X POST \
-H "Consumer-Key: CONSUMER_KEY" \
-H "Consumer-Token: CONSUMER_TOKEN" \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-d '{"candidate": "helidon", "version": "${version}", "url": "https://github.com/helidon-io/helidon-build-tools/releases/download/${version}/helidon-cli.zip"}' \
https://vendors.sdkman.io/release
```
The release endpoint support `POST` and `DELETE` HTTP methods. Releasing a new candidate does not make it the default
version. It is considered a minor release, setting the new candidate as a default version is major release. 

2. Set existing Version as Default for Candidate (Optional)

Use the following command to set a version as default.

```shell
curl -X PUT \
-H "Consumer-Key: CONSUMER_KEY" \
-H "Consumer-Token: CONSUMER_TOKEN" \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-d '{"candidate": "helidon", "version": "${version}"}' \
https://vendors.sdkman.io/default
```

3. Broadcast a Structured Message (Optional)

Announce the new release through SDKMan social media (X feed) and broadcast channel of SDKMAN! CLI. 

```shell
curl -X POST \
-H "Consumer-Key: CONSUMER_KEY" \
-H "Consumer-Token: CONSUMER_TOKEN" \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-d '{"candidate": "helidon", "version": "${version}", "url": "https://helidon.io"}' \
https://vendors.sdkman.io/announce/struct
```
`url`: display the url where user can find the binaries (or any other useful information).
The broadcast message looks like this `helidon ${version} available for download. https://helidon.io`

