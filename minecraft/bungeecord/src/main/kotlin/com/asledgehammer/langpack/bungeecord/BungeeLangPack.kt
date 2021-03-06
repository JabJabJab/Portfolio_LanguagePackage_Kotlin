@file:Suppress("unused")

package com.asledgehammer.langpack.bungeecord

import com.asledgehammer.langpack.bungeecord.objects.complex.BungeeActionText
import com.asledgehammer.langpack.bungeecord.objects.complex.BungeeStringPool
import com.asledgehammer.langpack.core.LangPack
import com.asledgehammer.langpack.core.Language
import com.asledgehammer.langpack.core.Languages
import com.asledgehammer.langpack.core.objects.LangArg
import com.asledgehammer.langpack.core.objects.complex.Complex
import com.asledgehammer.langpack.textcomponent.TextComponentLangPack
import com.asledgehammer.langpack.textcomponent.processor.TextComponentProcessor
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.io.File

/**
 * **BungeeLangPack** wraps the [LangPack] class to provide additional support for the BungeeCord API.
 *
 * @author Jab
 *
 * @param classLoader (Optional) (Recommended) Pass the plugin classloader instance to use the save features for the
 * library.
 * @param dir (Optional) The File Object for the directory where the LangFiles are stored. DEFAULT: 'lang/'
 * @throws IllegalArgumentException Thrown if the directory doesn't exist or isn't a valid directory.
 */
class BungeeLangPack(classLoader: ClassLoader = this::class.java.classLoader, dir: File = File("lang")) :
    TextComponentLangPack(classLoader, dir) {

    /**
     * Basic constructor. Uses the 'lang' directory in the server folder.
     *
     * @param classLoader The classloader to load resources.
     */
    constructor(classLoader: ClassLoader) : this(classLoader, File("lang"))

    /**
     * Broadcasts a message to all online players, checking their locales and sending the corresponding dialog.
     *
     * @param query The ID of the dialog to send.
     * @param args The variables to apply to the dialog sent.
     */
    fun broadcast(query: String, vararg args: LangArg) {

        val cache = HashMap<Language, TextComponent>()
        val server = ProxyServer.getInstance()

        for (player in server.players) {

            // Grab the players language, else fallback to default.
            val langPlayer = getLanguage(player)
            var lang = langPlayer

            if (cache[lang] != null) {
                player.sendMessage(cache[lang])
                continue
            }

            var resolved = resolve(query, lang)
            if (resolved == null) {
                lang = defaultLang
                resolved = resolve(query, lang)
            }

            val component: TextComponent

            if (resolved != null) {
                val value = resolved.value
                component = when (value) {
                    is Complex<*> -> {
                        val result = value.get()
                        val processedComponent: TextComponent = if (result is TextComponent) {
                            result
                        } else {
                            TextComponent(result.toString())
                        }
                        processedComponent
                    }
                    else -> {
                        TextComponent(value.toString())
                    }
                }
            } else {
                component = TextComponent(query)
            }

            val result = if (resolved != null) {
                (processor as TextComponentProcessor).process(component, this, langPlayer, resolved.parent, *args)
            } else {
                (processor as TextComponentProcessor).process(component, this, langPlayer, null, *args)
            }
            cache[lang] = result
            cache[langPlayer] = result

            player.sendMessage(result)
        }
    }

    /**
     * Messages a player with a given field and arguments. The language will be based on [ProxiedPlayer.getLocale].
     *   If the language is not supported, [LangPack.defaultLang] will be used.
     *
     * @param player The player to send the message.
     * @param query The field to send.
     * @param args Additional arguments to apply.
     */
    fun message(player: ProxiedPlayer, query: String, vararg args: LangArg) {

        val langPlayer = getLanguage(player)
        var lang = langPlayer

        var resolved = resolve(query, lang)
        if (resolved == null) {
            lang = defaultLang
            resolved = resolve(query, lang)
        }

        val component: TextComponent
        if (resolved != null) {
            val value = resolved.value
            component = when (value) {
                is Complex<*> -> {
                    val result = value.get()
                    val processedComponent: TextComponent = if (result is TextComponent) {
                        result
                    } else {
                        TextComponent(result.toString())
                    }
                    processedComponent
                }
                else -> {
                    TextComponent(value.toString())
                }
            }
        } else {
            component = TextComponent(query)
        }

        val result = if (resolved != null) {
            (processor as TextComponentProcessor).process(component, this, langPlayer, resolved.parent, *args)
        } else {
            (processor as TextComponentProcessor).process(component, this, langPlayer, null, *args)
        }
        player.sendMessage(result)
    }

    /**
     * @param player The player to read.
     *
     * @return Returns the language of the player's [ProxiedPlayer.getLocale]. If the locale set is invalid, the fallBack
     *   is returned.
     */
    fun getLanguage(player: ProxiedPlayer): Language {
        // When a player is kicked on login, the locale is null. Use default lang if so. -Jab
        return try {
            Languages.getClosest(player.locale, defaultLang)
        } catch (e: Exception) {
            defaultLang
        }
    }

    init {
        setBungeeLoaders(loaders)
    }

    companion object {

        private val actionTextLoader = BungeeActionText.Loader()
        private val stringPoolLoader = BungeeStringPool.Loader()

        /**
         * Adds the default loaders for the bungeecord module.
         *
         * @param map The map to store the loaders.
         */
        fun setBungeeLoaders(map: HashMap<String, Complex.Loader<*>>) {
            map["action"] = actionTextLoader
            map["pool"] = stringPoolLoader
        }
    }
}
