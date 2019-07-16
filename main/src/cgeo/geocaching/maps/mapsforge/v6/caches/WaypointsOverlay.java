package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.mapsforge.map.layer.Layer;

public class WaypointsOverlay extends AbstractCachesOverlay {
    public WaypointsOverlay(final int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers) {
        super(overlayId, geoEntries, bundle, anchorLayer, mapHandlers);
    }

    void hideWaypoints() {
        final Collection<String> removeCodes = getGeocodes();
        final Collection<String> newCodes = new HashSet<>();

        syncLayers(removeCodes, newCodes);
    }

    private Set<Waypoint> filterWaypoints(final Collection<String> baseGeoCodes, final boolean showStored) {
        final Set<Waypoint> waypoints = new HashSet<>();

        final Set<Geocache> baseCaches = DataStore.loadCaches(baseGeoCodes, LoadFlags.LOAD_WAYPOINTS);

        for (final Geocache cache : baseCaches) {
            waypoints.addAll(cache.getWaypoints());
        }

        if (showStored) {
            final boolean excludeMine = Settings.isExcludeMyCaches();
            final boolean excludeDisabled = Settings.isExcludeDisabledCaches();
            final CacheType type = Settings.getCacheType();

            final Set<Waypoint> waypointsInViewport = DataStore.loadWaypoints(getViewport(), excludeMine, excludeDisabled, type);
            MapUtils.filter(waypointsInViewport);
            waypoints.addAll(waypointsInViewport);
        }

        return waypoints;
    }

    void showWaypoints(final Collection<String> baseGeoCodes, final boolean showStored) {
        final Collection<String> removeCodes = getGeocodes();
        final Collection<String> newCodes = new HashSet<>();

        final Set<Waypoint> waypoints = filterWaypoints(baseGeoCodes, showStored);
        final boolean isDotMode = Settings.isDotMode();

        for (final Waypoint waypoint : waypoints) {
            if (waypoint == null || waypoint.getCoords() == null) {
                continue;
            }
            if (removeCodes.contains(waypoint.getGpxId())) {
                removeCodes.remove(waypoint.getGpxId());
            } else {
                if (addItem(waypoint, isDotMode)) {
                    newCodes.add(waypoint.getGpxId());
                }
            }
        }

        syncLayers(removeCodes, newCodes);
    }

    public int getClosestDistanceInM(final AbstractCachesOverlay baseOverlay, final Geopoint coord) {
        int minDistance = 50000000;
        if (null != baseOverlay) {
            final Set<Waypoint> waypoints = filterWaypoints(baseOverlay.getCacheGeocodes(), true);
            for (final Waypoint waypoint : waypoints) {
                final int distance = (int) (1000f * waypoint.getCoords().distanceTo(coord));
                if (distance > 0 && distance < minDistance) {
                    minDistance = distance;
                }
            }
        }
        return minDistance;
    }
}