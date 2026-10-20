package org.rhythmeta.chunithmd.core.update

enum class AppUpdateFailure {
    Network, InvalidManifest, UnsupportedDevice, Storage, Download, Integrity, Package, Signature, Install,
}
