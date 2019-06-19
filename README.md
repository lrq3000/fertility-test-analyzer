
# Fertility Test Analyzer

Fertility Test Analyzer (FTA) is an Android app developed by [Colnix Technology](https://www.colnix.com) that helps women interpret ovulation and pregnancy test strips.

## Building

The minimal requirements to build FTA are Java 8 and [Inkscape](https://inkscape.org/) installed and in the path. Simply run:
```
./gradlew build
```
from the terminal. The resulting APK installer will be produced as app/build/outputs/apk/app-release-unsigned.apk.

FTA is constructed with the building tool Gradle and can be imported and built within Android Studio, but still requires Inkscape.

The reason it needs Inkscape is because it generates the PNGs for all the screen resolutions from a list of SVGs. The Gradle plugin that actually does so can be easily ported to other projects copying the /app/svg.gradle file and including the line:
```
apply from: 'svg.gradle'
```
in the destination /app/build.gradle file.

## License Agreement

The source code has been released to the public under the [Apache 2.0 license](LICENSE.txt). If you use this software or any part of it, don't forget to display the [notice text attached](NOTICE.txt).

## Terms of Service

Any information given by FTA does not constitute medical practice or advice, nor is it intended to replace the necessity of consultation with a practitioner. Always consult your personal practitioner for definitive answers to your medical questions. You agree that Colnix will incur no legal or moral liabilities to you or anyone else for your use of FTA.

Colnix explicitly warns against and prohibits the use of FTA as a contraception tool or for pregnancy avoidance. FTA has been designed only to provide tools for people interested in getting pregnant. FTA cannot be used for scheduling or planning medical procedures, medication, or supplements usage. Always consult your health care provider before taking such decisions.

## Privacy Policy

FTA does not remotely collects, stores and uses your personal information at all. All the data and images you input are stored in your own device.

