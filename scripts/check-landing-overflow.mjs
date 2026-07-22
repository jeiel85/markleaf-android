// Fails if any landing/privacy page scrolls horizontally at a set of
// narrow-to-wide viewport widths — the class of layout breakage that #186 had
// to find by hand across all six languages (#187). Version *strings* are
// checked by verify-landing-versions.ps1; this checks that the rendered page
// actually fits inside its viewport.
//
// Run: node scripts/check-landing-overflow.mjs
// Requires the `playwright` package + its chromium browser (installed in CI).
import { chromium } from 'playwright';
import { readdirSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import path from 'node:path';

const DOCS = path.resolve('docs');
// Common phone/tablet/desktop widths, incl. the 320px floor where the worst
// #186 overflows lived.
const WIDTHS = [320, 360, 375, 414, 768, 1280];
const TOLERANCE = 1; // sub-pixel rounding slack

const pages = readdirSync(DOCS)
  .filter((f) => f.endsWith('.html') && (f.startsWith('index') || f.startsWith('privacy')))
  .sort();

if (pages.length === 0) {
  console.error('No landing/privacy pages found under docs/.');
  process.exit(1);
}

const browser = await chromium.launch();
const failures = [];

for (const file of pages) {
  const url = pathToFileURL(path.join(DOCS, file)).href;
  for (const width of WIDTHS) {
    const page = await browser.newPage({ viewport: { width, height: 900 } });
    await page.goto(url, { waitUntil: 'load' });
    const result = await page.evaluate((tol) => {
      const de = document.documentElement;
      const overflow = de.scrollWidth - de.clientWidth;
      const vw = de.clientWidth;
      const offenders = [];
      if (overflow > tol) {
        for (const el of document.body.querySelectorAll('*')) {
          const r = el.getBoundingClientRect();
          if (r.width === 0 || r.height === 0) continue;
          if (r.right > vw + tol) {
            const id = el.id ? `#${el.id}` : '';
            const cls =
              typeof el.className === 'string' && el.className.trim()
                ? '.' + el.className.trim().split(/\s+/).join('.')
                : '';
            offenders.push(`${el.tagName.toLowerCase()}${id}${cls} (right=${Math.round(r.right)})`);
          }
        }
      }
      return { overflow: Math.round(overflow), vw, offenders: offenders.slice(0, 4) };
    }, TOLERANCE);
    if (result.overflow > TOLERANCE) {
      failures.push(
        `${file} @ ${width}px: page overflows by ${result.overflow}px (viewport ${result.vw}px). ` +
          `Widest: ${result.offenders.join(', ') || 'n/a'}`
      );
    }
    await page.close();
  }
}

await browser.close();

if (failures.length) {
  console.error(`FAIL: horizontal overflow on ${failures.length} page/width combination(s):`);
  for (const f of failures) console.error('  - ' + f);
  process.exit(1);
}

console.log(`OK: ${pages.length} pages x ${WIDTHS.length} widths, no horizontal overflow.`);
