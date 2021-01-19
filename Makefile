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
> yarn

clean:
> rm -rf node_modules
> rm -rf ios/Pods
> rm -rf android/build
> rm -rf android/.gradle

help:
> @echo "make build target=[android, ios] - build the library"
> @echo "make clean - clean build"
> @echo ""
> @echo "make run target=[example:android example:ios example:server] - run the example app"
> @echo ""
> @echo "make test target=[android android:instrumented android:unit ios typescript] - run tests"

run-example: build
> cd example && yarn && yarn start

require-target:
ifndef target
> @echo "expect target parameter, see 'make help'"
> exit 1
endif

test: require-target build
> bin/run-tests $(target)

.PHONY: build clean help run-example test