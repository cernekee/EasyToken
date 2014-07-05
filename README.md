Easy Token
==========

Easy Token is an RSA SecurID-compatible software authenticator with
advanced usability features:

* Convenient lock screen and home screen widgets provide instant tokencodes
without navigating to an app.
* Optionally save your PIN.
* Supports SDTID files, importing http://127.0.0.1/... tokens from email,
and QR tokens.
* 100% open source: https://github.com/cernekee/EasyToken

## Support

To report issues, please email the author at [cernekee@gmail.com](mailto:cernekee+et@gmail.com).

## Building from source

On the host side you'll need to install:

* NDK r9d, nominally under /opt/android-ndk-r9d
* Host-side gcc, make, etc. (Red Hat "Development Tools" group or Debian build-essential)
* git, autoconf, automake, and libtool
* Android SDK in your $PATH (both platform-tools/ and tools/ directories)
* javac 1.6 and a recent version of Apache ant
* Use the Android SDK Manager to install API 19

First, clone the source trees:

    git clone git://github.com/cernekee/EasyToken
    cd EasyToken
    git submodule update --init

Then build the binary components (libs/ directory):

    make -C external NDK=/opt/android-ndk-r9d

Then build the Java components:

    android update project -p .
    ant debug

Build logs can be found on this project's [Travis CI page](https://travis-ci.org/cernekee/EasyToken).
