#!/usr/bin/env bash

CWD="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ -z $1 ]]; then
	echo "Choose your icon theme:"
	echo "1. Legacy"
	echo "2. Colibre"
	echo "3. Colibre (SVG)"
	echo "4. Colibre Dark"
	echo "5. Colibre Dark (SVG)"
	read -n1 -p "Your choice: " choice
else
	choice=$1
fi

rm -rf $CWD/build/oxt/icons
mkdir $CWD/build/oxt/icons

case $choice in
1)
	echo -e "\nYou chose the legacy theme"
	cp -R $CWD/icon-themes/legacy/* $CWD/build/oxt/icons
	sed 's/_24/_26/g' $CWD/build/oxt/Addons.template.xcu > $CWD/build/oxt/Addons.xcu
	sed 's/_24/_26/g' $CWD/build/oxt/Addons_AOO4.template.xcu > $CWD/build/oxt/Addons_AOO4.xcu
	echo "legacy" > $CWD/build/oxt/icon-theme.txt
;;
2)
	echo -e "\nYou chose the Colibre theme"
	cp -R $CWD/icon-themes/colibre/* $CWD/build/oxt/icons
	sed 's/_26/_24/g' $CWD/build/oxt/Addons.template.xcu > $CWD/build/oxt/Addons.xcu
	sed 's/_26/_24/g' $CWD/build/oxt/Addons_AOO4.template.xcu > $CWD/build/oxt/Addons_AOO4.xcu
	echo "colibre" > $CWD/build/oxt/icon-theme.txt
;;
3)
	echo -e "\nYou chose the Colibre (SVG) theme"
	cp -R $CWD/icon-themes/colibre_svg/* $CWD/build/oxt/icons
	sed 's/_26/_24/g' $CWD/build/oxt/Addons.template.xcu > $CWD/build/oxt/Addons.xcu
	sed 's/_26/_24/g' $CWD/build/oxt/Addons_AOO4.template.xcu > $CWD/build/oxt/Addons_AOO4.xcu
	echo "colibre_svg" > $CWD/build/oxt/icon-theme.txt
;;
4)
	echo -e "\nYou chose the Colibre Dark theme"
	cp -R $CWD/icon-themes/colibre_dark/* $CWD/build/oxt/icons
	sed 's/_26/_24/g' $CWD/build/oxt/Addons.template.xcu > $CWD/build/oxt/Addons.xcu
	sed 's/_26/_24/g' $CWD/build/oxt/Addons_AOO4.template.xcu > $CWD/build/oxt/Addons_AOO4.xcu
	echo "colibre_dark" > $CWD/build/oxt/icon-theme.txt
;;
5)
	echo -e "\nYou chose the Colibre Dark (SVG) theme"
	cp -R $CWD/icon-themes/colibre_dark_svg/* $CWD/build/oxt/icons
	sed 's/_26/_24/g' $CWD/build/oxt/Addons.template.xcu > $CWD/build/oxt/Addons.xcu
	sed 's/_26/_24/g' $CWD/build/oxt/Addons_AOO4.template.xcu > $CWD/build/oxt/Addons_AOO4.xcu
	echo "colibre_dark_svg" > $CWD/build/oxt/icon-theme.txt
;;
*)
	echo -e "\nInvalid choice :("
	exit
;;
esac

