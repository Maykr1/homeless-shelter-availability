const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const webRoot = path.resolve(__dirname, "..");

function read(relativePath) {
  return fs.readFileSync(path.join(webRoot, relativePath), "utf8");
}

test("index.html loads runtime config before the frontend bundle", () => {
  const html = read("dist/index.html");

  assert.match(html, /<script src="\/config\.js"><\/script>/);
  assert.match(
    html,
    /<script type="module" crossorigin src="\/assets\/index-J7a6VFdS\.js"><\/script>/,
  );
  assert.match(html, /<div id="root"><\/div>/);
  assert.ok(
    html.indexOf("/config.js") < html.indexOf("/assets/index-J7a6VFdS.js"),
    "config.js should load before the bundle so runtime config is available",
  );
});

test("config template exposes the expected runtime variables", () => {
  const template = read("dist/config.template.js");

  assert.match(
    template,
    /GOOGLE_MAPS_API_KEY: "\$\{GOOGLE_MAPS_API_KEY\}"/,
  );
  assert.match(template, /API_BASE_URL: "\$\{API_BASE_URL\}"/);
});

test("docker entrypoint generates config.js from env vars and preserves existing values", () => {
  const entrypoint = read("docker-entrypoint.d/40-generate-config.sh");

  assert.match(entrypoint, /envsubst/);
  assert.match(entrypoint, /GOOGLE_MAPS_API_KEY/);
  assert.match(entrypoint, /API_BASE_URL/);
  assert.match(entrypoint, /read_existing_value/);
  assert.match(entrypoint, /sed -n/);
  assert.match(entrypoint, /config\.template\.js/);
  assert.match(entrypoint, /config\.js/);
});

test("docker build keeps dist/config.js available as the runtime fallback source", () => {
  const dockerignore = read(".dockerignore");

  assert.match(dockerignore, /!dist\/config\.js/);
});

test("bundle still reads runtime app config instead of only hardcoded values", () => {
  const bundle = read("dist/assets/index-J7a6VFdS.js");

  assert.match(bundle, /__APP_CONFIG__/);
  assert.match(bundle, /GOOGLE_MAPS_API_KEY/);
  assert.match(bundle, /API_BASE_URL/);
  assert.match(bundle, /\/api\/shelters/);
  assert.match(bundle, /Shelters currently in the API/);
  assert.doesNotMatch(bundle, /Mock listings ready for the frontend/);
});
