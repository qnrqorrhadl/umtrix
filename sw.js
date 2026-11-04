// 엄트리스 오프라인 저장: 인터넷이 되면 항상 최신 index.html을 받아오고, 안 되면 저장해 둔 걸 보여줘요.
const CACHE = "umtris-pwa";
self.addEventListener("install", (e) => { self.skipWaiting(); });
self.addEventListener("activate", (e) => { e.waitUntil(self.clients.claim()); });
self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.method !== "GET" || new URL(req.url).origin !== location.origin) return;   // 점수 서버 등 다른 주소는 건드리지 않음
  e.respondWith(
    fetch(req).then((res) => {
      const copy = res.clone();
      caches.open(CACHE).then((c) => c.put(req, copy)).catch(() => {});
      return res;
    }).catch(() => caches.match(req).then((r) => r || caches.match("./")))
  );
});
