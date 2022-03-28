SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

all: help

build:
	bin/run-build

ci:
	bin/run-ci

clean: require-target
	bin/run-clean

help:
	bin/print-help

lint: build
	bin/run-lint

release: build
	bin/run-release

run: build
	cd example && yarn && yarn start

require-target:
ifndef TARGET
	@echo "expect TARGET parameter, see 'make help'"
	exit 1
endif

test: build
	bin/run-tests

upgrade-dependencies:
	bin/upgrade-dependencies

.PHONY: build ci clean help lint run test upgrade-dependencies
