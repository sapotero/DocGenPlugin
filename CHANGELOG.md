<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [1.0.2] - 2025-02-16

### Added

- Experimental option for tree-shaking a function to gather useful information and generate different outputs (must be
  enabled in plugin settings):
  - Create a raw tree-shaken file.
  - Generate a Kotest file with empty test cases.
  - Generate a Kotest file with initial test implementations.

## [1.0.1] - 2025-02-12

### Changed

- Added Kotlin K2 support:
  - Detect whether K2 mode is enabled and switch to the appropriate analysis API.
  - Use `KtAnalysisSession` for PSI resolution when running in K2 mode.
  - Maintain compatibility with K1 mode for older IDE versions.

### Fixed

- Generated documentation and Kotest files only for top-level class declaration functions:
  - Prevented unnecessary test generation for nested or local functions.
  - Ensured accurate parsing of function signatures before generating test cases.