package io.mailguru.whois.model.exception

/**
 * Exception to be thrown on call of [io.mailguru.whois.parser.Parser.parse] if the requested whois server doesn't
 * provide any information (but the call succeeded).
 */
class NotPermittedException : RuntimeException()
