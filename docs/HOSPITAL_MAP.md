# Hospital map on the home page

Open Home > Hospitals. Select a branch in Find your hospital, or choose
View on map on its card. The embedded map searches the branch's saved address
and area in Sri Lanka. Directions and Open in Google Maps open external maps.

Branch names and addresses come from the existing hospital_branches records.
Edit inaccurate demonstration addresses in Admin > Hospital Branches. The map
is an address search, not a set of independently verified GPS coordinates.
No patient details are included in map requests. Internet access is needed for
the third-party map; the hospital list remains available without it.

Files: templates/index.html, static/js/hospital-map.js,
static/css/hospital-map.css. No database schema change or account setup is
needed. Rebuild and restart the application to pick up cached templates.
