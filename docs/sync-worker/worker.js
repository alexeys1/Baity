function json(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      ...extraHeaders
    }
  });
}

function normalizeHexColor(value) {
  if (typeof value !== "string") return null;
  const m = value.match(/^#([A-Fa-f0-9]{6})$/);
  if (!m) return null;
  return `#${m[1].toUpperCase()}`;
}

function clampNumber(value, min, max, fallback) {
  const n = Number(value);
  if (!Number.isFinite(n)) return fallback;
  return Math.max(min, Math.min(max, n));
}

function sanitizeUserPayload(payload) {
  if (!payload || typeof payload !== "object") return null;
  const user = payload.user;
  if (!user || typeof user !== "object") return null;

  const uuid = String(user.uuid || "").trim();
  const name = String(user.name || "").trim();
  if (!/^[0-9a-fA-F-]{36}$/.test(uuid)) return null;
  if (!name || name.length > 32) return null;

  const isBaityUser = Boolean(user.isBaityUser);
  const features = user.features || {};
  const chromaRaw = features.nickTweaks || features.chromaOwnName || {};
  const smolRaw = features.smolPeople || {};

  const palette = Array.isArray(chromaRaw.palette)
    ? chromaRaw.palette.map(normalizeHexColor).filter(Boolean).slice(0, 16)
    : [];

  return {
    uuid: uuid.toLowerCase(),
    name,
    isBaityUser,
    features: {
      chromaOwnName: {
        nickTweaksEnabled: Boolean(chromaRaw.nickTweaksEnabled ?? true),
        enabled: Boolean(chromaRaw.enabled),
        speed: clampNumber(chromaRaw.speed, 0, 8, 1),
        palette: palette.length > 0 ? palette : ["#FF4D4D", "#FFAA00", "#FFFF66", "#66FF99", "#66CCFF", "#C299FF"],
        gradientStart: normalizeHexColor(chromaRaw.gradientStart) || "#FF0000",
        gradientEnd: normalizeHexColor(chromaRaw.gradientEnd) || "#0000FF"
      },
      nickTweaks: {
        nickTweaksEnabled: Boolean(chromaRaw.nickTweaksEnabled ?? true),
        enabled: Boolean(chromaRaw.enabled),
        speed: clampNumber(chromaRaw.speed, 0, 8, 1),
        palette: palette.length > 0 ? palette : ["#FF4D4D", "#FFAA00", "#FFFF66", "#66FF99", "#66CCFF", "#C299FF"],
        gradientStart: normalizeHexColor(chromaRaw.gradientStart) || "#FF0000",
        gradientEnd: normalizeHexColor(chromaRaw.gradientEnd) || "#0000FF"
      },
      smolPeople: {
        enabled: Boolean(smolRaw.enabled)
      }
    },
    meta: {
      protocol: 1,
      lastSeenAt: new Date().toISOString()
    }
  };
}

async function readIndex(env) {
  const raw = await env.PRESENCE_KV.get("users:index");
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

async function writeIndex(env, indexObj) {
  await env.PRESENCE_KV.put("users:index", JSON.stringify(indexObj));
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;

    if (request.method === "GET" && path === "/health") {
      return json({ ok: true, ts: Date.now() });
    }

    if (request.method === "POST" && path === "/report") {
      const token = request.headers.get("x-baity-token") || "";
      const writeToken = env.BAITY_WRITE_TOKEN || env.BAITY_TOKEN || "";
      if (!writeToken || token !== writeToken) {
        return json({ ok: false, error: "unauthorized" }, 401);
      }

      let body;
      try {
        body = await request.json();
      } catch {
        return json({ ok: false, error: "invalid_json" }, 400);
      }

      const sanitized = sanitizeUserPayload(body);
      if (!sanitized) return json({ ok: false, error: "invalid_payload" }, 400);

      const key = `user:${sanitized.uuid}`;
      await env.PRESENCE_KV.put(key, JSON.stringify(sanitized), { expirationTtl: 60 * 60 * 24 * 14 });

      const indexObj = await readIndex(env);
      indexObj[sanitized.uuid] = { name: sanitized.name, lastSeenAt: sanitized.meta.lastSeenAt };
      await writeIndex(env, indexObj);

      return json({ ok: true });
    }

    if (request.method === "GET" && path === "/users.json") {
      const indexObj = await readIndex(env);
      const uuids = Object.keys(indexObj);
      const users = {};
      const now = Date.now();

      for (const uuid of uuids) {
        const raw = await env.PRESENCE_KV.get(`user:${uuid}`);
        if (!raw) continue;
        try {
          const entry = JSON.parse(raw);
          const lastSeen = Date.parse(entry?.meta?.lastSeenAt || "");
          if (!Number.isFinite(lastSeen) || now - lastSeen > 1000 * 60 * 60 * 24 * 7) {
            continue;
          }
          users[uuid] = entry;
        } catch {
        }
      }

      return json({
        version: 1,
        updatedAt: new Date().toISOString(),
        users
      }, 200, { "cache-control": "public, max-age=15" });
    }

    return json({ ok: false, error: "not_found" }, 404);
  }
};
