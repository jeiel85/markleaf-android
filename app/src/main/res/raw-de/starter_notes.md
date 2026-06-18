# Willkommen bei Markleaf

Markleaf ist ein ruhiges, lokales Markdown-Notizbuch fuer Android. Es startet schnell, stoert nicht beim Schreiben und bewahrt deine Texte als eigenes Plain Text auf.

## Ein kurzer Rundgang

- Oeffne **Eine schoene Markdown-Leinwand**, um die Schreibflaeche zu sehen.
- Oeffne **Taegliches Schreibritual** fuer ein Journal-Beispiel.
- Oeffne **Projektbriefing** fuer Aufgaben, Links und Struktur.
- Oeffne **Lokaler Ordnermirror**, wenn du Dateien ausserhalb der App nutzen willst.

> [!TIP]
> Das sind normale Notizen. Du kannst sie bearbeiten, exportieren, in den Papierkorb legen oder loeschen.

#start #guide #anfang

---markleaf-note---

# Eine schoene Markdown-Leinwand

![Markleaf Beispiel-Leinwand](attachments/starter-note-2/markleaf-sample-cover.png)

Markdown bleibt als Text lesbar und wird in der **Vorschau** ruhig und aufgeraeumt dargestellt.

## Was diese Notiz zeigt

- **Fett**, _kursiv_, ~~durchgestrichen~~ und `Inline-Code`
- Ueberschriften, Listen, Checklisten, Zitate, Trenner, Codebloecke, Tabellen, Callouts, Fussnoten, Links und Bilder
- Live-Syntaxstil waehrend des Schreibens

> [!NOTE]
> Wechsle oben zwischen Bearbeiten und Vorschau. Die Notiz bleibt Markdown.

| Element | Zweck |
| --- | --- |
| `#tag` | Organisation |
| `[[Projektbriefing]]` | lokale Notizlinks |
| `![](...)` | Bildanhaenge |

```kotlin
fun markleaf() = "local-first markdown"
```

Eine kleine Fussnote haelt Details nah am Text.[^1]

[^1]: Fussnoten, Callouts, Tabellen und Codebloecke werden lokal gerendert.

#markdown #showcase #schreiben

---markleaf-note---

# Taegliches Schreibritual

## Morgenseite

Das Ziel ist nicht, mehr zu schreiben. Das Ziel ist, den ersten Satz leicht zu machen.

- [x] Einen Gedanken festhalten
- [ ] Eine Aufgabe in eine Notiz verwandeln
- [ ] Verwandte Arbeit mit [[Projektbriefing]] verlinken

> Halte die Notiz klein genug, damit du wirklich zurueckkommst.

## Abendabschluss

Was hat sich heute bewegt?

1. Eine nuetzliche Entscheidung
2. Eine offene Frage
3. Etwas fuer morgen

#journal #writing #tagebuch

---markleaf-note---

# Projektbriefing

Diese Notiz zeigt, wie Markleaf ein kleines Projekt halten kann, ohne schwer zu werden.

## Ergebnis

Ein klares Beispiel-Notizbuch liefern, das durch Nutzung erklaert.

## Plan

- [x] Markdown schoen zeigen
- [x] Einen Bildanhang einbinden
- [ ] Nach `local-first` suchen
- [ ] Backlinks aus **Taegliches Schreibritual** oeffnen

## Notizen

Verwandt: [[Taegliches Schreibritual]] und [[Tags, Suche und Backlinks]]

#project/markleaf #planning #projekt

---markleaf-note---

# Tags, Suche und Backlinks

Schreibe Tags direkt in den Text: #project, #writing, #privacy, #local-first.

## Suchideen

Suche nach:

- `local-first`
- `folder mirror`
- `Projektbriefing`

## Backlinks

Wikilinks verwenden `[[Notiztitel]]`. Wenn eine andere Notiz hierher verlinkt, zeigt Markleaf diese Beziehung lokal an. Kein Konto, kein Server.

Siehe auch [[Projektbriefing]].

#organize #search #ordnung

---markleaf-note---

# Lokaler Ordnermirror

Markleaf braucht keine eigene Cloud. Du waehlst einen Ordner, und Android oder dein Sync-Tool kuemmert sich darum.

## Was passiert

- Markleaf schreibt jede Notiz als Markdown-Datei.
- Das Frontmatter behaelt die stabile `markleaf_id`.
- Anhaenge bleiben neben den gespiegelten Notizen.
- Die App deklariert weiterhin keine INTERNET-Berechtigung.

## Warum das wichtig ist

Deine Notizen bleiben in anderen Markdown-Tools lesbar, und Sync bleibt deine Entscheidung.

#privacy #folder-mirror #local-first #datenschutz
