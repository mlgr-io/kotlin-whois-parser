package io.mailguru.whois.parser.impl

import io.mailguru.whois.model.WhoisResult
import io.mailguru.whois.model.exception.NotPermittedException
import io.mailguru.whois.parser.Parser
import org.parboiled.Rule

/**
 * Parser implementation for NICs that doesn't provide any data via their WHOIS servers at all.
 */
open class NoDataAvailableParser : Parser(
    setOf(
        "whois.nic.ch",
        "whois.nic.es",
        "whois.nic.li",
    )
) {

    /**
     * This is just a dummy rule.
     */
    override fun start(): Rule = ZeroOrMore(NoneOf("\n"))

    /**
     * Since we don't expect any useful input into this parser implementation, we just provide a dummy rule here.
     *
     * @throws NotPermittedException always.
     */
    @Throws(NotPermittedException::class)
    override fun parse(input: String): WhoisResult = throw NotPermittedException()
}
