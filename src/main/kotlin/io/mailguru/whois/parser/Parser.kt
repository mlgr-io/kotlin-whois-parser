package io.mailguru.whois.parser

import io.mailguru.whois.model.MutableWhoisResult
import io.mailguru.whois.model.WhoisResult
import org.parboiled.Action
import org.parboiled.BaseParser
import org.parboiled.Context
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.errors.ErrorUtils
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.ParseTreeUtils

/**
 * Abstract base class for whois result parsers.
 * @param whoisServers The FQDN of the whois servers this parser should be responsible for.
 */
@BuildParseTree
internal abstract class Parser(val whoisServers: Collection<String>) : BaseParser<Any>() {

    companion object {
        const val PARSE_ERROR_MSG = "The input could not be parsed properly."
    }

    /**
     * Method to be called with the response from a whois request.
     *
     * @param input The response from a whois request.
     * @return The parsed result, if the given [input] could be parsed successfully.
     * @throws IllegalStateException if the given input could not be parsed correctly.
     */
    @Throws(IllegalStateException::class)
    open fun parse(hostname: String, input: String): WhoisResult {
        return ReportingParseRunner<MutableWhoisResult>(start()).run(input).let { result ->

            check(result.parseErrors.isEmpty()) {
                ParseTreeUtils.printNodeTree(result)
                result.parseErrors.forEach {
                    ErrorUtils.printParseError(it)
                }

                PARSE_ERROR_MSG
            }

            result.resultValue.let { mutableResult ->
                WhoisResult(
                    domain = mutableResult.domain ?: hostname,
                    status = mutableResult.status ?: error(PARSE_ERROR_MSG),
                    headerComment = mutableResult.header.joinToString("\n") { it.trim() }.trim('\n'),
                    changedAt = mutableResult.changed
                )
            }
        }
    }

    abstract fun start(): Rule

    open fun ignoreRowsNotStartingWith(key: String): Rule = ZeroOrMore(
        TestNot(IgnoreCase(key)),
        ZeroOrMore(NoneOf("\n")),
        OneOrMore('\n'),
    )

    open fun singleRowWithActionIgnorePrevious(key: String, action: Action<String>): Rule = Sequence(
        ignoreRowsNotStartingWith(key),
        singleRowWithAction(key, action),
    )

    open fun singleRowWithAction(key: String, action: Action<String>): Rule = Sequence(
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
