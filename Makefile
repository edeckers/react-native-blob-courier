SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

ifeq ($(origin .RECIPEPREFIX), undefined)
  $(error This Make does not support .RECIPEPREFIX. Please use GNU Make 4.0 or later)
endif
.RECIPEPREFIX = >

SHELL := bash

all: help

build: clean
> bin/run-build

clean: require-target
> bin/run-clean

help:
> bin/print-help

run: build
> cd example && yarn && yarn start

require-target:
ifndef TARGET
> @echo "expect TARGET parameter, see 'make help'"
> exit 1
endif

test: build
> bin/run-clean
> bin/run-build
> bin/run-tests

.PHONY: build clean help run-example test