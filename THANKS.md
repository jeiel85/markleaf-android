# Thanks

Markleaf is built by one person. Almost everything fixed or added since v2.17
started with somebody outside the project sitting down and writing out what was
wrong.

That is worth naming, because the alternative costs nothing. There are more
notes apps than anyone can try; installing one, disliking it and uninstalling it
is free, and it tells the developer nothing. Writing up what broke — with the
steps, the screenshot, the guess at which two features were fighting each other
— is unpaid work on something you have no stake in.

Several of the reports below found things that could not have been found from
the inside: a crash on a device I don't own, a language I don't read laying out
backwards, a duplicate-file bug that only shows up when the same folder is
opened by two apps.

## Reported

Listed by first report.

| | Reported |
|---|---|
| [@Dking08](https://github.com/Dking08) | [#131](https://github.com/jeiel85/markleaf-android/issues/131), [#139](https://github.com/jeiel85/markleaf-android/issues/139), [#142](https://github.com/jeiel85/markleaf-android/issues/142) |
| [@licaon-kter](https://github.com/licaon-kter) | [#132](https://github.com/jeiel85/markleaf-android/issues/132) |
| [@dav23r](https://github.com/dav23r) | [#133](https://github.com/jeiel85/markleaf-android/issues/133) |
| [@victoriamontero443-wq](https://github.com/victoriamontero443-wq) | [#134](https://github.com/jeiel85/markleaf-android/issues/134) |
| [@Plexprofile](https://github.com/Plexprofile) | [#135](https://github.com/jeiel85/markleaf-android/issues/135) |
| [@vidalperezbohoyo](https://github.com/vidalperezbohoyo) | [#136](https://github.com/jeiel85/markleaf-android/issues/136) |
| [@dope791](https://github.com/dope791) | [#137](https://github.com/jeiel85/markleaf-android/issues/137) |
| [@forr64](https://github.com/forr64) | [#138](https://github.com/jeiel85/markleaf-android/issues/138) |
| [@bushrang3r](https://github.com/bushrang3r) | [#140](https://github.com/jeiel85/markleaf-android/issues/140), [#144](https://github.com/jeiel85/markleaf-android/issues/144), [#148](https://github.com/jeiel85/markleaf-android/issues/148) |
| [@bellomondo](https://github.com/bellomondo) | [#141](https://github.com/jeiel85/markleaf-android/issues/141) |
| [@Alice-afg](https://github.com/Alice-afg) | [#143](https://github.com/jeiel85/markleaf-android/issues/143) |
| [@pescepalla](https://github.com/pescepalla) | [#145](https://github.com/jeiel85/markleaf-android/issues/145) |
| [@miladmp73](https://github.com/miladmp73) | [#146](https://github.com/jeiel85/markleaf-android/issues/146) |
| [@bit2bold](https://github.com/bit2bold) | [#155](https://github.com/jeiel85/markleaf-android/issues/155), [#199](https://github.com/jeiel85/markleaf-android/issues/199), [#279](https://github.com/jeiel85/markleaf-android/issues/279), [#280](https://github.com/jeiel85/markleaf-android/issues/280) |
| [@Cwpute](https://github.com/Cwpute) | [#188](https://github.com/jeiel85/markleaf-android/issues/188), [#189](https://github.com/jeiel85/markleaf-android/issues/189), [#190](https://github.com/jeiel85/markleaf-android/issues/190), [#191](https://github.com/jeiel85/markleaf-android/issues/191), [#192](https://github.com/jeiel85/markleaf-android/issues/192), [#193](https://github.com/jeiel85/markleaf-android/issues/193), [#213](https://github.com/jeiel85/markleaf-android/issues/213), [#214](https://github.com/jeiel85/markleaf-android/issues/214), [#215](https://github.com/jeiel85/markleaf-android/issues/215), [#216](https://github.com/jeiel85/markleaf-android/issues/216) |
| [@xentenza](https://github.com/xentenza) | [#197](https://github.com/jeiel85/markleaf-android/issues/197) |
| [@Me-2u](https://github.com/Me-2u) | [#200](https://github.com/jeiel85/markleaf-android/issues/200) |
| [@ElizabethWega](https://github.com/ElizabethWega) | [#283](https://github.com/jeiel85/markleaf-android/issues/283), [#298](https://github.com/jeiel85/markleaf-android/issues/298) |

Not every request here was accepted — a couple were declined, and saying no to a
thoughtful suggestion is its own kind of debt. Being told what you want from the
app is useful whether or not it gets built, so those reports are listed too.

## Contributed

A report describes the problem; a patch decides the answer. Translations are the
sharpest version of that, because they are the one contribution the maintainer
cannot actually check. I can diff the file, count the keys, and match the format
specifiers — I cannot tell you whether the words sound like something a person
would write. Those languages are in the app on someone else's judgement.

The other thing an outside patch catches is what the maintainer has stopped
seeing. The launcher icon had been overflowing the adaptive-icon safe zone since
the day it was drawn — the leaf ran a millimetre past the mask, so every launcher
quietly cut its tip off — and I had looked at it on my own home screen for months
without registering it. It took somebody else opening their app drawer and
deciding the clipped leaf was worth a pull request.

| | Contributed |
|---|---|
| [@ALILEX-1](https://github.com/ALILEX-1) | [#294](https://github.com/jeiel85/markleaf-android/pull/294) — Simplified Chinese |
| [@ThatOneCalculator](https://github.com/ThatOneCalculator) | [#318](https://github.com/jeiel85/markleaf-android/pull/318) — launcher icon clipped by the adaptive-icon mask |

Reporting something new? [Issues](https://github.com/jeiel85/markleaf-android/issues)
for bugs, [Discussions](https://github.com/jeiel85/markleaf-android/discussions)
for ideas and questions. A translation, or a fix for one, is welcome as a pull
request.
