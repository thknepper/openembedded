DESCRIPTION = "udev is a daemon which dynamically creates and removes device nodes from \
/dev/, handles hotplug events and loads drivers at boot time. It replaces \
the hotplug package and requires a kernel not older than 2.6.12."
LICENSE = "GPLv2+"

PR = "r7"

# Untested
DEFAULT_PREFERENCE = "-1"

# Needed for udev-extras
DEPENDS = "gperf-native usbutils acl glib-2.0 mtd-utils"
RDEPENDS_${PN} += "module-init-tools-depmod udev-utils"

SRC_URI = "http://kernel.org/pub/linux/utils/kernel/hotplug/udev-${PV}.tar.gz \
	   file://mount.blacklist \
	   file://run.rules \
	   "

SRC_URI[md5sum] = "b8cf77be693fd09ccfcf26d3e4e23e97"
SRC_URI[sha256sum] = "5c271f43d8a28176050abbc6b4e4354203b6a7d810c72c18afefd1ee100485c3"

SRC_URI += " \
       file://udev.rules \
       file://devfs-udev.rules \
       file://links.conf \
       file://permissions.rules \
       file://mount.sh \
       file://network.sh \
       file://local.rules \
       file://default \
       file://init \
       file://cache \
       file://udev-compat-wrapper-patch \
"

SRC_URI_append_h2200 = " file://50-hostap_cs.rules "
PACKAGE_ARCH_h2200 = "h2200"

#buglabs's bug device
SRC_URI_append_bug = " \
       file://30-BUG.rules \
       file://10-mx31.rules \
       file://bmi_eventpipe.sh "
PACKAGE_ARCH_bug = "bug"

SRC_URI_append_nokia900 = " \
       file://10-cmt_speech.rules \
       file://70-persistent-net.rules \
       file://udev-rules-nokia-n900-hacks.rules \
       file://udev-rules-nokia-n900-snd.rules \
       file://nokia-n900-mac-hack.sh \
"
PACKAGE_ARCH_nokia900 = "nokia900"


inherit update-rc.d autotools

EXTRA_OECONF += " --with-udev-prefix= \
                  --with-libdir-name=${base_libdir} \
                  --with-pci-ids-path=/usr/share/misc \
                  --disable-introspection \
                  ac_cv_file__usr_share_pci_ids=no \
                  ac_cv_file__usr_share_hwdata_pci_ids=no \
                  ac_cv_file__usr_share_misc_pci_ids=yes \
                  --sbindir=${base_sbindir} \
                  --libexecdir=${base_libdir}/udev \
                  --with-rootlibdir=${base_libdir} \
"

INITSCRIPT_NAME = "udev"
INITSCRIPT_PARAMS = "start 04 S ."

PACKAGES =+ "libudev libgudev udev-utils"

FILES_libudev = "${base_libdir}/libudev.so.*"
FILES_libgudev = "${base_libdir}/libgudev*.so.*"

FILES_udev-utils = "${bindir}/udevinfo ${bindir}/udevtest ${base_sbindir}/udevadm"

RPROVIDES_${PN} = "hotplug"
FILES_${PN} += "${usrbindir}/* ${usrsbindir}/udevd"
FILES_${PN}-dbg += "${usrbindir}/.debug ${usrsbindir}/.debug"

# udev installs binaries under $(udev_prefix)/lib/udev, even if ${libdir}
# is ${prefix}/lib64
FILES_${PN} += "/lib/udev* ${libdir}/ConsoleKit"
FILES_${PN}-dbg += "/lib/udev/.debug"

# Package up systemd files
FILES_${PN} += "${base_libdir}/systemd"

RPROVIDES_udev_append = " udev-compat-wrapper"

# Modify init script on platforms that need to boot old kernels:
do_apply_compat_wrapper() {
	cd ${WORKDIR}
	sed -i "s:/sbin/udevd:\$UDEVD:g;s:/sbin/udevadm:\$UDEVADM:g" init
	patch <udev-compat-wrapper-patch
	cd -
}

