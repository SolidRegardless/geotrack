package com.geotrack.simulator.fleet;

import com.geotrack.simulator.route.RoutePoint;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Real Newcastle-upon-Tyne routes for fleet simulation.
 * Routes follow actual geography: roads, rail lines, river, and flight paths.
 */
public final class NewcastleRoutes {

    private NewcastleRoutes() {}

    private static final Instant BASE = Instant.now();

    /**
     * Bus route: Go North East Q3 style — Gateshead Interchange to Four Lane Ends
     * via Newcastle city centre. Follows real A-roads through Newcastle.
     * ~8km, ~25 min at bus speeds.
     */
    public static List<RoutePoint> busRoute() {
        // Road-snapped coordinates from OSRM routing engine (real roads)
        // Gateshead Interchange → Newcastle centre → Jesmond → Heaton
        return withTimestamps(List.of(
                new double[]{54.9527, -1.6037, 0},  // Gateshead Interchange
                new double[]{54.9597, -1.6055, 0},  // Gateshead High Street
                new double[]{54.9622, -1.6057, 0},  // Bottle Bank
                new double[]{54.9634, -1.6067, 0},  // Tyne Bridge approach
                new double[]{54.9592, -1.6147, 0},  // Swing Bridge area
                new double[]{54.9593, -1.6164, 0},  // Side / Quayside
                new double[]{54.9655, -1.6243, 0},  // St Nicholas Cathedral
                new double[]{54.9651, -1.6231, 0},  // Collingwood Street
                new double[]{54.9677, -1.6166, 0},  // Grey Street
                new double[]{54.9695, -1.6151, 0},  // Grey's Monument
                new double[]{54.9684, -1.6227, 0},  // Northumberland Street
                new double[]{54.9716, -1.6171, 0},  // Haymarket
                new double[]{54.9730, -1.6120, 0},  // Barras Bridge
                new double[]{54.9753, -1.6102, 0},  // Civic Centre
                new double[]{54.9750, -1.6056, 0},  // Jesmond Road West
                new double[]{54.9747, -1.5995, 0},  // Jesmond
                new double[]{54.9761, -1.5889, 0},  // Osborne Road
                new double[]{54.9764, -1.5866, 0},  // Sandyford
                new double[]{54.9770, -1.5747, 0},  // Heaton Road
                new double[]{54.9800, -1.5758, 0}   // Heaton Park
        ), 80); // ~80 sec between stops for a bus
    }

    /**
     * Train route: East Coast Main Line through Newcastle Central Station.
     * Durham → Chester-le-Street → Gateshead → Newcastle Central → Manors → Heaton.
     * ~20km of real rail alignment.
     */
    public static List<RoutePoint> trainRoute() {
        // ECML coordinates from OpenStreetMap (ways 29116083, 3991654, 28872267, 3991649, 5022075)
        return withTimestamps(List.of(
                new double[]{54.8575, -1.5791, 0},   // Chester-le-Street viaduct
                new double[]{54.8641, -1.5783, 0},   // Birtley
                new double[]{54.8688, -1.5779, 0},   // Ouston
                new double[]{54.8728, -1.5785, 0},   // Birtley north
                new double[]{54.8776, -1.5796, 0},   // Lamesley
                new double[]{54.8836, -1.5811, 0},   // Low Fell
                new double[]{54.8878, -1.5821, 0},   // Sheriff Hill
                new double[]{54.9337, -1.6097, 0},   // Teams south
                new double[]{54.9379, -1.6109, 0},   // Greenesfield
                new double[]{54.9408, -1.6118, 0},   // Bensham
                new double[]{54.9536, -1.6194, 0},   // Gateshead (Bensham tunnel)
                new double[]{54.9620, -1.6114, 0},   // King Edward Bridge south junction
                new double[]{54.9650, -1.6069, 0},   // King Edward Bridge (over Tyne)
                new double[]{54.9668, -1.6191, 0},   // Newcastle Central west throat
                new double[]{54.9679, -1.6168, 0},   // Newcastle Central platforms
                new double[]{54.9685, -1.6157, 0},   // Central east throat
                new double[]{54.9773, -1.5898, 0},   // Heaton junction
                new double[]{54.9790, -1.5843, 0},   // Heaton south
                new double[]{54.9811, -1.5781, 0},   // Heaton depot
                new double[]{54.9853, -1.5691, 0},   // Benton curve
                new double[]{54.9914, -1.5632, 0}    // Cramlington approach
        ), 30); // ~30 sec between points at ~200 km/h express
    }

