#!/usr/bin/env bash

set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

LOLZ_DIR="$(pwd)"

# Github info
git config --global user.name "Jprimero15"
git config --global user.email "jprimero15@aospa.co"

sudo pacman -Syu

sudo pacman -S --needed \
python python-pip git base-devel \
jdk17-openjdk unzip zip

sudo archlinux-java set java-17-openjdk

pip install --upgrade pip
pip install kivy buildozer cython

sudo pacman -S --needed \
libffi openssl sqlite zlib

buildozer init

cp -fr build.spec buildozer.spec

buildozer -v android debug

# End of Script
