/*
 * ISC License Copyright (c) 2004-2010 by Internet Systems Consortium, Inc. ("ISC")
 *
 * Copyright (c) 2021 by Jamie Redding
 *
 * Permission to use, copy, modify, and /or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above copyright
 * notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES WITH REGARD
 * TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.
 * IN NO EVENT SHALL ISC BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
 * WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING
 * OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package dev.coldhands.jersey.properties.core.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileBasedPropertyResolverTest {

    @TempDir
    Path tempDir;

    private Path propertyFile;

    @BeforeEach
    void setUp() {
        propertyFile = tempDir.resolve("file.properties");
    }

    @Nested
    class Constructor {

        @Test
        void whenPropertyFileDoesNotExist_thenThrowFileNotFoundException() {
            assertThatThrownBy(() -> new FileBasedPropertyResolver(propertyFile))
                    .isInstanceOf(FileNotFoundException.class)
                    .hasMessage("Property file does not exist: " + propertyFile.toAbsolutePath());
        }

        @Test
        void whenIOExceptionOccursReadingPropertyFile_thenThrowIOException() throws IOException {
            Files.createFile(propertyFile);
            final var expectedException = new IOException("boo");

            assertThatThrownBy(() ->
                    new FileBasedPropertyResolver(propertyFile, getInputStreamProviderThatThrows(expectedException)))
                    .isInstanceOf(IOException.class)
                    .hasCause(expectedException)
                    .hasMessage("Problem reading property file: " + propertyFile.toAbsolutePath());
        }

        @Test
        void canConstructSuccessfully() throws IOException {
            Files.createFile(propertyFile);

            new FileBasedPropertyResolver(propertyFile);
        }

        private FileBasedPropertyResolver.InputStreamProvider getInputStreamProviderThatThrows(IOException e) {
            return path -> {
                throw e;
            };
        }
    }

    @Nested
    class GetProperty {

        @Test
        void whenPropertyDoesNotExist_thenReturnNull() throws IOException {
            Files.createFile(propertyFile);

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("port")).isNull();
        }

        @Test
        void whenPropertyExistsInPropertyFile_thenReturnThatPropertyValue() throws IOException {
            Files.writeString(propertyFile, "port=8080\nhost=localhost\n");

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("port")).isEqualTo("8080");
            assertThat(underTest.getProperty("host")).isEqualTo("localhost");
        }

        @Test
        void whenPropertyValueContainsAnEquals_thenPreserveTheEquals() throws IOException {
            Files.writeString(propertyFile, "port=8080=8080");

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("port")).isEqualTo("8080=8080");
        }

        @Test
        void whenPropertyNameHasLeadingAndTrailingWhiteSpace_thenIgnoreTheWhitespace() throws IOException {
            Files.writeString(propertyFile, "\t  port  \t =8080");

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("port")).isEqualTo("8080");
        }

        @Test
        void whenPropertyValueHasLeadingAndTrailingWhiteSpace_thenIgnoreOnlyTheLeadingWhitespace() throws IOException {
            Files.writeString(propertyFile, "port=\t  \t8080\t  \t");

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("port")).isEqualTo("8080\t  \t");
        }

        @ParameterizedTest(name = "{0} line endings")
        @CsvSource({
                "unix,'port=8080\nhost=localhost\n'",
                "windows,'port=8080\r\nhost=localhost\r\n'"
        })
        void supportLineEndings(String platformName, String propertyFileContent) throws IOException {
            Files.writeString(propertyFile, propertyFileContent);

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("port")).isEqualTo("8080");
            assertThat(underTest.getProperty("host")).isEqualTo("localhost");
        }

        @Test
        void whenALineIsWhitespaceOnly_thenIgnoreThatLine() throws IOException {
            Files.writeString(propertyFile, "port=8080\n\n\t \n\r\n");

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("port")).isEqualTo("8080");
            assertThat(underTest.getProperty("\n")).isNull();
            assertThat(underTest.getProperty("\r\n")).isNull();
            assertThat(underTest.getProperty("\t ")).isNull();
        }

        @Test
        void whenALineStartsWithAHash_thenIgnoreThatLine() throws IOException {
            Files.writeString(propertyFile, "#port=8080");

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("port")).isNull();
        }

        @Test
        void whenAPropertyNameContainsAHash_thenIncludeTheHashInThePropertyName() throws IOException {
            Files.writeString(propertyFile, "po#rt=8080");

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("po#rt")).isEqualTo("8080");
        }

        @Test
        void whenAPropertyValueContainsAHash_thenIncludeTheHashAndTrailingCharacters() throws IOException {
            Files.writeString(propertyFile, "port=80#80");

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("port")).isEqualTo("80#80");
        }

        @Test
        void whenAPropertyIsDuplicated_thenIncludeTheLatestOne() throws IOException {
            Files.writeString(propertyFile, "port=8080\nport=9090\nport=10000\n");

            final PropertyResolver underTest = new FileBasedPropertyResolver(propertyFile);

            assertThat(underTest.getProperty("port")).isEqualTo("10000");
        }
    }

}