do_install () {
	install -d ${D}${usrsbindir} \
		   ${D}${sbindir}
	oe_runmake 'DESTDIR=${D}' INSTALL=install install
	install -d ${D}${sysconfdir}/init.d
	install -m 0755 ${WORKDIR}/init ${D}${sysconfdir}/init.d/udev
	install -m 0755 ${WORKDIR}/cache ${D}${sysconfdir}/init.d/udev-cache

	install -d ${D}${sysconfdir}/default
	install -m 0755 ${WORKDIR}/default ${D}${sysconfdir}/default/udev

 	cp ${S}/rules/rules.d/* ${D}${sysconfdir}/udev/rules.d/

	install -m 0644 ${WORKDIR}/mount.blacklist     ${D}${sysconfdir}/udev/
	install -m 0644 ${WORKDIR}/local.rules         ${D}${sysconfdir}/udev/rules.d/local.rules
	install -m 0644 ${WORKDIR}/permissions.rules   ${D}${sysconfdir}/udev/rules.d/permissions.rules
	install -m 0644 ${WORKDIR}/run.rules          ${D}${sysconfdir}/udev/rules.d/run.rules
	install -m 0644 ${WORKDIR}/udev.rules          ${D}${sysconfdir}/udev/rules.d/udev.rules
	install -m 0644 ${WORKDIR}/links.conf          ${D}${sysconfdir}/udev/links.conf
	if [ "${UDEV_DEVFS_RULES}" = "1" ]; then
		install -m 0644 ${WORKDIR}/devfs-udev.rules ${D}${sysconfdir}/udev/rules.d/devfs-udev.rules
	fi

	touch ${D}${sysconfdir}/udev/saved.uname
	touch ${D}${sysconfdir}/udev/saved.cmdline
	touch ${D}${sysconfdir}/udev/saved.devices
	touch ${D}${sysconfdir}/udev/saved.atags

	install -d ${D}${sysconfdir}/udev/scripts/

	install -m 0755 ${WORKDIR}/mount.sh ${D}${sysconfdir}/udev/scripts/mount.sh
	install -m 0755 ${WORKDIR}/network.sh ${D}${sysconfdir}/udev/scripts
}

do_install_append_h2200() {
	install -m 0644 ${WORKDIR}/50-hostap_cs.rules         ${D}${sysconfdir}/udev/rules.d/50-hostap_cs.rules
}

do_install_append_bug() {
	install -m 0644 ${WORKDIR}/30-BUG.rules ${D}${sysconfdir}/udev/rules.d/30-BUG.rules
	install -m 0644 ${WORKDIR}/10-mx31.rules ${D}${sysconfdir}/udev/rules.d/10-mx31.rules
	install -m 0644 ${WORKDIR}/bmi_eventpipe.sh ${D}${sysconfdir}/udev/scripts/bmi_eventpipe.sh
}

do_install_append_nokia900() {
	install -m 0644 ${WORKDIR}/10-cmt_speech.rules ${D}${sysconfdir}/udev/rules.d/10-cmt_speech.rules
	install -m 0644 ${WORKDIR}/70-persistent-net.rules ${D}${sysconfdir}/udev/rules.d/70-persistent-net.rules
	install -m 0644 ${WORKDIR}/udev-rules-nokia-n900-hacks.rules ${D}${sysconfdir}/udev/rules.d/udev-rules-nokia-n900-hacks.rules
	install -m 0644 ${WORKDIR}/udev-rules-nokia-n900-snd.rules ${D}${sysconfdir}/udev/rules.d/udev-rules-nokia-n900-snd.rules
	install -m 0755 ${WORKDIR}/nokia-n900-mac-hack.sh ${D}${sysconfdir}/udev/scripts/nokia-n900-mac-hack.sh
}

# Create the cache after checkroot has run
pkg_postinst_udev_append() {
	if test "x$D" != "x"; then
		OPT="-r $D"
	else
		OPT="-s"
	fi
	update-rc.d $OPT udev-cache start 36 S .
}
