// Cache-first service worker so the app keeps working offline after the first visit.
// 20260929185011 is replaced by build-web.ps1 on every build, which makes phones pick up new versions.
const CACHE = "thundercloud-20260929185011";
const ASSETS = [
  "./",
  "index.html",
  "classes.js",
  "manifest.webmanifest",
  "res/marble.png"
];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(ASSETS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  event.respondWith(
    caches.match(event.request, { ignoreSearch: true }).then((hit) => hit || fetch(event.request))
  );
});
