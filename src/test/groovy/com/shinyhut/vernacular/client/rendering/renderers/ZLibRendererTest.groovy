package com.shinyhut.vernacular.client.rendering.renderers

import com.shinyhut.vernacular.client.exceptions.InvalidMessageException
import com.shinyhut.vernacular.client.rendering.ImageBuffer
import com.shinyhut.vernacular.protocol.messages.PixelFormat
import com.shinyhut.vernacular.protocol.messages.Rectangle
import spock.lang.Specification

import java.util.zip.Deflater

import static com.shinyhut.vernacular.protocol.messages.Encoding.ZLIB

class ZLibRendererTest extends Specification {

    def pixelFormat = new PixelFormat(8, 8, true, true, 7, 7, 3, 0, 3, 6)
    def deflater = new Deflater()
    def renderer = new ZLibRenderer(new RawRenderer(new PixelDecoder([:]), pixelFormat), pixelFormat)
    def destination = new ImageBuffer(4, 4, false)

    // Compresses the data as part of one continuous zlib stream, as servers do
    private InputStream message(List<Integer> pixels) {
        def compressed = new byte[1024]
        deflater.setInput(pixels as byte[])
        int length = deflater.deflate(compressed, 0, compressed.length, Deflater.SYNC_FLUSH)
        def out = new ByteArrayOutputStream()
        new DataOutputStream(out).writeInt(length)
        out.write(compressed, 0, length)
        new ByteArrayInputStream(out.toByteArray())
    }

    def "should render consecutive rectangles of one zlib stream"() {
        when:
        renderer.render(message([0xff] * 4), destination, new Rectangle(0, 0, 2, 2, ZLIB))
        renderer.render(message([0xff] * 2), destination, new Rectangle(2, 0, 2, 1, ZLIB))

        then:
        destination.get(1, 1) == new Pixel(255, 255, 255).toInt()
        destination.get(3, 0) == new Pixel(255, 255, 255).toInt()
        destination.get(3, 1) == 0
    }

    def "should reject a negative compressed length"() {
        given:
        def input = new ByteArrayInputStream([0xff, 0xff, 0xff, 0xff] as byte[])

        when:
        renderer.render(input, destination, new Rectangle(0, 0, 1, 1, ZLIB))

        then:
        thrown(InvalidMessageException)
    }

    def "should reject more decompressed data than the rectangle contains"() {
        when:
        renderer.render(message([0xff] * 5), destination, new Rectangle(0, 0, 2, 2, ZLIB))

        then:
        thrown(InvalidMessageException)
    }
}
