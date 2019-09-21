SHELL := /bin/bash
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

.PHONY: setup setup-joker

.DEFAULT_GOAL := setup

setup-deps:
	@yarn

setup-joker:
	@printf "\n ${YELLOW} Download joker ${NC}\n"
	@wget -c https://github.com/candid82/joker/releases/download/v0.12.7/joker-0.12.7-linux-amd64.zip -O joker.zip
	@unzip joker.zip
	@printf "\n ${YELLOW} Put joker in bin ${NC}\n"
	@sudo chmod +x joker
	sudo mv -f ./joker /usr/local/bin/
	@printf "${YELLOW} Successfully installed ${NC}\n"
	@printf "\n ${YELLOW} Cleanup ${NC}\n"
	@rm -Rf joker joker.zip

setup: setup-joker setup-deps
