SHELL := /bin/bash
.DEFAULT_GOAL := signed

SDK_DIR ?= $(shell sed -n 's/^sdk.dir=//p' local.properties | tail -n 1)
BUILD_TOOLS_DIR ?= $(shell find "$(SDK_DIR)/build-tools" -maxdepth 1 -mindepth 1 -type d 2>/dev/null | sort -V | tail -n 1)
GRADLEW ?= bash gradlew
GRADLE_USER_HOME ?= .gradle-local

# Default to the signed v7 app package.
GRADLE_TASK ?= :app:assembleV7Release
APK_DIR ?= app/build/outputs/apk/v7/release
ABI ?= arm64-v8a

SIGNED_DIR ?= build/signed
KEYSTORE_DIR ?= signing
KEYSTORE_FILE ?= $(KEYSTORE_DIR)/autox-self.jks
KEYSTORE_PASS ?= 12345678
KEY_ALIAS ?= autox
KEY_PASS ?= $(KEYSTORE_PASS)
KEY_VALIDITY ?= 3650
KEY_DNAME ?= CN=AutoX, OU=Dev, O=AutoX, L=Shanghai, ST=Shanghai, C=CN

V1_SIGN ?= true
V2_SIGN ?= true
V3_SIGN ?= false
V4_SIGN ?= false

KEYTOOL ?= $(shell command -v keytool)
APKSIGNER ?= $(BUILD_TOOLS_DIR)/apksigner
ZIPALIGN ?= $(BUILD_TOOLS_DIR)/zipalign
GITVER ?= $(shell git rev-parse --short HEAD 2>/dev/null || echo unknown)
DOWNLOAD_DIR ?= $(HOME)/Downloads
COPY_TO_DOWNLOADS ?= true

.PHONY: help verify-tools keystore unsigned signed clean-signed \
	app-signed inrt-signed

help:
	@echo "Targets:"
	@echo "  make signed        Build v7 release APK, sign it, and copy it to Downloads"
	@echo "  make unsigned      Run Gradle task only"
	@echo "  make keystore      Generate self-signed JKS"
	@echo "  make clean-signed  Remove generated signed APKs and keystore"
	@echo "  make inrt-signed   Signed inrt commonRelease APK"
	@echo "  make app-signed    Signed app v7 release APK"
	@echo ""
	@echo "Useful overrides:"
	@echo "  GRADLE_TASK=:app:assembleV7Release"
	@echo "  APK_DIR=app/build/outputs/apk/v7/release"
	@echo "  KEYSTORE_FILE=signing/custom.jks"
	@echo "  KEYSTORE_PASS=... KEY_ALIAS=... KEY_PASS=..."
	@echo "  ABI=arm64-v8a"

verify-tools:
	@if [ -z "$(SDK_DIR)" ]; then echo "sdk.dir not found in local.properties"; exit 1; fi
	@if [ ! -x "$(APKSIGNER)" ]; then echo "apksigner not found: $(APKSIGNER)"; exit 1; fi
	@if [ ! -x "$(ZIPALIGN)" ]; then echo "zipalign not found: $(ZIPALIGN)"; exit 1; fi
	@if [ -z "$(KEYTOOL)" ]; then echo "keytool not found in PATH"; exit 1; fi

keystore: verify-tools
	@mkdir -p "$(KEYSTORE_DIR)"
	@if [ ! -f "$(KEYSTORE_FILE)" ]; then \
		echo "Generating self-signed keystore: $(KEYSTORE_FILE)"; \
		"$(KEYTOOL)" -genkeypair -v \
			-keystore "$(KEYSTORE_FILE)" \
			-storetype JKS \
			-storepass "$(KEYSTORE_PASS)" \
			-alias "$(KEY_ALIAS)" \
			-keypass "$(KEY_PASS)" \
			-keyalg RSA \
			-keysize 2048 \
			-validity "$(KEY_VALIDITY)" \
			-dname "$(KEY_DNAME)"; \
	else \
		echo "Using existing keystore: $(KEYSTORE_FILE)"; \
	fi

unsigned:
	GRADLE_USER_HOME="$(GRADLE_USER_HOME)" $(GRADLEW) --no-daemon $(GRADLE_TASK)

signed: unsigned keystore
	@mkdir -p "$(SIGNED_DIR)"
	@unsigned_apk=$$(find "$(APK_DIR)" -maxdepth 1 -type f -name "*$(ABI)*-unsigned.apk" | sort | head -n 1); \
	if [ -z "$$unsigned_apk" ]; then \
		unsigned_apk=$$(find "$(APK_DIR)" -maxdepth 1 -type f -name "*-unsigned.apk" | sort | head -n 1); \
	fi; \
	if [ -z "$$unsigned_apk" ]; then \
		echo "No unsigned APK found in $(APK_DIR)"; \
		exit 1; \
	fi; \
	base_name=$$(basename "$$unsigned_apk" .apk); \
	aligned_apk="$(SIGNED_DIR)/$${base_name%-unsigned}-aligned.apk"; \
	signed_apk="$(SIGNED_DIR)/$${base_name%-unsigned}-signed.apk"; \
	echo "Zipalign: $$unsigned_apk -> $$aligned_apk"; \
	"$(ZIPALIGN)" -f -p 4 "$$unsigned_apk" "$$aligned_apk"; \
	echo "Sign: $$aligned_apk -> $$signed_apk"; \
	"$(APKSIGNER)" sign \
		--ks "$(KEYSTORE_FILE)" \
		--ks-key-alias "$(KEY_ALIAS)" \
		--ks-pass pass:"$(KEYSTORE_PASS)" \
		--key-pass pass:"$(KEY_PASS)" \
		--v1-signing-enabled "$(V1_SIGN)" \
		--v2-signing-enabled "$(V2_SIGN)" \
		--v3-signing-enabled "$(V3_SIGN)" \
		--v4-signing-enabled "$(V4_SIGN)" \
		--out "$$signed_apk" \
		"$$aligned_apk"; \
	"$(APKSIGNER)" verify --verbose "$$signed_apk"; \
	if [ "$(COPY_TO_DOWNLOADS)" = "true" ]; then \
		mkdir -p "$(DOWNLOAD_DIR)"; \
		version_name=$$(sed -n 's/.*"versionName": "\(.*\)".*/\1/p' "$(APK_DIR)/output-metadata.json" | head -n 1); \
		if [ -z "$$version_name" ]; then \
			echo "Failed to read versionName from $(APK_DIR)/output-metadata.json"; \
			exit 1; \
		fi; \
		download_apk="$(DOWNLOAD_DIR)/Autox-v7s-$(ABI)-release-v$${version_name}-$(GITVER).apk"; \
		cp -f "$$signed_apk" "$$download_apk"; \
		echo "Copied APK: $$download_apk"; \
	fi; \
	echo "Signed APK: $$signed_apk"

inrt-signed:
	@$(MAKE) signed \
		GRADLE_TASK=:inrt:assembleCommonRelease \
		APK_DIR=inrt/build/outputs/apk/common/release \
		COPY_TO_DOWNLOADS=false

app-signed:
	@$(MAKE) signed \
		GRADLE_TASK=:app:assembleV7Release \
		APK_DIR=app/build/outputs/apk/v7/release

clean-signed:
	rm -rf "$(SIGNED_DIR)" "$(KEYSTORE_DIR)"
