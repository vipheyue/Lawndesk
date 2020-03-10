/*
 *     Copyright (C) 2020 Lawnchair Team.
 *
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.deletescape.lawnchair.globalsearch

import android.content.Context
import android.support.v7.view.ContextThemeWrapper
import ch.deletescape.lawnchair.LawnchairConfig
import ch.deletescape.lawnchair.colors.ColorEngine
import ch.deletescape.lawnchair.ensureOnMainThread
import ch.deletescape.lawnchair.globalsearch.providers.*
import ch.deletescape.lawnchair.theme.ThemeManager
import ch.deletescape.lawnchair.theme.ThemeOverride
import ch.deletescape.lawnchair.useApplicationContext
import ch.deletescape.lawnchair.util.SingletonHolder
import com.android.launcher3.Utilities

class ExternalSearchProviderController(private val context: Context) : ColorEngine.OnColorChangeListener {

    private val prefs by lazy { Utilities.getLawnchairPrefs(context) }
    private var cache: SearchProvider? = null
    private var cached: String = ""

    private val themeOverride = ThemeOverride(ThemeOverride.Launcher(), ThemeListener())
    private var themeRes: Int = 0

    private val listeners = HashSet<OnProviderChangeListener>()

    val isGoogle get() = searchProvider is GoogleSearchProvider

    init {
        ThemeManager.getInstance(context).addOverride(themeOverride)
        ColorEngine.getInstance(context).addColorChangeListeners(this, ColorEngine.Resolvers.ACCENT)
    }

    fun addOnProviderChangeListener(listener: OnProviderChangeListener) {
        listeners.add(listener)
    }

    fun removeOnProviderChangeListener(listener: OnProviderChangeListener) {
        listeners.remove(listener)
    }

    fun onExternalSearchProviderChanged() {
        cache = null
        notifyProviderChanged()
    }

    private fun notifyProviderChanged() {
        HashSet(listeners).forEach(OnProviderChangeListener::onExternalSearchProviderChanged)
    }

    val searchProvider: SearchProvider
        get() {
            val curr = prefs.externalSearchProvider
            if (cache == null || cached != curr) {
                cache = createProvider(prefs.externalSearchProvider) {
                    val lcConfig = LawnchairConfig.getInstance(context)
                    createProvider(lcConfig.defaultSearchProvider) { BaiduSearchProvider(context) }
                }
                cached = cache!!::class.java.name
                if (prefs.externalSearchProvider != cached) {
                    prefs.externalSearchProvider = cached
                }
                notifyProviderChanged()
            }
            return cache!!
        }

    private fun createProvider(providerName: String, fallback: () -> SearchProvider): SearchProvider {
        try {
            val constructor = Class.forName(providerName).getConstructor(Context::class.java)
            val themedContext = ContextThemeWrapper(context, themeRes)
            val prov = constructor.newInstance(themedContext) as SearchProvider
            if (prov.isAvailable) {
                return prov
            }
        } catch (ignored: Exception) { }
        return fallback()
    }

    override fun onColorChange(resolveInfo: ColorEngine.ResolveInfo) {
        if (resolveInfo.key == ColorEngine.Resolvers.ACCENT) {
            cache = null
            notifyProviderChanged()
        }
    }

    inner class ThemeListener : ThemeOverride.ThemeOverrideListener {

        override val isAlive = true

        override fun applyTheme(themeRes: Int) {
            this@ExternalSearchProviderController.themeRes = themeRes
        }

        override fun reloadTheme() {
            cache = null
            applyTheme(themeOverride.getTheme(context))
            notifyProviderChanged()
        }
    }

    interface OnProviderChangeListener {

        fun onExternalSearchProviderChanged()
    }

    companion object : SingletonHolder<ExternalSearchProviderController, Context>(ensureOnMainThread(useApplicationContext(::ExternalSearchProviderController))) {

        fun getExternalSearchProviders(context: Context) = listOf(
                GoogleSearchProvider(context),
                SFinderSearchProvider(context),
                GoogleGoSearchProvider(context),
                FirefoxSearchProvider(context),
                DuckDuckGoSearchProvider(context),
                BingSearchProvider(context),
                BaiduSearchProvider(context),
                YandexSearchProvider(context),
                QwantSearchProvider(context),
                SearchLiteSearchProvider(context),
                CoolSearchSearchProvider(context),
                EdgeSearchProvider(context)
        ).filter { it.isAvailable }
    }
}
