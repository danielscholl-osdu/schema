# Changelog

## [1.1.0](https://github.com/danielscholl-osdu/schema/compare/v1.0.0...v1.1.0) (2026-02-20)


### ✨ Features

* Add vendor-neutral OIDC authentication support for acceptance tests ([3089be8](https://github.com/danielscholl-osdu/schema/commit/3089be85dad821d9b33094569151d4fcb4a1ca26))
* Add vendor-neutral OIDC authentication support for acceptance tests ([8f308a4](https://github.com/danielscholl-osdu/schema/commit/8f308a49da9f08dea5ad84951b64c7b66cbbc162))
* **logging:** Add system schema audit logging with user details ([7470ef5](https://github.com/danielscholl-osdu/schema/commit/7470ef50d185d7b9f7f1ce9790eb8dd52e4a3e90))
* **schema-validation:** Handle version upgrades in JSON Patch paths under definitions ([a4bc9e8](https://github.com/danielscholl-osdu/schema/commit/a4bc9e8ccbf1a10357d114f6fc5855dba91b59ad))


### 🐛 Bug Fixes

* Aws sonarqube source fix ([b2e9e78](https://github.com/danielscholl-osdu/schema/commit/b2e9e78cd48f38b330b5dfd462efb7f990a55ade))
* Aws sonarqube source fix ([de88f4b](https://github.com/danielscholl-osdu/schema/commit/de88f4b8cb3606c187573fbc5bdea0bc39dacab0))
* Cve and spring-boot version bump ([ff985ea](https://github.com/danielscholl-osdu/schema/commit/ff985ea929b0f75ad6b65ce0bc7df250969e2c29))
* Cve and spring-boot version bump ([1c00449](https://github.com/danielscholl-osdu/schema/commit/1c00449c7628e112a06477f004a986c2b7a83c98))
* Not returning authority if already exists ([f489317](https://github.com/danielscholl-osdu/schema/commit/f4893177cc2cc357c9f3ca90fc3be5cd2bcb19d7))
* Not returning authority if already exists ([65d205e](https://github.com/danielscholl-osdu/schema/commit/65d205e773a57050794e30cfa97b1a685ce95264))
* Spring cves ([98a0d3a](https://github.com/danielscholl-osdu/schema/commit/98a0d3a53b7910c4f19033792d16516eda4ca03c))
* Spring cves ([125a95a](https://github.com/danielscholl-osdu/schema/commit/125a95ab9dc640ecae75ce8b8c2569fd57dc9268))
* Tomcat core and security crypto CVE ([81fd0f3](https://github.com/danielscholl-osdu/schema/commit/81fd0f3dbded7e685449db7a8aa12a75a3e469b8))
* Tomcat core and security crypto CVE ([386f4b2](https://github.com/danielscholl-osdu/schema/commit/386f4b25f254bfcdef40ca7dfab4c2be2b6634bc))
* **validation:** Correct regex pattern matching in schema operation handlers ([deb418b](https://github.com/danielscholl-osdu/schema/commit/deb418b1d4556777993199bdb867bda053c2da62))
* **validation:** Correct regex pattern matching in schema operation handlers ([c4a6b6d](https://github.com/danielscholl-osdu/schema/commit/c4a6b6d14b9cb7b216b55fc238aa62ffef98166b))


### 🔧 Miscellaneous

* Complete repository initialization ([91d0379](https://github.com/danielscholl-osdu/schema/commit/91d0379b61b2c21a27d94e5d548e367b73c2d2d7))
* Copy configuration and workflows from main branch ([2f2b3f3](https://github.com/danielscholl-osdu/schema/commit/2f2b3f38a89d237676ab1450a3905e6ad5ff13cb))
* Deleting aws helm chart ([188a89a](https://github.com/danielscholl-osdu/schema/commit/188a89a7b79ee69e69c7a71e5fc50ecc579c93b5))
* Deleting aws helm chart ([41949b5](https://github.com/danielscholl-osdu/schema/commit/41949b5b2af537453dd6b17ace6e6264542269c6))
* **deps:** Security dependency remediation - Spring Boot 3.5.8 and library updates ([6218bd4](https://github.com/danielscholl-osdu/schema/commit/6218bd4f478a00ee3cf5cd8266ff308ecb3c50c1))
* **deps:** Security dependency remediation - Spring Boot 3.5.8 and library updates ([f827402](https://github.com/danielscholl-osdu/schema/commit/f827402d2ec9ad354eaa65472479c249d8e9fa97))
* Fixing sonar issues ([521be0c](https://github.com/danielscholl-osdu/schema/commit/521be0c9bbaacf8928c23c559e6ed15626d4c3d8))
* Fixing sonar issues ([c668653](https://github.com/danielscholl-osdu/schema/commit/c668653d77b19a35aade1ee0ce26531ca31f6452))
* Removing helm copy from aws buildspec ([13f178b](https://github.com/danielscholl-osdu/schema/commit/13f178bdbdf22d25e5fd5fbaf166385563606117))
* Sync template updates ([d0afc03](https://github.com/danielscholl-osdu/schema/commit/d0afc03b98cc40c6826d6e1d1445767f951cc84c))


### ♻️ Code Refactoring

* **audit:** Encapsulate audit roles and request context in logging layer ([8680233](https://github.com/danielscholl-osdu/schema/commit/868023373a2f7550d9f3e850d06434838eeeb2b4))
* **audit:** Encapsulate audit roles and request context in logging layer ([bef8224](https://github.com/danielscholl-osdu/schema/commit/bef8224f9c1fcaf97eb08acdbb76e6947f36dc3d))
* **schema-store:** Replace existence check with transactional create-or-get logic ([2cd6c0e](https://github.com/danielscholl-osdu/schema/commit/2cd6c0ebc333249b6402dc2a61d72d2f9a372185))


### 🧪 Tests

* Add idempotent create tests for Authority, Source, and EntityType stores ([0247ccb](https://github.com/danielscholl-osdu/schema/commit/0247ccb5782e49bdd6e3008e515be33180b15f37))
* **schema:** Add tests for existing entity handling in store operations ([fc49098](https://github.com/danielscholl-osdu/schema/commit/fc4909872a38741fbd5165913a50537dac2c6931))


### 🔨 Build System

* Remove redundant version declarations from child modules ([b898358](https://github.com/danielscholl-osdu/schema/commit/b898358f1cb1da6b5bbc6e6b1fc21a08d2ca0e37))
* Remove redundant version declarations from child modules ([060b7d8](https://github.com/danielscholl-osdu/schema/commit/060b7d80147c19d73578135d862428fc2404f3ce))
