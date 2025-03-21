# Changelog

All notable changes to this project will be documented in this file.

## [2.1.11] - Official Release
### Added
- Added `C8oFsReplicateCancelOnDoublon` test.
- Added tests for `C8oExceptionMessages` and improved some classes.

### Changed
- Updated Travis configuration.
- Fixed compile warnings and used `CBL 1.4.1`.
- Added `try/catch` for handling specific exceptions.

---

## [2.1.10]
### Added
- Support for `Base64 put_attachments`.
- Improved document harmonization (multiple steps).

### Fixed
- Fixed `set testEndpoint` correctly.

---

## [2.1.9]
### Added
- License acceptance step.

### Changed
- Updated to `gradle 3.1.3`.
- Added support for `react-native-c8osdk`.

### Removed
- Unused PNG images.
- Removed unnecessary files.

---

## [2.1.8]
### Fixed
- Fixed `fs://.download_bulk` failing on Android 6 with Brocade LoadBalancer.

---

## [2.1.7]
### Added
- Web API to allow adding custom HTTP headers for all HTTP requests.

---

## [2.1.6]
### Changed
- FullSync now uses `trustAllCertificates` settings for self-signed HTTPS endpoints.

---

## [2.1.5]
### Fixed
- Fixed UTF-8 encoding issue for URL encoding.

### Added
- Increased timeout to `20 minutes` for launching the Android emulator.
- Added Travis CI configuration to allow `cloud.cli`.
- Set `gradlew chmod +x`.

---

## [2.1.4]
### Changed
- Disabled bintray tasks.
- Improved support of `fs://.download_bulk` feature.
- Throw error in case of http 500 error.

---

## [2.1.3]
### Added
- Implemented `fs://.download_bulk` feature.
- Improved support for `replicated views` in Client SDK.

### Fixed
- Fixed wrong endpointHost is no port in the endpoint.

---

## Notes
For more details, please refer to the commit history in the GitHub repository.
