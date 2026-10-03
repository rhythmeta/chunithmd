package org.rhythmeta.chunithmd.localization

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import org.rhythmeta.chunithmd.shared.localization.AppLanguage
import org.rhythmeta.chunithmd.shared.localization.AppStrings

object AndroidAppLanguage {
    fun localizedContext(context: Context): Context {
        val systemTags = context.resources.configuration.locales.toLanguageTags().split(',')
        val language = AppLanguage.resolve(systemTags)
        AppStrings.language = language
        val configuration = Configuration(context.resources.configuration)
        val locales = LocaleList.forLanguageTags(language.tag)
        configuration.setLocales(locales)
        LocaleList.setDefault(locales)
        return context.createConfigurationContext(configuration)
    }
}
