package com.geotrack.simulator;

import com.geotrack.simulator.route.GpxParser;
import com.geotrack.simulator.route.RoutePoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GpxParser")
class GpxParserTest {

    private final GpxParser parser = new GpxParser();

    private static final String SAMPLE_GPX = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="test">
              <trk>
                <name>Newcastle Quayside</name>
                <trkseg>
                  <trkpt lat="54.9695" lon="-1.5993">
                    <ele>10.5</ele>
                    <time>2026-01-15T10:00:00Z</time>
                  </trkpt>
                  <trkpt lat="54.9700" lon="-1.5950">
                    <ele>12.0</ele>
                    <time>2026-01-15T10:00:30Z</time>
                  </trkpt>
                  <trkpt lat="54.9710" lon="-1.5890">
                    <ele>8.3</ele>
                    <time>2026-01-15T10:01:00Z</time>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
            """;

    @Test
    @DisplayName("parses GPX track points with lat/lon/ele/time")
    void parsesTrackPoints() {
        List<RoutePoint> points = parser.parse(toStream(SAMPLE_GPX));

        assertThat(points).hasSize(3);
        assertThat(points.get(0).latitude()).isEqualTo(54.9695);
        assertThat(points.get(0).longitude()).isEqualTo(-1.5993);
        assertThat(points.get(0).elevation()).isEqualTo(10.5);
        assertThat(points.get(0).timestamp()).isNotNull();
    }

    @Test
    @DisplayName("preserves point ordering")
    void preservesOrder() {
        List<RoutePoint> points = parser.parse(toStream(SAMPLE_GPX));

        assertThat(points.get(0).latitude()).isLessThan(points.get(1).latitude());
        assertThat(points.get(1).latitude()).isLessThan(points.get(2).latitude());
    }

    @Test
    @DisplayName("handles GPX with no elevation")
    void handlesNoElevation() {
        String gpx = """
                <?xml version="1.0"?>
                <gpx version="1.1">
                  <trk><trkseg>
                    <trkpt lat="54.97" lon="-1.60">
                      <time>2026-01-15T10:00:00Z</time>
                    </trkpt>
                  </trkseg></trk>
                </gpx>
                """;

        List<RoutePoint> points = parser.parse(toStream(gpx));

        assertThat(points).hasSize(1);
        assertThat(points.get(0).elevation()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("handles empty track segment")
    void handlesEmptyTrack() {
        String gpx = """
                <?xml version="1.0"?>
                <gpx version="1.1"><trk><trkseg></trkseg></trk></gpx>
                """;

        assertThat(parser.parse(toStream(gpx))).isEmpty();
    }

    @Test
    @DisplayName("throws on invalid XML")
    void throwsOnInvalidXml() {
        var stream = toStream("not xml");
        assertThatThrownBy(() -> parser.parse(stream))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse GPX");
    }

    private ByteArrayInputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
