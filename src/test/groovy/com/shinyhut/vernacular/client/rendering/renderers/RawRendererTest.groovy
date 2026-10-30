package com.shinyhut.vernacular.client.rendering.renderers

import com.shinyhut.vernacular.client.rendering.ImageBuffer
import com.shinyhut.vernacular.protocol.messages.PixelFormat
import com.shinyhut.vernacular.protocol.messages.Rectangle
import spock.lang.Specification

import static com.shinyhut.vernacular.protocol.messages.Encoding.RAW

class RawRendererTest extends Specification {

    def "should consume all pixel data of a rectangle exceeding the destination bounds"() {
        given:
        def pixelFormat = new PixelFormat(8, 8, true, true, 7, 7, 3, 0, 3, 6)
        def renderer = new RawRenderer(new PixelDecoder([:]), pixelFormat)
        def destination = new ImageBuffer(2, 2, false)
        def input = new ByteArrayInputStream(([0xff] * 9 + [0x42]) as byte[])

        when:
        renderer.render(input, destination, new Rectangle(1, 1, 3, 3, RAW))

        then:
        input.read() == 0x42
        destination.get(1, 1) == new Pixel(255, 255, 255).toInt()
        destination.get(0, 0) == 0
    }
}
