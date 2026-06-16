plugins {
    // 自动下载项目所需的JDK版本
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

include(":app", ":automator", ":common", ":autojs", ":inrt", ":apkbuilder")
include(":paddleocr")
include(":codeeditor")
include(":enhancedfloaty")
project(":enhancedfloaty").projectDir = file("third_party/enhancedfloaty")
include(":mutabletheme")
project(":mutabletheme").projectDir = file("third_party/mutabletheme")
include(":colorpicker")
project(":colorpicker").projectDir = file("third_party/colorpicker")
include(":multi-level-listview")
project(":multi-level-listview").projectDir = file("third_party/multi-level-listview")
include(":apksigner-lib")
project(":apksigner-lib").projectDir = file("third_party/apksigner")
