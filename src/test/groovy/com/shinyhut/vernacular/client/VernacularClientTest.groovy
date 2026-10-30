package com.shinyhut.vernacular.client

import spock.lang.Specification

class VernacularClientTest extends Specification {

    def "should reject mouse buttons outside of 1 to 8"() {
        given:
        def client = new VernacularClient(new VernacularConfig())

        when:
        client.updateMouseButton(button, true)

        then:
        thrown(IllegalArgumentException)

        where:
        button << [0, 9, -1]
    }

    def "should stop and report the error if starting fails with an unexpected exception"() {
        given:
        def errors = []
        def config = new VernacularConfig()
        config.errorListener = { errors << it }
        def client = new VernacularClient(config)
        def socket = new Socket() {
            @Override
            InputStream getInputStream() {
                throw new IllegalStateException("test")
            }
        }

        when:
        client.start(socket)

        then:
        !client.running
        errors.size() == 1
        errors[0].cause instanceof IllegalStateException
    }

    def "should reject a target frame rate below 1"() {
        when:
        new VernacularConfig().targetFramesPerSecond = fps

        then:
        thrown(IllegalArgumentException)

        where:
        fps << [0, -1]
    }
}
