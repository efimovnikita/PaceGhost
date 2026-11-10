# 01: Room Spatial Index and Bounding Box Query

**What to build:** The database enables ultra-fast geographical range queries across historical track points by introducing a composite spatial index on latitude and longitude, accompanied by a dedicated DAO query that fetches ordered points inside an arbitrary geographic bounding box.

**Blocked by:** None (can start immediately)

**Status:** complete

- [x] `TrackPointEntity` declares a composite index on `(latitude, longitude)`
- [x] Room schema version is bumped or cleanly migrated so existing databases upgrade without errors
- [x] `WalkDao` includes `getPointsInBoundingBox(minLat, maxLat, minLon, maxLon)` returning points ordered by `walkId, id ASC`
- [x] Unit tests in `testDebugUnitTest` verify that points outside the bounding box are filtered out and points inside are returned in correct order
