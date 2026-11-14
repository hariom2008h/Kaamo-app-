package com.kaamo.app

/** The one place the product name lives in code. Legal copy strings are plain text and are renamed by scripts/rename_app.sh. */
object AppConfig {
    const val NAME = "Kaamo"

    /** The hosted web app. The service-provider form is served from here and shown inside the app. */
    const val WEB_URL = "https://kaamo-web.vercel.app"
}
