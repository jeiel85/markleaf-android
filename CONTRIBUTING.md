# Contributing to Markleaf

Markleaf is maintained by one person, so the most useful thing a contribution
can do is arrive in a shape that is easy to check. This page says what that
shape is.

- **Bugs** → [Issues](https://github.com/jeiel85/markleaf-android/issues).
  Steps to reproduce, your device and Android version, and the app version from
  *Settings → About* are what make a report fixable.
- **Ideas and questions** → [Discussions](https://github.com/jeiel85/markleaf-android/discussions).
  Feature requests are weighed against what Markleaf is for — fast, local-first,
  no accounts, no network, no tracking — so a request that needs any of those
  will be declined however good it is.
- **Translations** → read on. This is where an outside contribution helps most,
  because the maintainer does not read most of the languages the app ships in.

Everything public here — issues, pull requests, commit messages — is in
English by default. A translation issue may be written in the language it is
about; say what is wrong in English if you can, but a report in your own
language is better than no report.

## Translations

Markleaf's interface is available in the languages listed in
[`config/locales.tsv`](config/locales.tsv). English is the source; every other
language is a translation of it.

Most of those translations have never been read by a native speaker. When a
new English string lands, it is translated into every language in the same
pull request, because the build refuses to ship a string that is still in
English. That keeps the app complete, but it means awkward or wrong wording can
sit unnoticed for a long time. Telling us about it is the single most useful
thing you can do for your language.

There are three ways to help, from least to most effort.

### 1. Report a wrong or awkward translation

No Git needed. Open a
[translation issue](https://github.com/jeiel85/markleaf-android/issues/new?template=translation-fix.yml),
name the language, and paste the text you see in the app with what it should
say instead. A screenshot helps when the same words appear in several places.

### 2. Fix an existing translation

Edit the files for your language and open a pull request:

| What | File |
|---|---|
| App interface | `app/src/main/res/values-<code>/strings.xml` |
| Notes shown on first launch | `app/src/main/res/raw-<code>/starter_notes.md` |
| Store listing (Play, F-Droid) | `fastlane/metadata/android/<store>/short_description.txt` and `full_description.txt` |

`<code>` and `<store>` are the first two columns of `config/locales.tsv` — for
example `values-fr/` and `fastlane/metadata/android/fr-FR/`. The English
originals are in `values/`, `raw/` and `en-US/`.

Please leave per-release notes (`fastlane/.../changelogs/`) alone: those are
written for each release and are not kept up to date afterwards.

You do not need to fix everything at once. A pull request that corrects five
strings is easy to review and merge; one that rewrites the whole file is not,
because nobody else can check it.

### 3. Add a new language

Open a
[new-language issue](https://github.com/jeiel85/markleaf-android/issues/new?template=new-language.yml)
first, so two people do not translate the same language at the same time.

Then send a pull request with:

- **Required:** `app/src/main/res/values-<code>/strings.xml` — a copy of
  `values/strings.xml` with every value translated.
- **Optional:** `app/src/main/res/raw-<code>/starter_notes.md` — a translation
  of `raw/starter_notes.md`. Without it, the app shows the English starter
  notes on that language's devices, which works but feels unfinished.
- **Optional:** the store listing, `fastlane/metadata/android/<store>/short_description.txt`
  and `full_description.txt`.

That is all you are asked for. A shipped language also has a landing page,
privacy page, README, release notes and demo clip, and the build checks that
every one of them exists — so **a pull request that adds only the files above
will fail CI, and that is expected.** The maintainer adds the rest in a
follow-up and keeps your pull request's wording; you will be credited in
[`THANKS.md`](THANKS.md) and in the release notes. Slovak arrived exactly this
way ([#453](https://github.com/jeiel85/markleaf-android/pull/453), then
[#454](https://github.com/jeiel85/markleaf-android/pull/454)).

After the language ships, new English strings will be machine-translated into
it until someone reads them. If you are willing to look over new strings from
time to time, say so in the issue.

### Rules the build checks

These are the mistakes translations have actually shipped with. The build
catches them, but it is quicker to get them right the first time.

**In `strings.xml`:**

- **Keep every key.** Do not delete a `<string>` because it seems unused, and
  do not add keys that are not in `values/strings.xml`.
- **Keep placeholders exactly.** `%1$s`, `%1$d`, `%2$d` must appear in your
  translation with the same numbers and letters. You may move them within the
  sentence. Do not add a `%` sign that the English does not have.
- **Escape apostrophes and quotes** as `\'` and `\"`. A bare `'` breaks the
  build. Use `&amp;` and `&lt;` for `&` and `<`.
- **Plurals** (`<plurals>`) use your language's own categories. Russian, for
  example, needs `one`, `few`, `many` and `other`; Japanese needs only
  `other`. Every `<item>` keeps the same placeholder.
- **Do not leave English in place.** A value identical to the English fails
  the build, unless it is genuinely the same word in your language (a brand
  name, `Markdown`, `PDF`). If so, say which keys in the pull request and the
  maintainer will add them to the exception list.
- **"Markleaf" is a name** and is not translated.

**In `starter_notes.md`** — the app reads this file's structure, not just its
text, and the Slovak translation shipped four of these as bugs before a test
caught them:

- `---markleaf-note---` separates the notes. Keep every one, exactly as written.
- Callout keywords stay in English: `> [!TIP]`, `[!NOTE]`, `[!IMPORTANT]`,
  `[!WARNING]`, `[!CAUTION]`. Translate the text after them.
- A tag is one word with no spaces: `#writing` may become `#pisanje`, but a
  two-word translation needs a hyphen or a single word instead.
- A wikilink `[[Like This]]` must stay on one line and name the translated
  title of another starter note.
- The first note's tour lists the other notes by their **translated** titles,
  in bold, exactly as their `#` headings read.
- Leave code blocks, URLs and image paths untouched.

**In the store listing:** `short_description.txt` is at most 80 characters and
`full_description.txt` at most 4000. The line that reads "UI in N languages
(…)" must keep a number that matches `config/locales.tsv`.

### Checking your work

You do not have to build the app to contribute — CI runs every check on your
pull request and names what failed. To run them yourself:

```bash
./gradlew testDebugUnitTest
```

and, for the language list, with [PowerShell 7](https://github.com/PowerShell/PowerShell):

```bash
pwsh scripts/verify-locales.ps1
```

The best check is the app itself: install a debug build, switch the app to
your language (Android 13+: *Settings → Apps → Markleaf → Language*), and read
every screen. Wording that is correct but too long shows up as text cut off on
a button.

## Code

Code pull requests are welcome for issues already discussed — please comment on
the issue before starting, so the approach can be agreed first. The rules the
project holds itself to are in [`AGENTS.md`](AGENTS.md) (in Korean) and
[`docs/AGENT_SPEC.md`](docs/AGENT_SPEC.md). Run `./gradlew testDebugUnitTest`
before pushing, and use [Conventional Commits](https://www.conventionalcommits.org/)
(`fix(editor): …`, `feat(list): …`).

By contributing, you agree that your contribution is licensed under the
project's [Apache License 2.0](LICENSE).
