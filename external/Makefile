#
# external/Makefile: Cross-compile libstoken.so for {arm,x86,mips}
#
# This file is part of Easy Token
# Copyright (c) 2014, Kevin Cernekee <cernekee@gmail.com>
#
# Portions based on OpenConnect's android/Makefile.
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#

NDK := /opt/android-ndk-r10d

TOPDIR := $(shell pwd)
LIBS := $(TOPDIR)/../libs
DISTDIR := $(TOPDIR)/dist

ARCH_LIST := arm x86 mips

NDK_ARCH_arm := armeabi
TRIPLET_arm := arm-linux-androideabi
CFLAGS_arm := -mthumb -Os

NDK_ARCH_x86 := x86
TRIPLET_x86 := i686-linux-android
CFLAGS_x86 := -Os

NDK_ARCH_mips := mips
TRIPLET_mips := mipsel-linux-android
CFLAGS_mips := -Os

SYSROOT := toolchain/sysroot/usr

set_vars = PATH=$(TOPDIR)/$1/toolchain/bin:$$PATH \
	PKG_CONFIG_LIBDIR=$(TOPDIR)/$1/$(SYSROOT)/lib/pkgconfig \
	CFLAGS="$(CFLAGS_$1)"
do_make = $(call set_vars,$1) $(MAKE)

FETCH := $(TOPDIR)/fetch.sh

ARCH_TOOLCHAIN_TARGETS := $(addsuffix /.toolchain-built,$(ARCH_LIST))
ARCH_LIBXML2_TARGETS := $(addsuffix /$(SYSROOT)/lib/libxml2.la,$(ARCH_LIST))
ARCH_GMP_TARGETS := $(addsuffix /$(SYSROOT)/lib/libgmp.la,$(ARCH_LIST))
ARCH_NETTLE_TARGETS := $(addsuffix /$(SYSROOT)/lib/libnettle.a,$(ARCH_LIST))
ARCH_STOKEN_TARGETS := $(addsuffix /libstoken.so,$(ARCH_LIST))

.PHONY: all clean distclean

all: $(ARCH_STOKEN_TARGETS) stoken-wrapper.jar

clean:
	rm -rf $(ARCH_LIST) stoken-wrapper.jar
	cd stoken/java && ant clean

distclean: clean
	rm -rf dist nettle gmp libxml2

#####################################################################
# Toolchain
#
# (The fallback logic is because NDK versions <= r8e can fail after
# trying to use 32-bit binaries on a 64-bit NDK installation.)
#
#####################################################################

$(ARCH_TOOLCHAIN_TARGETS): %/.toolchain-built:
	mkdir -p $*/toolchain
	bash $(NDK)/build/tools/make-standalone-toolchain.sh \
		--platform=android-14 \
		--arch=$* --install-dir=$*/toolchain || \
	bash $(NDK)/build/tools/make-standalone-toolchain.sh \
		--platform=android-14 --system=linux-x86_64 \
		--arch=$* --install-dir=$*/toolchain
	touch $@

#####################################################################
# libxml2 (out of tree build)
#####################################################################

LIBXML2_TAR := $(DISTDIR)/libxml2-2.9.2.tar.gz
LIBXML2_SHA1 := f46a37ea6d869f702e03f393c376760f3cbee673

$(LIBXML2_TAR):
	mkdir -p $(DISTDIR)
	$(FETCH) $(LIBXML2_TAR) $(LIBXML2_SHA1)

libxml2/configure: $(LIBXML2_TAR)
	mkdir -p $(DISTDIR)
	mkdir libxml2
	tar -C libxml2 --strip 1 -zxf $(LIBXML2_TAR)
	touch $@

$(ARCH_LIBXML2_TARGETS): %/$(SYSROOT)/lib/libxml2.la: \
		%/.toolchain-built libxml2/configure
	rm -rf $*/libxml2
	mkdir $*/libxml2
	cd $*/libxml2 && $(call set_vars,$*) $(TOPDIR)/libxml2/configure \
		--prefix=$(TOPDIR)/$*/$(SYSROOT) --host=$(TRIPLET_$*) \
		--disable-shared --enable-static \
		--without-c14n -without-catalog --without-debug --without-docbook \
		--without-fexceptions --without-ftp --without-history \
		--without-html --without-http --without-iconv --without-iconv \
		--without-iso8859x --without-legacy --without-pattern \
		--without-push --without-regexps --without-run-debug \
		--without-sax1 --without-schemas --without-schematron \
		--without-threads --without-valid --without-xinclude \
		--without-xpath --without-xptr --without-zlib --without-lzma \
		--without-coverage --without-python
	$(call do_make,$*) -C $*/libxml2 libxml2.la
	$(call do_make,$*) -C $*/libxml2 libxml-2.0.pc
	$(call do_make,$*) -C $*/libxml2 install-libLTLIBRARIES
	$(call do_make,$*) -C $*/libxml2 install-data

