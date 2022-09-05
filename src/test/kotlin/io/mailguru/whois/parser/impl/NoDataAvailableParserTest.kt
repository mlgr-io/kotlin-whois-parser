package io.mailguru.whois.parser.impl

import io.kotest.assertions.throwables.shouldThrow
import io.mailguru.whois.TestUtil
import io.mailguru.whois.model.exception.NotPermittedException
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.fail

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NoDataAvailableParserTest {

    private val sut = NoDataAvailableParser()

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/whois/no-data-available/example-ch.txt",
            "/whois/no-data-available/example-es.txt",
        ]
    )
    fun `expect an exception on every parse() call`(inputFilename: String) {
        TestUtil.getResourceAsText(inputFilename)?.let { input ->
            shouldThrow<NotPermittedException> {
                sut.parse(input)
            }
        } ?: fail("Input may not be null.")
    }
}
