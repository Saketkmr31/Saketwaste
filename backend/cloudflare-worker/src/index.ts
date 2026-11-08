export interface Env {
  TANTRAKULAM_API_KEY: string;
  JWT_HS256_SECRET: string;
}

const UPSTREAM = "https://astro.tantragurukul.org";
const ROUTES: Record<string, { path: string; allowed: string[]; required: string[] }> = {
  "/api/panchanga": { path: "/v1/vedic/panchanga", allowed: ["date","place","lat","lon","tz","engine","time","lang"], required: ["date"] },
  "/api/kundali": { path: "/v1/vedic/kundali", allowed: ["date","time","place","lat","lon","tz","engine","ayanamsa","house","lang"], required: ["date","time","lat","lon","tz"] },
  "/api/muhurta": { path: "/v1/vedic/muhurta", allowed: ["date","place","lat","lon","tz","karya","span","engine","lang"], required: ["date","lat","lon","tz"] },
  "/api/dashas": { path: "/v1/vedic/dashas", allowed: ["date","time","lat","lon","tz","engine","as_of"], required: ["date","time","lat","lon","tz"] },
  "/api/divisional": { path: "/v1/vedic/divisionals", allowed: ["date","time","lat","lon","tz","engine","chart"], required: ["date","time","lat","lon","tz"] },
  "/api/matching": { path: "/v1/vedic/match/ashtakoot", allowed: ["boy_date","boy_time","boy_lat","boy_lon","boy_tz","girl_date","girl_time","girl_lat","girl_lon","girl_tz","engine","lang"], required: ["boy_date","boy_time","boy_lat","boy_lon","boy_tz","girl_date","girl_time","girl_lat","girl_lon","girl_tz"] }
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" } });
}
function b64url(value: string): Uint8Array {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "="));
  return Uint8Array.from(raw, c => c.charCodeAt(0));
}
async function verifyJwt(token: string, secret: string): Promise<boolean> {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return false;
    const [head, body, signature] = parts;
    const h = JSON.parse(new TextDecoder().decode(b64url(head)));
    const p = JSON.parse(new TextDecoder().decode(b64url(body)));
    if (h.alg !== "HS256" || typeof p.sub !== "string" || !p.sub) return false;
    if (typeof p.exp !== "number" || p.exp <= Math.floor(Date.now() / 1000)) return false;
    const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["verify"]);
    return crypto.subtle.verify("HMAC", key, b64url(signature), new TextEncoder().encode(head + "." + body));
  } catch { return false; }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method !== "POST") return json({ error: "POST required" }, 405);
    const route = ROUTES[new URL(request.url).pathname];
    if (!route) return json({ error: "Route not found" }, 404);
    if (!env.TANTRAKULAM_API_KEY || !env.JWT_HS256_SECRET) return json({ error: "Commercial gateway is not configured" }, 503);
    const auth = request.headers.get("authorization") || "";
    const token = auth.startsWith("Bearer ") ? auth.slice(7) : "";
    if (!(await verifyJwt(token, env.JWT_HS256_SECRET))) return json({ error: "Unauthorised" }, 401);

    let input: Record<string, unknown>;
    try { input = await request.json() as Record<string, unknown>; }
    catch { return json({ error: "Invalid JSON body" }, 400); }
    const params = new URLSearchParams();
    for (const key of route.allowed) {
      const value = input[key];
      if (value === undefined || value === null || String(value).trim() === "") continue;
      if (!["string", "number", "boolean"].includes(typeof value)) return json({ error: "Invalid parameter type", field: key }, 400);
      params.set(key, String(value));
    }
    const missing = route.required.filter(key => !params.has(key));
    if (missing.length) return json({ error: "Missing required parameters", fields: missing }, 400);

    // Do not log request bodies: they can contain personal birth details.
    const upstream = await fetch(UPSTREAM + route.path + "?" + params.toString(), {
      headers: { "X-API-Key": env.TANTRAKULAM_API_KEY, "Accept": "application/json" }
    });
    return new Response(await upstream.text(), {
      status: upstream.status,
      headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
    });
  }
};
