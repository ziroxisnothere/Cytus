plugins {
    checkstyle
}

extensions.configure<CheckstyleExtension> {
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    configProperties["org.checkstyle.google.suppressionfilter.config"] = rootProject.file("checkstyle-suppressions.xml")
    maxErrors = 0
    maxWarnings = 0
    toolVersion = libs.checkstyle.get().version.toString()
}
