# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.0 (2025-07-13)


### 🐛 Bug Fixes

* Spring cves ([98a0d3a](https://github.com/danielscholl-osdu/schema/commit/98a0d3a53b7910c4f19033792d16516eda4ca03c))
* Spring cves ([125a95a](https://github.com/danielscholl-osdu/schema/commit/125a95ab9dc640ecae75ce8b8c2569fd57dc9268))
* Tomcat core and security crypto CVE ([81fd0f3](https://github.com/danielscholl-osdu/schema/commit/81fd0f3dbded7e685449db7a8aa12a75a3e469b8))
* Tomcat core and security crypto CVE ([386f4b2](https://github.com/danielscholl-osdu/schema/commit/386f4b25f254bfcdef40ca7dfab4c2be2b6634bc))


### 🔧 Miscellaneous

* Complete repository initialization ([5129a8c](https://github.com/danielscholl-osdu/schema/commit/5129a8c052ec9999f9c9d058aa5db7a53bd771f6))
* Copy configuration and workflows from main branch ([23df2ef](https://github.com/danielscholl-osdu/schema/commit/23df2efa0a667b019e4e8318964cfde619fe8c16))
* Deleting aws helm chart ([188a89a](https://github.com/danielscholl-osdu/schema/commit/188a89a7b79ee69e69c7a71e5fc50ecc579c93b5))
* Deleting aws helm chart ([41949b5](https://github.com/danielscholl-osdu/schema/commit/41949b5b2af537453dd6b17ace6e6264542269c6))
* Removing helm copy from aws buildspec ([13f178b](https://github.com/danielscholl-osdu/schema/commit/13f178bdbdf22d25e5fd5fbaf166385563606117))

## [2.0.0] - Major Workflow Enhancement & Documentation Release

### ✨ Features
- **Comprehensive MkDocs Documentation Site**: Complete documentation overhaul with GitHub Pages deployment
- **Automated Cascade Failure Recovery**: System automatically recovers from cascade workflow failures
- **Human-Centric Cascade Pattern**: Issue lifecycle tracking with human notifications for critical decisions
- **Integration Validation**: Comprehensive validation system for cascade workflows
- **Claude Workflow Integration**: Full Claude Code CLI support with Maven MCP server integration
- **GitHub Copilot Enhancement**: Java development environment setup and firewall configuration
- **Fork Resources Staging Pattern**: Template-based staging for fork-specific configurations
- **Conventional Commits Validation**: Complete validation system with all supported commit types
- **Enhanced PR Label Management**: Simplified production PR labels with automated issue closure
- **Meta Commit Strategy**: Advanced release-please integration for better version management
- **Push Protection Handling**: Sophisticated upstream secrets detection and resolution workflows

### 🔨 Build System
- **Workflow Separation Pattern**: Template development vs. fork instance workflow isolation
- **Template Workflow Management**: 9 comprehensive template workflows for fork management
- **Enhanced Action Reliability**: Improved cascade workflow trigger reliability with PR event filtering
- **Base64 Support**: Enhanced create-enhanced-pr action with encoding capabilities

### 📚 Documentation
- **Structured MkDocs Site**: Complete documentation architecture with GitHub Pages
- **AI-First Development Docs**: Comprehensive guides for AI-enhanced development
- **ADR Documentation**: 20+ Architectural Decision Records covering all major decisions
- **Workflow Specifications**: Detailed documentation for all 9 template workflows
- **Streamlined README**: Focused quick-start guide directing to comprehensive documentation

### 🛡️ Security & Reliability
- **Advanced Push Protection**: Intelligent handling of upstream repositories with secrets
- **Branch Protection Integration**: Automated branch protection rule management
- **Security Pattern Recognition**: Enhanced security scanning and pattern detection
- **MCP Configuration**: Secure Model Context Protocol integration for AI development

### 🔧 Workflow Enhancements
- **Cascade Monitoring**: Advanced cascade workflow monitoring and SLA management
- **Dependabot Integration**: Enhanced dependabot validation and automation
- **Template Synchronization**: Sophisticated template update propagation system
- **Issue State Tracking**: Advanced issue lifecycle management and tracking
- **GITHUB_TOKEN Standardization**: Improved token handling across all workflows

### ♻️ Code Refactoring
- **Removed AI_EVOLUTION.md**: Migrated to structured ADR approach for better maintainability
- **Simplified README Structure**: Eliminated redundancy between README and documentation site
- **Enhanced Initialization Cleanup**: Improved fork repository cleanup and setup process
- **Standardized Error Handling**: Consistent error handling patterns across all workflows

### 🐛 Bug Fixes
- **YAML Syntax Issues**: Resolved multiline string handling in workflow configurations
- **Release Workflow Compatibility**: Updated to googleapis/release-please-action@v4
- **MCP Server Configuration**: Fixed Maven MCP server connection and configuration issues
- **Cascade Trigger Reliability**: Implemented pull_request_target pattern for better triggering
- **Git Diff Syntax**: Corrected git command syntax in sync-template workflow
- **Label Management**: Standardized label usage across all workflows and templates

## [1.0.0] - Initial Release

### ✨ Features
- Initial release of OSDU Fork Management Template
- Automated fork initialization workflow
- Daily upstream synchronization with AI-enhanced PR descriptions
- Three-branch management strategy (main, fork_upstream, fork_integration)
- Automated conflict detection and resolution guidance
- Semantic versioning and release management
- Template development workflows separation

### 📚 Documentation
- Complete architectural decision records (ADRs)
- Product requirements documentation
- Development and usage guides
- GitHub Actions workflow documentation
