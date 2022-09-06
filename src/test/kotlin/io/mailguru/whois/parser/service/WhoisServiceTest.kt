package io.mailguru.whois.parser.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mailguru.whois.callPrivateMethod
import io.mailguru.whois.getPrivateProperty
import io.mailguru.whois.model.WhoisResult
import io.mailguru.whois.model.exception.NotPermittedException
import io.mailguru.whois.parser.Parser
import io.mailguru.whois.service.WhoisService
import io.mockk.called
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.apache.commons.net.whois.WhoisClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Locale
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WhoisServiceTest {

    companion object {
        const val IANA = "whois.iana.org"
        const val TESTNIC = "some.nic"
        const val TESTRESPONSE = "useless string data"
    }

    private val availableParsers: Map<String, Parser>? = WhoisService.getPrivateProperty("availableParsers")

    @BeforeEach
    fun beforeEach() {
        // clear the cache
        WhoisService.clearCache()

        // mock the subject to prepare offline test scenario
        mockkObject(WhoisService, recordPrivateCalls = true)
    }

    @AfterEach
    fun afterEach() {
        // Ensure this was a mocked offline test
        verify { (WhoisService getProperty "whois")?.wasNot(called) }

        // Unmockk
        unmockkAll()
    }

    @Nested
    inner class ParserMount {

        @ParameterizedTest
        @ValueSource(strings = ["whois.nic.ch", "whois.denic.de"])
        fun `the service should find all available parser implementations`(whoisServer: String) {
            availableParsers?.also { map ->
                withClue(String.format(Locale.ROOT, "Retrieve parser for%s:", whoisServer)) {
                    map[whoisServer] shouldNotBe null
                }
            } ?: fail("The map of available parsers should not be null.")
        }
    }

    @Nested
    inner class TestWhoisServerRetrieval {

        // Class tests getCachedResponsibleWhoisServer()

        @ParameterizedTest
        @CsvSource(
            "example.co.uk,uk",
            "example.com,com",
            "example.de,de",
        )
        fun `valid requests and responses`(hostname: String, tld: String) {

            // Mocking stuff

            every {
                WhoisService getProperty "availableParsers"
            } throws SocketException()

            every {
                WhoisService invoke "getResponseFromWhois" withArguments listOf(IANA, tld)
            } returns String.format(Locale.ROOT, "whois: %s", TESTNIC)

            every {
                WhoisService invoke "getResponseFromWhois" withArguments listOf(TESTNIC, hostname)
            } returns TESTRESPONSE

            val parserMock = mockk<Parser>().also { parserMock ->
                every { parserMock.parse(any(), any()) } throws SocketException()
                every { WhoisService getProperty "availableParsers" } returns mapOf(
                    TESTNIC to parserMock
                )
            }

            // Do the tests

            shouldThrow<SocketException> {
                WhoisService.lookup(hostname)
            }

            verifyOrder {
                WhoisService invoke "getWhoisServer" withArguments listOf(hostname)
                WhoisService invoke "getCachedResponsibleWhoisServer" withArguments listOf(tld)
                WhoisService invoke "getResponseFromWhois" withArguments listOf(IANA, tld)
                WhoisService invoke "getResponseFromWhois" withArguments listOf(TESTNIC, hostname)
                parserMock.parse(hostname, TESTRESPONSE)
            }
        }

        @ParameterizedTest
        @CsvSource(
            "example.co.uk,uk",
            "example.com,com",
            "example.de,de",
        )
        fun `invalid response from iana`(hostname: String, tld: String) {

            // Mocking stuff

            every {
                WhoisService invoke "getResponseFromWhois" withArguments listOf(IANA, tld)
            } returns TESTRESPONSE

            // Do the tests

            WhoisService.lookup(hostname) shouldBe null

            verifyOrder {
                WhoisService invoke "getWhoisServer" withArguments listOf(hostname)
                WhoisService invoke "getCachedResponsibleWhoisServer" withArguments listOf(tld)
                WhoisService invoke "getResponseFromWhois" withArguments listOf(IANA, tld)
            }

            verify(exactly = 1) {
                WhoisService invoke "getResponseFromWhois" withArguments listOf(any<String>(), any<String>())
            }

            verify { (WhoisService getProperty "availableParsers")?.wasNot(called) }
        }

        @ParameterizedTest
        @CsvSource(
            "example.co.uk,uk",
            "example.com,com",
            "example.de,de",
        )
        fun `iana call throws exception`(hostname: String, tld: String) {

            // Mocking stuff

            every {
                WhoisService invoke "getResponseFromWhois" withArguments listOf(IANA, tld)
            } throws IOException()

            // Do the tests

            WhoisService.lookup(hostname) shouldBe null

            verifyOrder {
                WhoisService invoke "getWhoisServer" withArguments listOf(hostname)
                WhoisService invoke "getCachedResponsibleWhoisServer" withArguments listOf(tld)
                WhoisService invoke "getResponseFromWhois" withArguments listOf(IANA, tld)
            }

            verify(exactly = 1) {
                WhoisService invoke "getResponseFromWhois" withArguments listOf(any<String>(), any<String>())
            }

            verify { (WhoisService getProperty "availableParsers")?.wasNot(called) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WhoisClientUsage {

        @Test
        fun `proper usage of that WhoisClient()`() {

            val hostname = "example.de"
            val tld = "de"

            // Mocking stuff

            val mockedResult = mockk<WhoisResult>()

            val parserMock = mockk<Parser>().also { parserMock ->
                every { parserMock.parse(any(), any()) } returns mockedResult
                every { WhoisService getProperty "availableParsers" } returns mapOf(
                    TESTNIC to parserMock
                )
            }

            val whoisClientMock = mockk<WhoisClient>().also { mock ->
                every { mock.connect(IANA) } just runs
                every { mock.connect(TESTNIC) } just runs

                every { mock.query(tld) } returns String.format(Locale.ROOT, "whois: %s", TESTNIC)
                every { mock.query(hostname) } returns TESTRESPONSE

                every { mock.disconnect() } just runs
            }

            every { WhoisService getProperty "whois" } returns whoisClientMock

            // Do the tests

            WhoisService.lookup(hostname) shouldBe mockedResult

            verifyOrder {
                WhoisService invoke "getWhoisServer" withArguments listOf(hostname)
                WhoisService invoke "getCachedResponsibleWhoisServer" withArguments listOf(tld)
                WhoisService invoke "getResponseFromWhois" withArguments listOf(IANA, tld)
                WhoisService invoke "getResponseFromWhois" withArguments listOf(TESTNIC, hostname)
                parserMock.parse(hostname, TESTRESPONSE)
            }
        }

        @Suppress("UnusedPrivateMember")
        private fun provideWhoisClientExceptions() = Stream.of(
            Arguments.of(IOException("")),
            Arguments.of(SocketException("")),
            Arguments.of(UnknownHostException("")),
        )

        @ParameterizedTest
        @MethodSource("provideWhoisClientExceptions")
        fun `exceptions on iana calls should be handled properly`(exception: Exception) {

            val hostname = "example.de"
            val tld = "de"

            // Mocking stuff

            val whoisClientMock = mockk<WhoisClient>().also { mock ->
                every { mock.connect(IANA) } throws exception

                every {
                    mock.query(any<String>())
                } answers { fail("whois.query(...) should not have been called.") }

                every {
                    mock.disconnect()
                } answers { fail("whois.disconnect(...) should not have been called.") }
            }

            every { WhoisService getProperty "whois" } returns whoisClientMock

            // Do the tests

            WhoisService.lookup(hostname) shouldBe null

            verifyOrder {
                WhoisService invoke "getWhoisServer" withArguments listOf(hostname)
                WhoisService invoke "getCachedResponsibleWhoisServer" withArguments listOf(tld)
                whoisClientMock.connect(IANA)
            }
        }

        @Test
        fun `consecutive calls to getWhoisServer() with related args should result in a single whois request`() {

            // Mocking stuff

            val whoisClientMock = mockk<WhoisClient>().also { mock ->
                every { mock.connect(IANA) } just runs
                every { mock.query(any<String>()) } returns String.format(Locale.ROOT, "whois: %s", TESTNIC)
                every { mock.disconnect() } just runs
            }

            every { WhoisService getProperty "whois" } returns whoisClientMock

            // Doing the first row of requests with all the same TLD

            setOf("example.com", "foo.com", "google.com").forEach { hostname ->
                val result: String? = WhoisService.callPrivateMethod("getWhoisServer", hostname)
                result shouldBe TESTNIC
            }

            verify(exactly = 1) {
                // connect() should have been called exactly once with "whois.iana.org" as argument
                whoisClientMock.connect(IANA)
                whoisClientMock.connect(any<String>())

                // query() should have been called exactly once with input as argument
                whoisClientMock.query("com")
                whoisClientMock.query(any<String>())

                whoisClientMock.disconnect()
            }

            // Request another TLD should result in a 2nd call to whois.iana.org

            val result: String? = WhoisService.callPrivateMethod("getWhoisServer", "anotherTLD.test")
            result shouldBe TESTNIC

            verify(exactly = 2) {
                // connect() should have been called with "whois.iana.org" as the only argument
                whoisClientMock.connect(IANA)
                whoisClientMock.connect(any<String>())

                whoisClientMock.query(any<String>())

                whoisClientMock.disconnect()
            }

            verify(exactly = 1) {
                // query() should have been called once with each TLD as argument
                whoisClientMock.query("com")
                whoisClientMock.query("test")
            }

            verifyOrder {
                whoisClientMock.connect(IANA)
                whoisClientMock.query("com")
                whoisClientMock.disconnect()

                whoisClientMock.connect(IANA)
                whoisClientMock.query("test")
                whoisClientMock.disconnect()
            }
        }
    }

    @ParameterizedTest
    @CsvSource(
        "ch,whois.nic.ch",
        "es,whois.nic.es",
        "li,whois.nic.li",
    )
    fun `some whois servers does not output any information at all`(tld: String, whoisServer: String) {

        val hostname = String.format(Locale.ROOT, "foobar.%s", tld)

        // Mocking stuff

        mockkObject(WhoisService, recordPrivateCalls = true)

        every {
            WhoisService invoke "getResponseFromWhois" withArguments listOf("whois.iana.org", tld)
        } returns String.format(Locale.ROOT, "whois: %s", whoisServer)

        every {
            WhoisService invoke "getResponseFromWhois" withArguments listOf(whoisServer, hostname)
        } returns TESTRESPONSE

        // Do the tests

        shouldThrow<NotPermittedException> {
            WhoisService.lookup(hostname)
        }

        verifyOrder {
            WhoisService invoke "getResponseFromWhois" withArguments listOf("whois.iana.org", tld)
            WhoisService invoke "getResponseFromWhois" withArguments listOf(whoisServer, hostname)
        }

        // Ensure this was a mocked offline test

        verify { (WhoisService getProperty "whois")?.wasNot(called) }
    }
}
