package io.mailguru.whois.parser

import io.mailguru.whois.model.WhoisResult
import org.parboiled.Action
import org.parboiled.BaseParser
import org.parboiled.Context
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree

/**
 * Abstract base class for whois result parsers.
 * @param whoisServer The FQDN of the whois server this parser should be responsible for.
 */
@BuildParseTree
abstract class Parser(val whoisServer: String) : BaseParser<Any>() {

    companion object {
        const val PARSE_ERROR_MSG = "The input could not be parsed properly."
    }

    /**
     * Method to be called with the response from a whois request.
     * @param input The response from a whois request.
     * @return The parsed result, if the given [input] could be parsed successfully.
     * @throws IllegalStateException if the given input could not be parsed correctly.
     */
    @Throws(IllegalStateException::class)
    abstract fun parse(input: String): WhoisResult

    abstract fun start(): Rule

    open fun singleRowWithAction(key: String, action: Action<String>): Rule = Sequence(
        ZeroOrMore(' '),
        IgnoreCase(key),
        ZeroOrMore(' '),
        ZeroOrMore(NoneOf("\n")),
        object : Action<String> {
            override fun run(context: Context<String>): Boolean = push(context.match.trim())
        },
        OneOrMore('\n'),
        action,
        ZeroOrMore('\n'),
    )
}
