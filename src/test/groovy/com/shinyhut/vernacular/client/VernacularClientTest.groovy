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

    def "should reject a target frame rate below 1"() {
        when:
        new VernacularConfig().targetFramesPerSecond = fps

        then:
        thrown(IllegalArgumentException)

        where:
        fps << [0, -1]
    }
}
