package co.kica.fileutils

import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import java.util.regex.Pattern

object Storage {
    val storageSet: HashSet<String?>
        get() {
            var storageSet = getStorageSet(File("/proc/mounts"), false)
            if (storageSet.isNullOrEmpty()) {
                storageSet = HashSet()
                storageSet.add(Environment.getExternalStorageDirectory().absolutePath)
            }
            return storageSet
        }

    fun getStorageSet(file: File?, is_fstab_file: Boolean): HashSet<String?> {
        val storageSet = HashSet<String?>()
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(InputStreamReader(FileInputStream(file)))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                var _storage: HashSet<String?>? = null
                _storage = if (is_fstab_file) {
                    parseVoldFile(line)
                } else {
                    parseMountsFile(line)
                }
                if (_storage == null) continue
                storageSet.addAll(_storage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                reader!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            reader = null
        }
        /*
         * set default external storage
         */storageSet.add(Environment.getExternalStorageDirectory().absolutePath)
        return storageSet
    }

    private fun parseMountsFile(str: String?): HashSet<String?>? {
        if (str.isNullOrEmpty()) return null
        if (str.startsWith("#")) return null
        val storageSet = HashSet<String?>()
        /*
         * /dev/block/vold/179:19 /mnt/sdcard2 vfat rw,dirsync,nosuid,nodev,noexec,relatime,uid=1000,gid=1015,fmask=0002,dmask=0002,allow_utime=0020,codepage=cp437,iocharset=iso8859-1,shortname=mixed,utf8,errors=remount-ro 0 0
         * /dev/block/vold/179:33 /mnt/sdcard vfat rw,dirsync,nosuid,nodev,noexec,relatime,uid=1000,gid=1015,fmask=0002,dmask=0002,allow_utime=0020,codepage=cp437,iocharset=iso8859-1,shortname=mixed,utf8,errors=remount-ro 0 0
         */
        val patter = Pattern.compile("/dev/block/vold.*?(/mnt/.+?|/storage.+?) vfat .*")
        val matcher = patter.matcher(str)
        val b = matcher.find()
        if (b) {
            val _group = matcher.group(1)
            if (!_group.contains("asec") && !_group.contains(":")) storageSet.add(_group)
        }
        return storageSet
    }

    private fun parseVoldFile(str: String?): HashSet<String?>? {
        if (str.isNullOrEmpty()) return null
        if (str.startsWith("#")) return null
        val storageSet = HashSet<String?>()
        /*
         * dev_mount sdcard /mnt/sdcard auto /devices/platform/msm_sdcc.1/mmc_host
         * dev_mount SdCard /mnt/sdcard/extStorages /mnt/sdcard/extStorages/SdCard auto sd /devices/platform/s3c-sdhci.2/mmc_host/mmc1
         */
        val patter1 = Pattern.compile("(/mnt/[^ ]+?)((?=[ ]+auto[ ]+)|(?=[ ]+(\\d*[ ]+)))")
        /*
         * dev_mount ins /mnt/emmc emmc /devices/platform/msm_sdcc.3/mmc_host
         */
        val patter2 = Pattern.compile("(/mnt/.+?)[ ]+")
        val matcher1 = patter1.matcher(str)
        val b1 = matcher1.find()
        if (b1) {
            val _group = matcher1.group(1)
            storageSet.add(_group)
        }
        val matcher2 = patter2.matcher(str)
        val b2 = matcher2.find()
        if (!b1 && b2) {
            val _group = matcher2.group(1)
            storageSet.add(_group)
        }
        return storageSet
    }
}