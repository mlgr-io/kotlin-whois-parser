package io.mailguru.whois.model.exception

/**
 * Exception to be thrown when a proper parser implementation could not be found.
 */
class ParserNotImplementedException(message: String) : RuntimeException(message)
