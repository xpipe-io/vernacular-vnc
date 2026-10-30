package com.shinyhut.vernacular.client.rendering

import spock.lang.Specification

class ImageBufferTest extends Specification {

    def "should ignore writes outside of the buffer"() {
        given:
        def buffer = new ImageBuffer(4, 4, false)

        when:
        buffer.set(4, 0, 1)
        buffer.set(0, 4, 1)
        buffer.set(-1, 0, 1)

        then:
        buffer.buffer.every { it == 0 }
    }

    def "should clip rectangles exceeding the buffer"() {
        given:
        def buffer = new ImageBuffer(4, 4, false)

        when:
        buffer.fillRect(2, 2, 10, 10, 1)

        then:
        buffer.get(2, 2) == 1
        buffer.get(3, 3) == 1
        buffer.get(1, 1) == 0
        buffer.get(0, 3) == 0
        (buffer.buffer as List).count { it == 1 } == 4
    }

    def "should copy overlapping regions correctly"() {
        given:
        def buffer = new ImageBuffer(1, 4, false)
        (0..3).each { buffer.set(0, it, it + 1) }

        when: "a scroll down by one row"
        buffer.duplicate(0, 0, 1, 3, 0, 1)

        then:
        buffer.buffer as List == [1, 1, 2, 3]
    }
}