#####################################################################
# libgmp (out of tree build)
#####################################################################

GMP_TAR := $(DISTDIR)/gmp-5.1.3.tar.bz2
GMP_SHA1 := b35928e2927b272711fdfbf71b7cfd5f86a6b165

$(GMP_TAR):
	mkdir -p $(DISTDIR)
	$(FETCH) $(GMP_TAR) $(GMP_SHA1)

gmp/configure: $(GMP_TAR)
	mkdir -p $(DISTDIR)
	mkdir gmp
	tar -C gmp --strip 1 -jxf $(GMP_TAR)
	touch $@

$(ARCH_GMP_TARGETS): %/$(SYSROOT)/lib/libgmp.la: \
		%/.toolchain-built gmp/configure
	rm -rf $*/gmp
	mkdir $*/gmp
	cd $*/gmp && $(call set_vars,$*) $(TOPDIR)/gmp/configure \
		--prefix=$(TOPDIR)/$*/$(SYSROOT) --host=$(TRIPLET_$*) \
		--disable-assembly \
		--disable-shared --enable-static
	$(call do_make,$*) -C $*/gmp
	$(call do_make,$*) -C $*/gmp install

#####################################################################
# nettle (out of tree build)
#####################################################################

NETTLE_TAR := $(DISTDIR)/nettle-2.7.1.tar.gz
NETTLE_SHA1 := e7477df5f66e650c4c4738ec8e01c2efdb5d1211

$(NETTLE_TAR):
	mkdir -p $(DISTDIR)
	$(FETCH) $(NETTLE_TAR) $(NETTLE_SHA1)

nettle/configure: $(NETTLE_TAR)
	mkdir -p $(DISTDIR)
	mkdir nettle
	tar -C nettle --strip 1 -zxf $(NETTLE_TAR)
	touch $@

$(ARCH_NETTLE_TARGETS): %/$(SYSROOT)/lib/libnettle.a: \
		%/.toolchain-built nettle/configure \
		%/$(SYSROOT)/lib/libgmp.la
	rm -rf $*/nettle
	mkdir $*/nettle
	cd $*/nettle && $(call set_vars,$*) $(TOPDIR)/nettle/configure \
		--prefix=$(TOPDIR)/$*/$(SYSROOT) --host=$(TRIPLET_$*) \
		--disable-assembler \
		--disable-shared --enable-static
	$(call do_make,$*) -C $*/nettle SUBDIRS=
	$(call do_make,$*) -C $*/nettle SUBDIRS= install

#####################################################################
# stoken (out of tree build)
#####################################################################

stoken/configure:
	cd stoken && bash autogen.sh

$(ARCH_STOKEN_TARGETS): %/libstoken.so: \
		%/.toolchain-built stoken/configure \
		%/$(SYSROOT)/lib/libnettle.a %/$(SYSROOT)/lib/libxml2.la
	rm -rf $*/stoken
	mkdir $*/stoken
	cd $*/stoken && $(call set_vars,$*) $(TOPDIR)/stoken/configure \
		--prefix=/usr --host=$(TRIPLET_$*) \
		--without-gtk --with-java=$(TOPDIR)/$*/$(SYSROOT)/include \
		--enable-jni-standalone
	$(call do_make,$*) -C $*/stoken
	$(call do_make,$*) -C $*/stoken install DESTDIR=$(TOPDIR)/$*
	cp $*/usr/lib/libstoken.so $@.tmp
	mkdir -p $(LIBS)/$(NDK_ARCH_$*)
	cp $@.tmp $(LIBS)/$(NDK_ARCH_$*)/libstoken.so
	$(TOPDIR)/$*/toolchain/bin/$(TRIPLET_$*)-strip \
		$(LIBS)/$(NDK_ARCH_$*)/libstoken.so
	mv $@.tmp $@

stoken-wrapper.jar:
	cd stoken/java && ant
	mkdir -p $(LIBS)
	cp stoken/java/dist/stoken-wrapper.jar $(LIBS)/
	cp $(LIBS)/stoken-wrapper.jar .

#####################################################################
# Misc.
#####################################################################

sources: $(LIBXML2_TAR) $(GMP_TAR) $(NETTLE_TAR)

