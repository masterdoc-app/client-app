package pro.masterdoc.client.update

enum class AppUpdateFlow { Immediate, Silent }

fun versionCodeFromSemVer(major: Int, minor: Int, patch: Int): Int =
    major * 10_000 + minor * 100 + patch

fun majorFromVersionCode(versionCode: Int): Int = versionCode / 10_000

fun selectUpdateFlow(installedVersionCode: Int, availableVersionCode: Int): AppUpdateFlow? {
    if (availableVersionCode <= installedVersionCode) return null
    return if (majorFromVersionCode(availableVersionCode) > majorFromVersionCode(installedVersionCode)) {
        AppUpdateFlow.Immediate
    } else {
        AppUpdateFlow.Silent
    }
}