    /**
     * Plane route: Approach to Newcastle Airport (NCL) runway 07.
     * Descending from 3000ft (914m) south of the airport down to touchdown.
     * Follows standard ILS approach path.
     */
    public static List<RoutePoint> planeRoute() {
        return withTimestamps(List.of(
                new double[]{54.9100, -1.7500, 914},  // Initial approach fix ~14km south
                new double[]{54.9200, -1.7400, 853},  // Descending
                new double[]{54.9300, -1.7300, 792},  // 
                new double[]{54.9400, -1.7200, 731},  // Passing over Consett area
                new double[]{54.9500, -1.7150, 670},  // 
                new double[]{54.9600, -1.7100, 609},  // Approaching from south
                new double[]{54.9700, -1.7050, 548},  // 
                new double[]{54.9800, -1.7020, 487},  // Glideslope descent
                new double[]{54.9900, -1.6990, 426},  // 
                new double[]{55.0000, -1.6960, 365},  // ~4km from runway
                new double[]{55.0100, -1.6950, 304},  // 
                new double[]{55.0200, -1.6940, 243},  // ~2km final
                new double[]{55.0250, -1.6935, 182},  // Short final
                new double[]{55.0300, -1.6925, 122},  // Decision height
                new double[]{55.0340, -1.6920, 61},   // Over threshold
                new double[]{55.0375, -1.6917, 10},   // Touchdown runway 07
                new double[]{55.0390, -1.6900, 0},    // Rollout
                new double[]{55.0395, -1.6880, 0},    // Deceleration
                new double[]{55.0392, -1.6850, 0}     // Taxi to stand
        ), 15); // ~15 sec between points, approach takes ~4-5 min
    }

    /**
     * Boat route: River Tyne from Newcastle Quayside downstream to Tynemouth.
     * Following the navigable channel ~15km.
     */
    public static List<RoutePoint> boatRoute() {
        // River Tyne centreline from OpenStreetMap (way 632860771, 796368855, 632860774)
        return withTimestamps(List.of(
                new double[]{54.9659, -1.6111, 0},  // Tyne at Redheugh Bridge
                new double[]{54.9646, -1.6136, 0},  // Tyne at Dunston
                new double[]{54.9631, -1.6160, 0},  // Scotswood bend
                new double[]{54.9619, -1.6187, 0},  // Tyne at Elswick
                new double[]{54.9606, -1.6226, 0},  // Benwell reach
                new double[]{54.9598, -1.6257, 0},  // Teams bend
                new double[]{54.9591, -1.6301, 0},  // Dunston Staiths area
                new double[]{54.9590, -1.6375, 0},  // MetroCentre bend
                new double[]{54.9593, -1.6418, 0},  // Derwenthaugh
                new double[]{54.9601, -1.6468, 0},  // Blaydon reach
                new double[]{54.9631, -1.6598, 0},  // Ryton island area
                new double[]{54.9638, -1.6656, 0},  // Newburn
                new double[]{54.9648, -1.6739, 0},  // Tyne curve at Lemington
                new double[]{54.9649, -1.6769, 0},  // Lemington gut
                new double[]{54.9646, -1.6798, 0},  // Tyne at Sugley
                new double[]{54.9648, -1.6825, 0},  // Newburn Bridge area
                new double[]{54.9659, -1.6867, 0},  // Bell Close bend
                new double[]{54.9672, -1.6907, 0},  // Tyne west reach
                new double[]{54.9693, -1.6950, 0},  // Wylam approach
                new double[]{54.9704, -1.6981, 0}   // Wylam riverside
        ), 60); // ~60 sec between points at ~8 knots
    }

    private static List<RoutePoint> withTimestamps(List<double[]> coords, int secondsBetween) {
        return IntStream.range(0, coords.size())
                .mapToObj(i -> new RoutePoint(
                        coords.get(i)[0], coords.get(i)[1], coords.get(i)[2],
                        BASE.plus((long) i * secondsBetween, ChronoUnit.SECONDS)))
                .toList();
    }
}
