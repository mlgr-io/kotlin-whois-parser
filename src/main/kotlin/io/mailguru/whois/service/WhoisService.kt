package io.mailguru.whois.service

import io.mailguru.whois.model.WhoisResult
import io.mailguru.whois.model.exception.NotPermittedException
import io.mailguru.whois.parser.Parser
import org.apache.commons.net.whois.WhoisClient
import org.parboiled.Parboiled
import org.reflections.Reflections
import java.io.IOException
import java.lang.reflect.Modifier
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object WhoisService {

    private val whois by lazy {
        WhoisClient()
    }

    /**
     * Map of responsible whois servers, used as a cache in order to reduce the number of requests to whois.iana.org.
     */
    private val whoisServerList: ConcurrentMap<String, Optional<String>> = ConcurrentHashMap()

    /**
     * Reads all implementation of [Parser] from the "io.mailguru.whois.parser.impl" namespace and mounts them into a
     * [Map] with each of their [Parser.whoisServers] as the key and their instance as a value.
     * <p>
     * Since the singleton pattern is used (for [WhoisService]), every [Parser] implementation should be instantiated
     * only once, too.
     */
    private val availableParsers: Map<String, Parser> by lazy {
        Reflections("io.mailguru.whois.parser.impl").getSubTypesOf(Parser::class.java)
            .filterNot { Modifier.isAbstract(it.modifiers) }
            .flatMap { clazz ->
                clazz.getDeclaredConstructor().newInstance()!!.let { instance: Parser ->
                    instance.whoisServers.map {
                        Pair(it, Parboiled.createParser(clazz))
                    }
                }
            }
            .associate { it.first to it.second }
    }

    /**
     * Clears the (private) map of responsible whois servers. Being public for debugging and testing purposes mainly.
     */
    @Synchronized
    fun clearCache() {
        whoisServerList.clear()
    }

    /**
     * Looks up whois data for a given hostname.
     *
     * @param hostname The hostname to request whois data for.
     * @return The final result for the requested [hostname].
     * @return null if no responsible whois server could be found.
     * @throws IllegalArgumentException if the whois data could not be parsed correctly.
     * @throws NotPermittedException if the NIC doesn't provide any data via their whois server.
     * @throws IOException if the whois request itself failed.
     */
    @JvmStatic
    @Suppress("SwallowedException")
    @Throws(IllegalArgumentException::class, IOException::class, NotPermittedException::class)
    fun lookup(hostname: String): WhoisResult? = getWhoisServer(hostname)?.let { whoisServer ->
        availableParsers[whoisServer]?.parse(
            hostname,
            getResponseFromWhois(whoisServer, hostname)
        )
    }

    private fun getWhoisServer(hostname: String): String? = getCachedResponsibleWhoisServer(
        hostname.split(".").last()
    )?.orElseGet { null }

    /**
     * Retrieves the responsible whois server for a given TLD. Uses [whoisServerList] as a cache.
     * <p>
     * Calls to [WhoisClient] methods may throw either [IOException], [SocketException] or [UnknownHostException]; but
     * since [SocketException] and [UnknownHostException] are derived from [IOException], they're not caught explicitly.
     *
     * @param tld The TLD to retrieve the responsible whois server for.
     * @return A non-empty [Optional] containing the whois server responsible for the given TLD.
     * @return An empty [Optional] if the request to IANA didn't return a whois server in their result.
     * @return null if an error occurred.
     */
    @Suppress("SwallowedException")
    private fun getCachedResponsibleWhoisServer(tld: String): Optional<String>? = try {
        val rowPrefix = "whois:"
        whoisServerList.getOrPut(tld) {
            getResponseFromWhois("whois.iana.org", tld)
                .lines()
                .firstOrNull { it.lowercase().startsWith(rowPrefix) }
                ?.substring(rowPrefix.length)
                ?.trim()
                ?.let {
                    Optional.of(it)
                } ?: Optional.empty()
        }
    } catch (e: IOException) {
        null
    }

    private fun getResponseFromWhois(server: String, request: String): String = whois.connect(server).let { _ ->
        whois.query(request).also {
            whois.disconnect()
        }
    }
}
