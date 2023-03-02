## CLI publication Todo list

This documentation aims to describe the steps to release the Helidon CLI.

# SDKMan

Helidon team possesses credentials to access `SDKMan` REST API to do the following actions:

* **Release a new version** - process a minor release, the default version is not set to the new candidate.
The release endpoint is `https://vendors.sdkman.io/release` and support `POST`, `PATCH` and `DELETE`.
Payload example:
```bash
curl -X POST \
-H "Consumer-Key: CONSUMER_KEY" \
-H "Consumer-Token: CONSUMER_TOKEN" \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-d '{"candidate": "helidon", "version": "3.0.4", "url": "https://repo1.maven.org/maven2/io/helidon/build-tools/cli/helidon-cli-impl/3.0.4/helidon-cli-impl-3.0.4.zip"}' \
https://vendors.sdkman.io/release
```
* **Set the default version** - Example to set version as default for candidate:
```bash
curl -X PUT \
-H "Consumer-Key: CONSUMER_KEY" \
-H "Consumer-Token: CONSUMER_TOKEN" \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-d '{"candidate": "helidon", "version": "3.0.4"}' \
https://vendors.sdkman.io/default
```
* **Broadcast a release message** - This will result in a structured message announcement on social media and SDKMAN! CLI.
Example:
```bash
curl -X POST \
-H "Consumer-Key: CONSUMER_KEY" \
-H "Consumer-Token: CONSUMER_TOKEN" \
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-d '{"candidate": "helidon", "version": "3.0.4", "url": "https://helidon.io"}' \
https://vendors.sdkman.io/announce/struct
```

# HomeBrew

Update the `HomeBrew` formula from `homebrew/homebrew-core` to release a new Helidon CLI version. 


## Useful links

* SDKMan: https://sdkman.io/vendors
* HomeBrew: https://docs.brew.sh/