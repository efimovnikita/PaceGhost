# 04: Repository Integration and Heatmap Tile Cache Invalidation

**What to build:** Automatic invalidation and cleanup of the disk tile cache whenever the user records a new walk, imports a GPX file, deletes a walk, or clears database history, guaranteeing that the heatmap never displays stale route segments.

**Blocked by:** 03: Full-Screen Heatmap Screen, Inverted Base Map, and Navigation

**Status:** ready-for-agent

- [ ] Heatmap tile cache directory is localized in `context.cacheDir.resolve("heatmap_tiles")`
- [ ] Saving a completed walk (`WalkRepository.insertWalk`) clears the heatmap cache directory
- [ ] Importing GPX tracks (`GpxImporter`) triggers heatmap cache invalidation
- [ ] Deleting a walk (`deleteWalk` or `clearAllWalks`) purges cached tiles
- [ ] Subsequent views to `HeatmapScreen` cleanly re-rasterize tiles on-demand using current database contents
