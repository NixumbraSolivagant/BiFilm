package com.bifilm.app.navigation

object Routes {
    const val Home = "home"
    const val Settings = "settings"
    const val Capture = "capture/{projectId}"
    const val Compose = "compose/{projectId}"
    const val Export = "export/{projectId}"

    fun capture(projectId: String) = "capture/$projectId"
    fun compose(projectId: String) = "compose/$projectId"
    fun export(projectId: String) = "export/$projectId"
}

object NavArgs {
    const val ProjectId = "projectId"
}
