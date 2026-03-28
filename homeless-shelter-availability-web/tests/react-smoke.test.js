const test = require("node:test");
const assert = require("node:assert/strict");
const React = require("react");
const { renderToStaticMarkup } = require("react-dom/server");

test("react can render a simple frontend tree", () => {
  const markup = renderToStaticMarkup(
    React.createElement(
      "main",
      { role: "main" },
      React.createElement("h1", null, "Shelter Access"),
      React.createElement(
        "p",
        null,
        "Frontend test smoke check",
      ),
    ),
  );

  assert.equal(
    markup,
    '<main role="main"><h1>Shelter Access</h1><p>Frontend test smoke check</p></main>',
  );
});
