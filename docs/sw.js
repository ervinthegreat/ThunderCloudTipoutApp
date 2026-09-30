// Cache-first service worker so the app keeps working offline after the first visit.
// 20260929203533 is replaced by build-web.ps1 on every build, which makes phones pick up new versions.
const CACHE = "thundercloud-20260929203533";
const ASSETS = [
  "./",
  "index.html",
  "classes.js",
  "manifest.webmanifest",
  "res/thunder.obj",
  "res/gems/amethyst.obj",
  "res/gems/diamond.obj",
  "res/gems/sapphire.obj",
  "res/gems/topaz.obj"
];

self.addEventListener("install", (event) => {
  // cache: "reload" skips the browser's HTTP cache (GitHub Pages sets max-age=600),
  // otherwise a new build could be cached with the previous build's files.
  event.waitUntil(
    caches.open(CACHE)
      .then((cache) => cache.addAll(ASSETS.map((url) => new Request(url, { cache: "reload" }))))
      .then(() => self.skipWaiting())
  );
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
