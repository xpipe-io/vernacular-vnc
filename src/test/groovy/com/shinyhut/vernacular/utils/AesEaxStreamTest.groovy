package com.shinyhut.vernacular.utils

import spock.lang.Specification

class AesEaxStreamTest extends Specification {

    def "should encrypt data larger than 8 KB which decrypts back to the original data"() {
        given:
        def key = new byte[16]
        def data = (0..<20000).collect { (byte) it } as byte[]
        def encrypted = new ByteArrayOutputStream()

        when:
        new AesEaxOutputStream(key, encrypted).write(data)
        def input = new AesEaxInputStream(key, new ByteArrayInputStream(encrypted.toByteArray()))

        then:
        input.read() == data
    }

    def "should split data exceeding the maximum message length into multiple messages"() {
        given:
        def key = new byte[16]
        def data = (0..<100000).collect { (byte) it } as byte[]
        def encrypted = new ByteArrayOutputStream()

        when:
        new AesEaxOutputStream(key, encrypted).write(data)
        def input = new AesEaxInputStream(key, new ByteArrayInputStream(encrypted.toByteArray()))
        def first = input.read()
        def second = input.read()
        def decrypted = new ByteArrayOutputStream()
        decrypted.write(first)
        decrypted.write(second)

        then:
        first.length == 65535
        second.length == 100000 - 65535
        decrypted.toByteArray() == data
    }
}
