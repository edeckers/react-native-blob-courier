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
> @echo "TARGET=[android android:instrumented android:unit ios typescript] make build - build the library"
> @echo "TARGET=[android android:instrumented android:unit ios typescript] make clean - clean build"
> @echo ""
> @echo "TARGET=[android ios] make run - run the example app"
> @echo ""
> @echo "TARGET=[android android:instrumented android:unit ios typescript] make test - run tests"

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